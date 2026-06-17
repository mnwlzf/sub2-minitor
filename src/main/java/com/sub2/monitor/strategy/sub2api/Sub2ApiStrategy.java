package com.sub2.monitor.strategy.sub2api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.client.HttpsClient;
import com.sub2.monitor.config.datasource.DataSourceKey;
import com.sub2.monitor.config.datasource.DataSourceSwitcher;
import com.sub2.monitor.dto.LoginRequest;
import com.sub2.monitor.dto.sub2api.Sub2ApiAccountProxy;
import com.sub2.monitor.dto.sub2api.Sub2ApiChannelResponse;
import com.sub2.monitor.dto.sub2api.Sub2ApiLoginResponse;
import com.sub2.monitor.dto.sub2api.Sub2ApiRequest;
import com.sub2.monitor.dto.sub2api.Sub2ApiResponse;
import com.sub2.monitor.entity.AccountBalanceHistory;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.entity.Accounts;
import com.sub2.monitor.mapper.AccountBalanceHistoryMapper;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.mapper.AccountsMapper;
import com.sub2.monitor.mapper.PlatformMapper;
import com.sub2.monitor.mapper.PlatformRateHistoryMapper;
import com.sub2.monitor.mapper.sub2api.Sub2ApiAccountMapper;
import com.sub2.monitor.strategy.AbstractApiStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * sub2api 策略实现。
 * <p>
 * 与 newapi 共用同一套登录/会话/请求处理逻辑，
 * 仅接口地址与请求对象不同。
 */
@Slf4j
@Component
public class Sub2ApiStrategy extends AbstractApiStrategy<Sub2ApiRequest, Sub2ApiResponse> {
    private static final long LOGIN_REQUEST_INTERVAL_MS = 3000L;  // 登录请求间隔

    public Sub2ApiStrategy(HttpsClient httpsClient) {
        super(httpsClient);
    }

    @Autowired
    private PlatformMapper platformMapper;


    @Autowired
    private AccountsMapper accountsMapper;

    @Autowired
    private AccountBalanceHistoryMapper accountBalanceHistoryMapper;

    @Autowired
    private PlatformRateHistoryMapper platformRateHistoryMapper;

    @Autowired
    private Sub2ApiAccountMapper sub2ApiAccountMapper;


    @Autowired
    private DataSourceSwitcher dataSourceSwitcher;



    public Sub2ApiResponse execute(Sub2ApiRequest request) {
        TaskContext context = prepareRequest(request);
        Platform platform = context.platform();
        log.info("开始执行 Sub1API 任务, baseUrl={}, 账号数={}",
                request.getBaseUrl(),
                context.accounts().size());

        // 【2】加载账号代理配置，后续登录和采集请求共用同一份映射。
        Map<String, HttpsClient.ProxyConfig> proxyConfigMap = loadProxyConfigMap(request.getBaseUrl());


        // 【3】先登录并从登录响应采集余额，再用登录态采集渠道倍率。
        login(request, context, proxyConfigMap);
        collectChannels(request, context, proxyConfigMap);


        return null;
    }


    public void login(Sub2ApiRequest request) {
        // 【1】独立登录也需要平台信息，确保余额历史字段完整。
        TaskContext context = prepareRequest(request);
        Platform platform = context.platform();

        // 【2】独立登录仍复用账号代理配置，保持网络路径一致。
        login(request, context, loadProxyConfigMap(request.getBaseUrl()));

    }

