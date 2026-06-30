package com.sub2.monitor.collect.sub2api.service.impl;

import com.sub2.monitor.collect.common.dto.LoginResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2AvailableGroupsResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2KeysResponse;
import com.sub2.monitor.collect.sub2api.dto.Sub2LoginRequest;
import com.sub2.monitor.collect.sub2api.dto.Sub2LoginRes;
import com.sub2.monitor.collect.sub2api.dto.Sub2UsageStatsResponse;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
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
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class Sub2CollectServiceImpl implements Sub2CollectService {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String AVAILABLE_GROUPS_PATH = "/api/v1/groups/available?timezone=Asia%2FShanghai";
    private static final String KEYS_PATH = "/api/v1/keys?page=1&page_size=100&sort_by=created_at&sort_order=desc&timezone=Asia%2FShanghai";
    private static final String USAGE_STATS_PATH = "/api/v1/usage/dashboard/stats?timezone=Asia%2FShanghai";
    private static final String AUTHORIZATION_CACHE_PREFIX = "sub2api:authorization:";
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 10;
    private static final int RETRY_TIMES = 3;
    private static final int RETRY_DELAY_SECONDS = 5;
    private static final int ACCOUNT_INTERVAL_SECONDS = 5;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 预留扩展

    @Override
    public LoginResponse<Sub2LoginRes> login(String baseUrl) {
        List<Account> accounts = accountMapper.selectSub2apiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要登录的账号 (baseUrl={})", baseUrl);
            return null;
        }
        log.info("开始执行 Sub2 登录任务，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        WebClient client = buildWebClient(baseUrl)
                .mutate()
                .defaultHeader(HttpHeaders.ORIGIN, baseUrl)
                .build();

        for (Account account : accounts) {
            String email = account.getEmail();
            try {
                // 多账号连续登录时保留固定间隔，降低平台风控或限流概率。
                TimeUnit.SECONDS.sleep(ACCOUNT_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待账号登录间隔时线程被中断，停止后续登录，baseUrl={}，当前账号={}", baseUrl, email);
                return null;
            }

            Sub2LoginRequest request = new Sub2LoginRequest();
            request.setEmail(email);
            request.setPassword(account.getPassword());

            try {
                log.info("开始登录 Sub2 账号，baseUrl={}，账号={}", baseUrl, email);
                LoginResponse<Sub2LoginRes> response = client.post()
                        .uri(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("Sub2 登录接口响应，账号={}，状态码={}", email, status);
                            if (status.is4xxClientError()) {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Sub2 账号登录客户端错误，账号={}，状态码={}，响应体={}",
                                                    email, status, errorBody);
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
                                            log.warn("Sub2 登录接口服务端错误，账号={}，状态码={}，响应体={}",
                                                    email, status, errorBody);
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
                                // 成功响应需要同时保留响应头和业务体，后续如需 Cookie 或限流头时可以直接复用。
                                return clientResponse.bodyToMono(Sub2LoginRes.class)
                                        .map(body -> {
                                            LoginResponse<Sub2LoginRes> wrapper = new LoginResponse<>();
                                            wrapper.setHeaders(clientResponse.headers().asHttpHeaders());
                                            wrapper.setBody(body);
                                            return wrapper;
                                        });
                            }
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("Sub2 账号登录重试，账号={}，第{}次，原因={}",
                                                email,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();
                String accessToken = getAccessToken(response);
                if (accessToken == null || accessToken.isBlank()) {
                    log.warn("Sub2 账号登录成功但未返回 access_token，账号={}，响应={}", email, response);
                    continue;
                }

                // 仅缓存 token，不打印 token 明文，避免日志泄露认证凭据。
                redisTemplate.opsForValue().set(buildAuthorizationCacheKey(baseUrl, email), accessToken, 120, TimeUnit.MINUTES);
                log.info("Sub2 账号登录成功并已缓存 token，账号={}，token长度={}", email, accessToken.length());
                return response;
            } catch (WebClientResponseException e) {
                String errorBody = e.getResponseBodyAsString();
                log.error("Sub2 账号登录最终失败，账号={}，HTTP状态码={}，响应体={}",
                        email, e.getStatusCode(), errorBody);
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("Sub2 账号登录异常，账号={}，异常类型={}，原因={}",
                        email, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }
        log.warn("Sub2 登录任务结束，所有账号均未成功登录，baseUrl={}", baseUrl);
        return null;
    }

    @Override
    public Sub2AvailableGroupsResponse collectSub2AvailableGroups(String baseUrl) {
        List<Account> accounts = accountMapper.selectSub2apiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要采集可用分组的账号 (baseUrl={})", baseUrl);
            return null;
        }

        List<Sub2AvailableGroupsResponse> responses = new ArrayList<>();
        log.info("开始采集 Sub2 可用分组，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        // WebClient 在本次采集任务内复用，避免每个账号重复创建连接配置。
        WebClient client = buildWebClient(baseUrl);

        for (Account account : accounts) {
            String email = account.getEmail();

            // 采集接口依赖登录阶段写入 Redis 的 token；缺失时跳过当前账号，避免无效请求。
            String token = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, email));
            if (token == null || token.isBlank()) {
                log.warn("Sub2 可用分组采集跳过账号，Redis 中未找到 token，账号={}，baseUrl={}", email, baseUrl);
                continue;
            }

            // 每个账号使用自己的 Authorization 请求头，基础连接配置仍复用上面的 client。
            WebClient requestClient = client.mutate()
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();

            log.info("开始请求 Sub2 可用分组，账号={}，url={}{}", email, baseUrl, AVAILABLE_GROUPS_PATH);

            try {
                Sub2AvailableGroupsResponse response = requestClient.get()
                        .uri(AVAILABLE_GROUPS_PATH)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("Sub2 可用分组接口响应，账号={}，状态码={}", email, status);

                            if (status.is2xxSuccessful()) {
                                return clientResponse.bodyToMono(Sub2AvailableGroupsResponse.class)
                                        .doOnSuccess(body -> {
                                            int groupCount = body == null || body.getData() == null ? 0 : body.getData().size();
                                            log.info("Sub2 可用分组解析完成，账号={}，分组数量={}", email, groupCount);
                                            log.debug("Sub2 可用分组响应体，账号={}，响应={}", email, body);
                                        });
                            } else {
                                // 非 2xx 时读取错误体，方便排查 token 失效、权限不足或平台侧错误。
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Sub2 可用分组请求失败，账号={}，状态码={}，响应体={}",
                                                    email, status, errorBody);
                                            return Mono.error(new RuntimeException(
                                                    "Request failed with status: " + status + ", body: " + errorBody));
                                        });
                            }
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("Sub2 可用分组采集重试，账号={}，第{}次，原因={}",
                                                email,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();

                if (response != null) {
                    responses.add(response);
                    log.info("Sub2 可用分组采集成功，账号={}，当前成功数量={}", email, responses.size());
                } else {
                    log.warn("Sub2 可用分组采集返回空响应，账号={}", email);
                }

            } catch (Exception e) {
                // 单个账号失败不影响其他账号，最终通过日志定位具体失败账号和原因。
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("Sub2 可用分组采集最终失败，账号={}，异常类型={}，原因={}",
                        email, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }

        // 当前接口返回单个响应对象，先沿用原有行为：多个账号成功时返回第一个成功结果。
        log.info("Sub2 可用分组采集任务结束，baseUrl={}，成功数量={}", baseUrl, responses.size());
        return responses.isEmpty() ? null : responses.get(0);
    }

    @Override
    public Sub2KeysResponse collectSub2Keys(String baseUrl) {
        List<Account> accounts = accountMapper.selectSub2apiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要采集密钥的账号 (baseUrl={})", baseUrl);
            return null;
        }

        List<Sub2KeysResponse> responses = new ArrayList<>();
        log.info("开始采集 Sub2 密钥列表，baseUrl={}，账号数量={}", baseUrl, accounts.size());

        // 密钥列表接口与分组接口一致，都依赖登录后缓存的 Bearer token。
        WebClient client = buildWebClient(baseUrl);

        for (Account account : accounts) {
            String email = account.getEmail();

            String token = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, email));
            if (token == null || token.isBlank()) {
                log.warn("Sub2 密钥采集跳过账号，Redis 中未找到 token，账号={}，baseUrl={}", email, baseUrl);
                continue;
            }

            WebClient requestClient = client.mutate()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

            log.info("开始请求 Sub2 密钥列表，账号={}，url={}{}", email, baseUrl, KEYS_PATH);

            try {
                Sub2KeysResponse response = requestClient.get()
                        .uri(KEYS_PATH)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            log.info("Sub2 密钥列表接口响应，账号={}，状态码={}", email, status);

                            if (status.is2xxSuccessful()) {
                                return clientResponse.bodyToMono(Sub2KeysResponse.class)
                                        .doOnSuccess(body -> {
                                            int keyCount = body == null || body.getData() == null || body.getData().getItems() == null
                                                    ? 0
                                                    : body.getData().getItems().size();
                                            int total = body == null || body.getData() == null ? 0 : body.getData().getTotal();
                                            log.info("Sub2 密钥列表解析完成，账号={}，当前页数量={}，总数量={}", email, keyCount, total);
                                            log.debug("Sub2 密钥列表响应体，账号={}，响应={}", email, body);
                                        });
                            } else {
                                // 记录错误体，便于区分 token 过期、权限不足、参数异常等平台返回。
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            log.error("Sub2 密钥列表请求失败，账号={}，状态码={}，响应体={}",
                                                    email, status, errorBody);
                                            return Mono.error(new RuntimeException(
                                                    "Request failed with status: " + status + ", body: " + errorBody));
                                        });
                            }
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("Sub2 密钥列表采集重试，账号={}，第{}次，原因={}",
                                                email,
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage())
                                )
                        )
                        .block();

                if (response != null) {
                    responses.add(response);
                    log.info("Sub2 密钥列表采集成功，账号={}，当前成功数量={}", email, responses.size());
                } else {
                    log.warn("Sub2 密钥列表采集返回空响应，账号={}", email);
                }
            } catch (Exception e) {
                // 与分组采集保持一致，单账号失败后继续处理其他账号。
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("Sub2 密钥列表采集最终失败，账号={}，异常类型={}，原因={}",
                        email, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }

        // 当前 service 方法返回单个响应，多个账号成功时沿用分组接口行为返回第一个成功结果。
        log.info("Sub2 密钥列表采集任务结束，baseUrl={}，成功数量={}", baseUrl, responses.size());
        return responses.isEmpty() ? null : responses.get(0);
    }

    @Override
    public Sub2UsageStatsResponse collectUsageStats(String baseUrl) {
        List<Account> accounts = accountMapper.selectSub2apiAccounts(baseUrl);
        if (accounts.isEmpty()) {
            log.info("没有需要采集用量统计的 Sub2 账号 (baseUrl={})", baseUrl);
            return null;
        }

        WebClient client = buildWebClient(baseUrl);
        for (Account account : accounts) {
            String email = account.getEmail();
            String token = redisTemplate.opsForValue().get(buildAuthorizationCacheKey(baseUrl, email));
            if (token == null || token.isBlank()) {
                log.warn("Sub2 用量统计采集跳过账号，Redis 中未找到 token，账号={}，baseUrl={}", email, baseUrl);
                continue;
            }
            WebClient requestClient = client.mutate()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            try {
                Sub2UsageStatsResponse response = requestClient.get()
                        .uri(USAGE_STATS_PATH)
                        .exchangeToMono(clientResponse -> {
                            HttpStatusCode status = clientResponse.statusCode();
                            if (status.is2xxSuccessful()) {
                                return clientResponse.bodyToMono(Sub2UsageStatsResponse.class);
                            }
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException(
                                            "Request failed with status: " + status + ", body: " + errorBody)));
                        })
                        .retryWhen(Retry.fixedDelay(RETRY_TIMES, Duration.ofSeconds(RETRY_DELAY_SECONDS)))
                        .block();
                if (response != null) {
                    return response;
                }
            } catch (Exception e) {
                Throwable rootCause = Exceptions.unwrap(e);
                log.error("Sub2 用量统计采集失败，账号={}，异常类型={}，原因={}",
                        email, rootCause.getClass().getSimpleName(), rootCause.getMessage(), rootCause);
            }
        }
        return null;
    }

    /**
     * 构建 Sub2 API 专用客户端，统一设置基础请求头和超时，避免各接口配置不一致。
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
    private String buildAuthorizationCacheKey(String baseUrl, String email) {
        return AUTHORIZATION_CACHE_PREFIX + baseUrl + ":" + email;
    }

    /**
     * 登录响应层级较深，集中处理空值判断，避免平台返回异常结构时触发空指针。
     */
    private String getAccessToken(LoginResponse<Sub2LoginRes> response) {
        if (response == null || response.getBody() == null || response.getBody().getData() == null) {
            return null;
        }
        return response.getBody().getData().getAccessToken();
    }
}
