package com.sub2.monitor.collect.newApi.service.impl;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginReq;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginRes;
import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.monitor.entity.Account;
import com.sub2.monitor.monitor.mapper.AccountMapper;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NewApiCollectServiceImpl implements NewApiCollectService {

    private static final String LOGIN_PATH = "/api/user/login";
    private static final String AUTHORIZATION_CACHE_PREFIX = "newapi:authorization:";
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;
    private static final int RETRY_TIMES = 3;
    private static final int RETRY_DELAY_SECONDS = 5;
    private static final int ACCOUNT_INTERVAL_SECONDS = 5;
    private static final long SESSION_CACHE_TIMEOUT_MINUTES = 120;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 预留扩展

    @Override
    public LoginResponse<NewApiLoginRes> login(String baseUrl) {
        List<Account> accounts = accountMapper.selectNewApiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要登录的 NewApi 账号 (baseUrl={})", baseUrl);
            return null;
        }
        log.info("开始执行 NewApi 登录任务，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        WebClient client = buildWebClient(baseUrl)
                .mutate()
                .defaultHeader(HttpHeaders.ORIGIN, baseUrl)
                .build();

        for (Account account : accounts) {
            String username = account.getUsername();
            try {
                // 多账号连续登录时保留固定间隔，降低平台风控或限流概率。
                TimeUnit.SECONDS.sleep(ACCOUNT_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待 NewApi 账号登录间隔时线程被中断，停止后续登录，baseUrl={}，当前账号={}", baseUrl, username);
                return null;
            }

            NewApiLoginReq request = new NewApiLoginReq();
            request.setUsername(username);
            request.setPassword(account.getPassword());

            try {
                log.info("开始登录 NewApi 账号，baseUrl={}，账号={}", baseUrl, username);
                LoginResponse<NewApiLoginRes> response = client.post()
                        .uri(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("NewApi 登录接口响应，账号={}，状态码={}", username, status);
                            if (status.is4xxClientError()) {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("NewApi 账号登录客户端错误，账号={}，状态码={}，响应体={}",
                                                    username, status, errorBody);
                                            return Mono.error(new WebClientResponseException(
                                                    "Client error: " + errorBody,
                                                    status.value(),
                                                    status.toString(),
                                                    null,
                                                    errorBody.getBytes(StandardCharsets.UTF_8),
                                                    StandardCharsets.UTF_8
                                            ));
                                        });
                            } else if (status.is5xxServerError()) {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.warn("NewApi 登录接口服务端错误，账号={}，状态码={}，响应体={}",
                                                    username, status, errorBody);
                                            return Mono.error(new WebClientResponseException(
                                                    "Server error: " + errorBody,
                                                    status.value(),
                                                    status.toString(),
                                                    null,
                                                    errorBody.getBytes(StandardCharsets.UTF_8),
                                                    StandardCharsets.UTF_8
                                            ));
                                        });
                            } else {
                                // NewApi 登录态通常通过响应头 Cookie 维持，因此保留 headers 供后续接口复用。
                                return clientResponse.bodyToMono(NewApiLoginRes.class)
                                        .map(body -> {
                                            LoginResponse<NewApiLoginRes> wrapper = new LoginResponse<>();
                                            wrapper.setHeaders(clientResponse.headers().asHttpHeaders());
                                            wrapper.setBody(body);
                                            return wrapper;
                                        });
                            }
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("NewApi 账号登录重试，账号={}，第{}次，原因={}",
                                                username,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();

                if (response == null || response.getBody() == null) {
                    log.warn("NewApi 账号登录返回空响应，账号={}", username);
                    continue;
                }
                if (!Boolean.TRUE.equals(response.getBody().getSuccess())) {
                    log.warn("NewApi 账号登录业务失败，账号={}，响应={}", username, response.getBody());
                    continue;
                }

                // NewApi 没有 access_token 字段，先缓存 Set-Cookie，后续采集接口可直接读取并放入 Cookie 请求头。
                String cookie = response.getHeaders() == null ? null : response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
                if (cookie != null && !cookie.isBlank()) {
                    redisTemplate.opsForValue().set(buildAuthorizationCacheKey(baseUrl, username), cookie, SESSION_CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    log.info("NewApi 账号登录成功并已缓存 Cookie，账号={}，Cookie长度={}", username, cookie.length());
                } else {
                    log.info("NewApi 账号登录成功但响应头未返回 Set-Cookie，账号={}", username);
                }
                return response;
            } catch (WebClientResponseException e) {
                String errorBody = e.getResponseBodyAsString();
                log.error("NewApi 账号登录最终失败，账号={}，HTTP状态码={}，响应体={}",
                        username, e.getStatusCode(), errorBody);
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("NewApi 账号登录异常，账号={}，异常类型={}，原因={}",
                        username, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }
        log.warn("NewApi 登录任务结束，所有账号均未成功登录，baseUrl={}", baseUrl);
        return null;
    }

    /**
     * 构建 NewApi 专用客户端，统一设置基础请求头和超时，避免各接口配置不一致。
     */
    private WebClient buildWebClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                ))
                .build();
    }

    /**
     * Redis key 统一包含平台、用途、平台地址和账号标识，避免不同平台或同名账号互相覆盖。
     */
    private String buildAuthorizationCacheKey(String baseUrl, String username) {
        return AUTHORIZATION_CACHE_PREFIX + baseUrl + ":" + username;
    }
}