    /**
     * 内部登录实现。
     *
     * 【1】逐账号登录。
     * 【2】保存登录态。
     * 【3】读取 data.user.balance 并批量写入余额历史。
     */
    private void login(Sub2ApiRequest request, TaskContext context, Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        if (context.accounts() == null || context.accounts().isEmpty()) {
            throw new IllegalArgumentException("accounts is required");
        }

        // 【1】预加载本地账号 ID。必须带 platformId，避免不同平台同名账号串数据。
        String loginUrl = buildUrl(request.getBaseUrl(), "/api/v1/auth/login");
        Map<String, Long> accountIdMap = loadAccountIdMap(context.accounts(), context.platform().getId());

        // 【2】逐账号登录，余额历史先暂存在内存，循环结束后统一批量落库。
        List<AccountBalanceHistory> historyList = new ArrayList<>();
        for (AccountCredential account : context.accounts()) {
            // 【3】控制登录间隔，降低触发目标平台限流的概率。
            try {
                Thread.sleep(LOGIN_REQUEST_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("登录线程被中断", e);
                break;
            }

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(account.userName());
            loginRequest.setPassword(account.password());

            try {
                // 【4】发送登录请求。账号有代理就走代理，没有代理就直连。
                HttpsClient.HttpResult result = post(loginUrl, buildHeaders(request.getApiKey()), loginRequest,
                        proxyConfigMap.get(account.userName()));
                Sub2ApiLoginResponse loginResponse = JSONUtil.toBean(result.getBody(), Sub2ApiLoginResponse.class);

                if (loginResponse != null && loginResponse.isSuccess()) {
                    // 【5】保存登录态，后续渠道请求通过 mergeHeaders 自动带上 Authorization / Cookie。
                    saveSessionContext(
                            request.getBaseUrl(),
                            account.userName(),
                            loginResponse,
                            extractCookies(result.getHeaders())
                    );

                    // 【6】从登录响应的 data.user.balance 生成余额历史。
                    AccountBalanceHistory history = buildBalanceHistory(
                            account,
                            loginResponse,
                            accountIdMap,
                            context
                    );
                    if (history != null) {
                        historyList.add(history);
                    }
                    log.info("登录成功: {}", logContext(context, account.userName()));
                } else {
                    log.warn("登录失败: {}, response={}", logContext(context, account.userName()), result.getBody());
                }
            } catch (Exception e) {
                log.error("登录请求异常: {}", logContext(context, account.userName()), e);
                // 【7】单账号失败不影响本批次其他账号。
            }
        }

        // 【8】批量写入余额历史，只写入成功解析到账号 ID 和余额的记录。
        if (!historyList.isEmpty()) {
            accountBalanceHistoryMapper.insertBatch(historyList);
        }
    }




    public Sub2ApiResponse collectChannels(Sub2ApiRequest request, Long platformId) {
        TaskContext context = prepareRequest(request);
        Platform platform = context.platform();
        return collectChannels(request, context, loadProxyConfigMap(request.getBaseUrl()));
    }

    private Sub2ApiResponse collectChannels(Sub2ApiRequest request, TaskContext context, Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        // 【1】渠道接口依赖登录态，所以必须在 login(...) 之后调用。
        String url = buildUrl(request.getBaseUrl(), "/api/v1/groups/available?timezone=Asia%2FShanghai");
        Map<String, PlatformRateHistory> historyMap = new LinkedHashMap<>();
        List<Sub2ApiResponse> responses = new ArrayList<>();
        OffsetDateTime batchTime = OffsetDateTime.now();

        // 【2】按账号请求渠道，使用 mergeHeaders 合并登录阶段保存的 Authorization / Cookie。
        for (AccountCredential account : context.accounts()) {
            try {
                HttpsClient.HttpResult result = get(url,
                        mergeHeaders(request.getBaseUrl(), account.userName(), request.getApiKey()),
                        proxyConfigMap.get(account.userName()));
                log.info("渠道接口响应: {}, body={}", logContext(context, account.userName()), result.getBody());
                Sub2ApiChannelResponse channelResponse = JSONUtil.toBean(result.getBody(), Sub2ApiChannelResponse.class);
                if (channelResponse == null || !channelResponse.isSuccess()) {
                    log.warn("渠道响应为空: {}, response={}", logContext(context, account.userName()), result.getBody());
                    continue;
                }

                for (Sub2ApiChannelResponse.ChannelData channelData : channelResponse.getData()) {
                    if (channelData == null || StrUtil.isBlank(channelData.getName())) {
                        continue;
                    }

                    PlatformRateHistory history = new PlatformRateHistory();
                    history.setPlatformId(context.platform().getId());
                    history.setChannelName(channelData.getName());
                    history.setCurrentRate(channelData.getRateMultiplier() == null ? BigDecimal.ONE : channelData.getRateMultiplier());
                    history.setCreateTime(batchTime);
                    historyMap.putIfAbsent(channelData.getName(), history);
                }

                Sub2ApiResponse response = new Sub2ApiResponse();
                response.setRawBody(result.getBody());
                responses.add(response);
            } catch (Exception e) {
                log.error("渠道采集异常: {}", logContext(context, account.userName()), e);
            }
        }

        // 【3】同一批结果按渠道名去重后落库，避免多个账号看到相同渠道时重复写入。
        if (!historyMap.isEmpty()) {
            log.info("渠道费率准备落库: platformId={}, platformName={}, count={}",
                    context.platform().getId(), context.platform().getName(), historyMap.size());
            platformRateHistoryMapper.insertBatch(new ArrayList<>(historyMap.values()));
        }

        // 【4】保留原始响应，方便后续排查接口结构变化。
        Sub2ApiResponse response = new Sub2ApiResponse();
        response.setRawBody(responses.stream()
                .map(Sub2ApiResponse::getRawBody)
                .collect(Collectors.joining("\n")));
        return response;
    }


    public Sub2ApiResponse testModelAvailability(Sub2ApiRequest request) {
        String url = buildUrl(request.getBaseUrl(), "/v1/models/" + request.getModel());
        return null;
    }


    private String buildExecuteBody(Sub2ApiResponse balanceResponse, Sub2ApiResponse channelsResponse, Sub2ApiResponse modelResponse) {
        Map<String, String> result = new HashMap<>();
        result.put("balance", balanceResponse.getRawBody());
        result.put("channels", channelsResponse.getRawBody());
        result.put("model", modelResponse.getRawBody());
        return result.toString();
    }

    private Platform findPlatform(String baseUrl) {
        // 【1】按 baseUrl 查询主库平台配置。
        // 这里返回的 id/name 会作为余额历史的 platformId/platform 字段。
        if (StrUtil.isBlank(baseUrl)) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        LambdaQueryWrapper<Platform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Platform::getBaseUrl, baseUrl);
        Platform platform = platformMapper.selectOne(wrapper);
        if (platform == null) {
            throw new IllegalArgumentException("platform not found, baseUrl=" + baseUrl);
        }
        return platform;
    }

