package com.sub2.monitor.collect.newApi.service.impl;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiGroupsResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginReq;
import com.sub2.monitor.collect.newApi.dto.NewApiLoginRes;
import com.sub2.monitor.collect.newApi.dto.NewApiRawGroupsResponse;
import com.sub2.monitor.collect.newApi.dto.NewApiTokensResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NewApiCollectServiceImpl implements NewApiCollectService {

    private static final String LOGIN_PATH = "/api/user/login";
    private static final String GROUPS_PATH = "/api/user/self/groups";
    private static final String TOKENS_PATH = "/api/token/?p=1&size=20";
    private static final String AUTHORIZATION_CACHE_PREFIX = "newapi:authorization:";
    private static final String COOKIE_CACHE_NAME = "cookie";
    private static final String NEW_API_USER_CACHE_NAME = "new-api-user";
    private static final String NEW_API_USER_HEADER = "new-api-user";
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

                // NewApi 后续接口需要同时携带 Cookie 和 new-api-user，请求头值分别来自登录响应头和响应体用户 id。
                String cookie = response.getHeaders() == null ? null : response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
                Long userId = response.getBody().getData() == null ? null : response.getBody().getData().getId();
                if (cookie != null && !cookie.isBlank()) {
                    redisTemplate.opsForValue().set(buildAuthorizationCacheKey(baseUrl, username, COOKIE_CACHE_NAME), cookie, SESSION_CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    log.info("NewApi 账号登录成功并已缓存 Cookie，账号={}，Cookie长度={}", username, cookie.length());
                } else {
                    log.info("NewApi 账号登录成功但响应头未返回 Set-Cookie，账号={}", username);
                }
                if (userId != null) {
                    redisTemplate.opsForValue().set(buildAuthorizationCacheKey(baseUrl, username, NEW_API_USER_CACHE_NAME), userId.toString(), SESSION_CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    log.info("NewApi 账号登录成功并已缓存 {}，账号={}，用户ID={}", NEW_API_USER_HEADER, username, userId);
                } else {
                    log.warn("NewApi 账号登录成功但响应体未返回用户ID，账号={}，响应={}", username, response.getBody());
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

    @Override
    public NewApiGroupsResponse collectGroups(String baseUrl) {
        List<Account> accounts = accountMapper.selectNewApiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要采集分组的 NewApi 账号 (baseUrl={})", baseUrl);
            return null;
        }
        log.info("开始采集 NewApi 分组，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        WebClient client = buildWebClient(baseUrl);
        List<NewApiGroupsResponse> responses = new ArrayList<>();

        for (Account account : accounts) {
            String username = account.getUsername();
            String cookie = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, username, COOKIE_CACHE_NAME));
            String newApiUser = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, username, NEW_API_USER_CACHE_NAME));
            if (cookie == null || cookie.isBlank() || newApiUser == null || newApiUser.isBlank()) {
                log.warn("NewApi 分组采集跳过账号，Redis 中缺少 Cookie 或 {}，账号={}，baseUrl={}", NEW_API_USER_HEADER, username, baseUrl);
                continue;
            }

            WebClient requestClient = client.mutate()
                    .defaultHeader(HttpHeaders.COOKIE, cookie)
                    .defaultHeader(NEW_API_USER_HEADER, newApiUser)
                    .build();

            log.info("开始请求 NewApi 分组，账号={}，url={}{}", username, baseUrl, GROUPS_PATH);
            try {
                NewApiGroupsResponse response = requestClient.get()
                        .uri(GROUPS_PATH)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("NewApi 分组接口响应，账号={}，状态码={}", username, status);

                            if (status.is2xxSuccessful()) {
                                return clientResponse.bodyToMono(NewApiRawGroupsResponse.class)
                                        .map(this::convertGroupsResponse)
                                        .doOnSuccess(body -> {
                                            int groupCount = body == null || body.getData() == null ? 0 : body.getData().size();
                                            log.info("NewApi 分组解析完成，账号={}，分组数量={}", username, groupCount);
                                            log.debug("NewApi 分组响应体，账号={}，响应={}", username, body);
                                        });
                            }

                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("NewApi 分组请求失败，账号={}，状态码={}，响应体={}",
                                                username, status, errorBody);
                                        return Mono.error(new RuntimeException(
                                                "Request failed with status: " + status + ", body: " + errorBody));
                                    });
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("NewApi 分组采集重试，账号={}，第{}次，原因={}",
                                                username,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();

                if (response != null) {
                    responses.add(response);
                    log.info("NewApi 分组采集成功，账号={}，当前成功数量={}", username, responses.size());
                } else {
                    log.warn("NewApi 分组采集返回空响应，账号={}", username);
                }
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("NewApi 分组采集最终失败，账号={}，异常类型={}，原因={}",
                        username, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }

        log.info("NewApi 分组采集任务结束，baseUrl={}，成功数量={}", baseUrl, responses.size());
        return responses.isEmpty() ? null : responses.get(0);
    }

    @Override
    public NewApiTokensResponse collectNewApiKeys(String baseUrl) {
        List<Account> accounts = accountMapper.selectNewApiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要采集令牌的 NewApi 账号 (baseUrl={})", baseUrl);
            return null;
        }
        log.info("开始采集 NewApi 令牌列表，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        WebClient client = buildWebClient(baseUrl);
        List<NewApiTokensResponse> responses = new ArrayList<>();

        for (Account account : accounts) {
            String username = account.getUsername();
            String cookie = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, username, COOKIE_CACHE_NAME));
            String newApiUser = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, username, NEW_API_USER_CACHE_NAME));
            if (cookie == null || cookie.isBlank() || newApiUser == null || newApiUser.isBlank()) {
                log.warn("NewApi 令牌采集跳过账号，Redis 中缺少 Cookie 或 {}，账号={}，baseUrl={}", NEW_API_USER_HEADER, username, baseUrl);
                continue;
            }

            WebClient requestClient = client.mutate()
                    .defaultHeader(HttpHeaders.COOKIE, cookie)
                    .defaultHeader(NEW_API_USER_HEADER, newApiUser)
                    .build();

            log.info("开始请求 NewApi 令牌列表，账号={}，url={}{}", username, baseUrl, TOKENS_PATH);
            try {
                NewApiTokensResponse response = requestClient.get()
                        .uri(TOKENS_PATH)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("NewApi 令牌列表接口响应，账号={}，状态码={}", username, status);

                            if (status.is2xxSuccessful()) {
                                return clientResponse.bodyToMono(NewApiTokensResponse.class)
                                        .doOnSuccess(body -> {
                                            int tokenCount = body == null || body.getData() == null || body.getData().getItems() == null
                                                    ? 0
                                                    : body.getData().getItems().size();
                                            int total = body == null || body.getData() == null ? 0 : body.getData().getTotal();
                                            log.info("NewApi 令牌列表解析完成，账号={}，当前页数量={}，总数量={}", username, tokenCount, total);
                                            log.debug("NewApi 令牌列表响应体，账号={}，响应={}", username, body);
                                        });
                            }

                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("NewApi 令牌列表请求失败，账号={}，状态码={}，响应体={}",
                                                username, status, errorBody);
                                        return Mono.error(new RuntimeException(
                                                "Request failed with status: " + status + ", body: " + errorBody));
                                    });
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("NewApi 令牌列表采集重试，账号={}，第{}次，原因={}",
                                                username,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();

                if (response != null) {
                    responses.add(response);
                    log.info("NewApi 令牌列表采集成功，账号={}，当前成功数量={}", username, responses.size());
                } else {
                    log.warn("NewApi 令牌列表采集返回空响应，账号={}", username);
                }
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("NewApi 令牌列表采集最终失败，账号={}，异常类型={}，原因={}",
                        username, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }

        log.info("NewApi 令牌列表采集任务结束，baseUrl={}，成功数量={}", baseUrl, responses.size());
        return responses.isEmpty() ? null : responses.get(0);
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
     * NewApi 分组接口返回 Map，key 是分组名；这里转换成列表，便于后续统一处理 name/desc/ratio。
     */
    private NewApiGroupsResponse convertGroupsResponse(NewApiRawGroupsResponse rawResponse) {
        NewApiGroupsResponse response = new NewApiGroupsResponse();
        if (rawResponse == null) {
            return response;
        }

        response.setSuccess(rawResponse.getSuccess());
        response.setMessage(rawResponse.getMessage());
        List<NewApiGroupsResponse.GroupItem> groups = new ArrayList<>();
        if (rawResponse.getData() != null) {
            for (Map.Entry<String, NewApiRawGroupsResponse.GroupValue> entry : rawResponse.getData().entrySet()) {
                NewApiGroupsResponse.GroupItem group = new NewApiGroupsResponse.GroupItem();
                group.setName(entry.getKey());
                if (entry.getValue() != null) {
                    group.setDesc(entry.getValue().getDesc());
                    group.setRatio(entry.getValue().getRatio());
                }
                groups.add(group);
            }
        }
        response.setData(groups);
        return response;
    }

    /**
     * Redis key 统一包含平台、用途、平台地址和账号标识，避免不同平台或同名账号互相覆盖。
     */
    private String buildAuthorizationCacheKey(String baseUrl, String username, String headerName) {
        return AUTHORIZATION_CACHE_PREFIX + headerName + ":" + baseUrl + ":" + username;
    }
}