    private TaskContext prepareRequest(Sub2ApiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        Platform platform = findPlatform(request.getBaseUrl());
        List<Accounts> accounts = loadAccounts(platform.getId(), request.getBaseUrl());
        fillDefaultModel(request, accounts);
        return new TaskContext(platform, toCredentials(accounts));
    }

    private List<Accounts> loadAccounts(Long platformId, String baseUrl) {
        List<Accounts> accounts = accountsMapper.selectList(new LambdaQueryWrapper<Accounts>()
                .eq(Accounts::getPlatformId, platformId)
                .orderByAsc(Accounts::getId));
        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException("accounts not found, baseUrl=" + baseUrl);
        }
        return accounts;
    }

    private List<AccountCredential> toCredentials(List<Accounts> accounts) {
        return accounts.stream()
                .filter(account -> StrUtil.isNotBlank(account.getUsername()))
                .map(account -> new AccountCredential(account.getUsername(), account.getPassword()))
                .collect(Collectors.toList());
    }

    private void fillDefaultModel(Sub2ApiRequest request, List<Accounts> accounts) {
        if (StrUtil.isNotBlank(request.getModel())) {
            return;
        }
        accounts.stream()
                .map(Accounts::getTestModel)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .ifPresent(request::setModel);
    }

    private Map<String, Long> loadAccountIdMap(List<AccountCredential> accounts, Long platformId) {
        // 【1】从请求账号中提取有效用户名。
        // 空用户名无法匹配本地 accounts 表，需要提前过滤。
        List<String> usernames = accounts.stream()
                .map(AccountCredential::userName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (usernames.isEmpty()) {
            return Map.of();
        }

        // 【2】按 username + platformId 查询本地账号 ID。
        // 同一个 username 可能存在于不同 baseUrl 平台下，所以必须限定 platformId。
        return accountsMapper.selectByNames(usernames, platformId).stream()
                .collect(Collectors.toMap(Accounts::getUsername, Accounts::getId, (old, ignored) -> old));
    }

    private AccountBalanceHistory buildBalanceHistory(AccountCredential account,
                                                      Sub2ApiLoginResponse loginResponse,
                                                      Map<String, Long> accountIdMap,
                                                      TaskContext context) {
        // 【1】先根据请求账号名找到本地 accountId。
        // account_balance_history.account_id 是必填字段，找不到就跳过该账号。
        Long accountId = accountIdMap.get(account.userName());
        if (accountId == null) {
            log.warn("本地无此账号: {}", logContext(context, account.userName()));
            return null;
        }

        // 【2】再从登录响应里读取 data.user.balance。
        // sub2api 的余额字段就在登录响应体中，不需要额外调用余额接口。
        Sub2ApiLoginResponse.User user = loginResponse.getData().getUser();
        if (user == null || user.getBalance() == null) {
            log.warn("登录响应未返回余额: {}", logContext(context, account.userName()));
            return null;
        }

        // 【3】最后组装余额历史实体。
        // 必填字段包括 accountId、platformId、platform、currentBalance、createTime。
        AccountBalanceHistory history = new AccountBalanceHistory();
        history.setAccountId(accountId);
        history.setPlatformId(context.platform().getId());
        history.setPlatform(context.platform().getName());
        history.setCurrentBalance(user.getBalance());
        history.setCreateTime(OffsetDateTime.now());
        return history;
    }

    private String logContext(TaskContext context, String userName) {
        Platform platform = context.platform();
        return "platformId=" + platform.getId()
                + ", platformName=" + platform.getName()
                + ", platformType=" + platform.getType()
                + ", baseUrl=" + platform.getBaseUrl()
                + ", username=" + userName;
    }

    private record TaskContext(Platform platform, List<AccountCredential> accounts) {
    }

    private record AccountCredential(String userName, String password) {
    }

    private void saveSessionContext(String baseUrl, String userName, Sub2ApiLoginResponse loginResponse,
                                    List<String> cookies) {
        // 【1】保存 Authorization。
        // access_token 来自登录响应 data.access_token，token_type 缺省时按 Bearer 处理。
        Map<String, String> sessionHeaders = new LinkedHashMap<>();
        String tokenType = StrUtil.blankToDefault(loginResponse.getData().getTokenType(), "Bearer");
        sessionHeaders.put("Authorization", tokenType + " " + loginResponse.getData().getAccessToken());

        // 【2】合并响应头里的 Set-Cookie。
        // 如果服务端同时依赖 Cookie 和 Bearer Token，后续请求也能保持完整登录态。
        if (cookies != null && !cookies.isEmpty()) {
            sessionHeaders.put("Cookie", String.join("; ", cookies));
        }

        // 【3】保存用户 ID。
        // 该字段主要用于排查问题或后续需要按用户标识补充请求头的场景。
        Sub2ApiLoginResponse.User user = loginResponse.getData().getUser();
        if (user != null && user.getId() != null) {
            sessionHeaders.put("sub2-api-user", String.valueOf(user.getId()));
        }

        // 【4】按 baseUrl + userName 维度缓存会话头。
        // 不同平台或不同账号的登录态不会互相覆盖。
        saveSessionHeaders(baseUrl, userName, sessionHeaders);
    }


    /**
     * 从 sub2api 数据库加载代理配置映射（账号名 -> ProxyConfig）
     */
    private Map<String, HttpsClient.ProxyConfig> loadProxyConfigMap(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return Map.of();
        }

        List<Sub2ApiAccountProxy> accountProxies = dataSourceSwitcher.use(
                DataSourceKey.SUB2_API,
                () -> sub2ApiAccountMapper.selectAccountProxiesByBaseUrl(baseUrl)
        );

        return toProxyConfigMap(accountProxies, log);
    }
}
