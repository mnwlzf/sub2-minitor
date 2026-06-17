package com.sub2.monitor.strategy.newapi;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.client.HttpsClient;
import com.sub2.monitor.config.datasource.DataSourceKey;
import com.sub2.monitor.config.datasource.DataSourceSwitcher;
import com.sub2.monitor.dto.LoginRequest;
import com.sub2.monitor.dto.newapi.NewApiAccountResponse;
import com.sub2.monitor.dto.newapi.NewApiChannelResponse;
import com.sub2.monitor.dto.newapi.NewApiLoginResponse;
import com.sub2.monitor.dto.newapi.NewApiRequest;
import com.sub2.monitor.dto.newapi.NewApiResponse;
import com.sub2.monitor.dto.sub2api.Sub2ApiAccountProxy;
import com.sub2.monitor.entity.AccountApiKeyGroup;
import com.sub2.monitor.entity.AccountBalanceHistory;
import com.sub2.monitor.entity.Accounts;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.entity.PlatformRateHistory;
import com.sub2.monitor.mapper.AccountApiKeyGroupMapper;
import com.sub2.monitor.mapper.AccountBalanceHistoryMapper;
import com.sub2.monitor.mapper.AccountsMapper;
import com.sub2.monitor.mapper.PlatformMapper;
import com.sub2.monitor.mapper.PlatformRateHistoryMapper;
import com.sub2.monitor.mapper.sub2api.Sub2ApiAccountMapper;
import com.sub2.monitor.strategy.AbstractApiStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NewAPI 平台策略实现
 * 负责登录、余额采集、渠道（分组）采集、模型可用性测试
 */
@Component
@Slf4j
public class NewApiStrategy extends AbstractApiStrategy<NewApiRequest, NewApiResponse> {

    // 常量定义：消除魔法数字
    private static final long LOGIN_REQUEST_INTERVAL_MS = 3000L;  // 登录请求间隔
    private static final BigDecimal QUOTA_TO_BALANCE_DIVISOR = BigDecimal.valueOf(500_000); // quota转余额的除数

    public NewApiStrategy(HttpsClient httpsClient) {
        super(httpsClient);
    }

    @Autowired
    private PlatformMapper platformMapper;

    @Autowired
    private AccountsMapper accountsMapper;

    @Autowired
    private AccountApiKeyGroupMapper accountApiKeyGroupMapper;

    @Autowired
    private AccountBalanceHistoryMapper accountBalanceHistoryMapper;

    @Autowired
    private PlatformRateHistoryMapper platformRateHistoryMapper;

    @Autowired
    private Sub2ApiAccountMapper sub2ApiAccountMapper;

    @Autowired
    private DataSourceSwitcher dataSourceSwitcher;


    /**
     * 执行完整任务流程：登录 -> 采集余额 -> 采集渠道 -> 测试模型
     */
    public NewApiResponse execute(NewApiRequest request) {
        TaskContext context = prepareRequest(request);
        Platform platform = context.platform();
        log.info("开始执行 NewAPI 任务, baseUrl={}, 账号数={}",
                request.getBaseUrl(),
                context.accounts().size());

        // 【2】加载账号代理配置，后续登录、余额、渠道、模型请求共用同一份映射。
        Map<String, HttpsClient.ProxyConfig> proxyConfigMap = loadProxyConfigMap(request.getBaseUrl());

        // 【3】按业务顺序执行：登录 -> 余额 -> 渠道 -> 模型探测。
        login(request, context, proxyConfigMap);
        collectBalance(request, context, proxyConfigMap);
        NewApiResponse channelResponse = collectChannels(request, context, proxyConfigMap);
        collectApiKeyGroups(request, context, proxyConfigMap, channelResponse.getChannelResults());
        NewApiResponse modelResponse = testModelAvailabilityByAccounts(request, context, proxyConfigMap);

        // 【4】聚合渠道和模型结果，作为本次完整采集的响应。
        NewApiResponse response = new NewApiResponse();
        response.setChannelResults(channelResponse.getChannelResults());
        response.setAccountResults(modelResponse.getAccountResults());
        response.setRawBody(JSONUtil.toJsonStr(Map.of(
                "channels", channelResponse.getChannelResults(),
                "models", modelResponse.getAccountResults()
        )));

        log.info("NewAPI 任务完成, 渠道数={}, 模型测试数={}",
                channelResponse.getChannelResults().size(),
                modelResponse.getAccountResults().size());
        return response;
    }

    private void collectApiKeyGroups(NewApiRequest request,
                                     TaskContext context,
                                     Map<String, HttpsClient.ProxyConfig> proxyConfigMap,
                                     List<NewApiResponse.ChannelResult> channelResults) {
        Map<String, BigDecimal> rateMap = toChannelRateMap(channelResults);
        Map<String, Long> accountIdMap = loadAccountIdMap(context.accounts(), context.platform().getId());
        List<AccountApiKeyGroup> keyGroups = new ArrayList<>();
        OffsetDateTime collectTime = OffsetDateTime.now();

        for (AccountCredential account : context.accounts()) {
            Long accountId = accountIdMap.get(account.userName());
            if (accountId == null) {
                log.warn("本地无此账号，跳过 NewApi 密钥分组采集: {}", logContext(context, account.userName()));
                continue;
            }
            keyGroups.addAll(fetchNewApiKeyGroups(request, context, account, accountId, rateMap, proxyConfigMap, collectTime));
        }

        if (!keyGroups.isEmpty()) {
            accountApiKeyGroupMapper.upsertBatch(keyGroups);
            log.info("NewApi 密钥分组已保存, platformId={}, count={}", context.platform().getId(), keyGroups.size());
        }
    }

    private List<AccountApiKeyGroup> fetchNewApiKeyGroups(NewApiRequest request,
                                                          TaskContext context,
                                                          AccountCredential account,
                                                          Long accountId,
                                                          Map<String, BigDecimal> rateMap,
                                                          Map<String, HttpsClient.ProxyConfig> proxyConfigMap,
                                                          OffsetDateTime collectTime) {
        List<AccountApiKeyGroup> result = new ArrayList<>();
        int page = 1;
        int pageSize = 100;
        while (true) {
            String url = buildUrl(request.getBaseUrl(), "/api/token/?p=" + page + "&size=" + pageSize);
            String responseBody = getBody(url,
                    mergeHeaders(request.getBaseUrl(), account.userName(), request.getApiKey()),
                    proxyConfigMap.get(account.userName()));
            JSONObject root = JSONUtil.parseObj(responseBody);
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                break;
            }
            JSONArray items = data.getJSONArray("items");
            if (items == null || items.isEmpty()) {
                break;
            }
            for (Object itemObj : items) {
                JSONObject item = JSONUtil.parseObj(itemObj);
                String groupName = item.getStr("group");
                BigDecimal currentRate = rateMap.getOrDefault(groupName, BigDecimal.ZERO);
                AccountApiKeyGroup keyGroup = baseKeyGroup(context, account, accountId, String.valueOf(item.getLong("id")), groupName, currentRate, collectTime);
                keyGroup.setKeyName(item.getStr("name"));
                keyGroup.setKeyStatus(String.valueOf(item.getInt("status", 0)));
                keyGroup.setUsedAmount(toBalanceAmount(item.getBigDecimal("used_quota")));
                keyGroup.setRemainAmount(toBalanceAmount(item.getBigDecimal("remain_quota")));
                result.add(keyGroup);
            }
            Integer total = data.getInt("total");
            if (total == null || page * pageSize >= total) {
                break;
            }
            page++;
        }
        return result;
    }

    private Map<String, BigDecimal> toChannelRateMap(List<NewApiResponse.ChannelResult> channelResults) {
        if (channelResults == null || channelResults.isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> rateMap = new LinkedHashMap<>();
        for (NewApiResponse.ChannelResult result : channelResults) {
            if (StrUtil.isBlank(result.getGroupName())) {
                continue;
            }
            rateMap.putIfAbsent(result.getGroupName(), BigDecimal.valueOf(result.getRatio() == null ? 0D : result.getRatio()));
        }
        return rateMap;
    }

    private AccountApiKeyGroup baseKeyGroup(TaskContext context,
                                            AccountCredential account,
                                            Long accountId,
                                            String remoteKeyId,
                                            String groupName,
                                            BigDecimal currentRate,
                                            OffsetDateTime collectTime) {
        OffsetDateTime now = OffsetDateTime.now();
        AccountApiKeyGroup keyGroup = new AccountApiKeyGroup();
        keyGroup.setPlatformId(context.platform().getId());
        keyGroup.setAccountId(accountId);
        keyGroup.setUsername(account.userName());
        keyGroup.setPlatformType(context.platform().getType());
        keyGroup.setRemoteKeyId(remoteKeyId);
        keyGroup.setGroupName(groupName);
        keyGroup.setCurrentRate(currentRate);
        keyGroup.setActualRate(currentRate.multiply(toDeductRate(context.platform())).setScale(8, RoundingMode.HALF_UP));
        keyGroup.setCollectTime(collectTime);
        keyGroup.setCreateTime(now);
        keyGroup.setUpdateTime(now);
        return keyGroup;
    }

    private BigDecimal toBalanceAmount(BigDecimal quota) {
        if (quota == null) {
            return null;
        }
        return quota.divide(QUOTA_TO_BALANCE_DIVISOR, 8, RoundingMode.HALF_UP);
    }


    public void login(NewApiRequest request) {
        TaskContext context = prepareRequest(request);
        login(request, context, loadProxyConfigMap(request.getBaseUrl()));
    }

    /**
     * 内部登录实现（支持代理）
     */
    private void login(NewApiRequest request, TaskContext context, Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        if (context.accounts() == null || context.accounts().isEmpty()) {
            throw new IllegalArgumentException("accounts is required");
        }

        String loginUrl = buildUrl(request.getBaseUrl(), "/api/user/login?turnstile=");

        for (AccountCredential account : context.accounts()) {
            // 【1】控制登录间隔，降低触发目标平台限流的概率。
            try {
                Thread.sleep(LOGIN_REQUEST_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("登录线程被中断", e);
                break;
            }

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(account.userName());
            loginRequest.setPassword(account.password());

            try {
                HttpsClient.HttpResult result = post(loginUrl, buildHeaders(request.getApiKey()), loginRequest,
                        proxyConfigMap.get(account.userName()));
                NewApiLoginResponse loginResponse = JSONUtil.toBean(result.getBody(), NewApiLoginResponse.class);
                if (loginResponse != null && loginResponse.isSuccess() && loginResponse.getData() != null) {
                    saveSessionContext(
                            request.getBaseUrl(),
                            account.userName(),
                            String.valueOf(loginResponse.getData().getId()),
                            extractCookies(result.getHeaders())
                    );
                    log.info("登录成功: {}", logContext(context, account.userName()));
                } else {
                    log.warn("登录失败: {}, response={}", logContext(context, account.userName()), result.getBody());
                }
            } catch (Exception e) {
                log.error("登录请求异常: {}", logContext(context, account.userName()), e);
                // 【2】单账号失败不影响本批次其他账号。
            }
        }
    }


    public void collectBalance(NewApiRequest request, Long platformId, String platformName) {
        TaskContext context = prepareRequest(request);
        collectBalance(request, context, loadProxyConfigMap(request.getBaseUrl()));
    }

    /**
     * 内部余额采集实现（支持代理）
     */
    private void collectBalance(NewApiRequest request, TaskContext context, Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        List<NewApiAccountResponse.AccountData> results = new ArrayList<>();
        List<String> usernames = new ArrayList<>();

        // 【1】逐账号请求余额接口，先收集远端返回的账号余额数据。
        for (AccountCredential credential : context.accounts()) {
            String url = buildUrl(request.getBaseUrl(), "/api/user/self");
            Map<String, String> headers = mergeHeaders(request.getBaseUrl(), credential.userName(), request.getApiKey());
            String responseBody = getBody(url, headers, proxyConfigMap.get(credential.userName()));
            NewApiAccountResponse accountResponse = JSONUtil.toBean(responseBody, NewApiAccountResponse.class);
            if (accountResponse == null || accountResponse.getData() == null) {
                log.warn("余额响应为空: {}", logContext(context, credential.userName()));
                continue;
            }
            NewApiAccountResponse.AccountData data = accountResponse.getData();
            results.add(data);
            usernames.add(data.getUsername());
        }

        if (usernames.isEmpty()) {
            return;
        }

        // 【2】查询本地账号 ID。必须带 platformId，避免不同平台同名账号串数据。
        Map<String, Long> accountIdMap = accountsMapper.selectByNames(usernames, context.platform().getId()).stream()
                .collect(Collectors.toMap(Accounts::getUsername, Accounts::getId, (old, ignored) -> old));

        // 【3】构建余额历史。这里保留直观循环，方便维护者排查单账号问题。
        List<AccountBalanceHistory> historyList = new ArrayList<>();
        for (NewApiAccountResponse.AccountData result : results) {
            Long accountId = accountIdMap.get(result.getUsername());
            if (accountId == null) {
                log.warn("本地无此账号: {}", logContext(context, result.getUsername()));
                continue;
            }
            AccountBalanceHistory history = new AccountBalanceHistory();
            history.setPlatformId(context.platform().getId());
            history.setAccountId(accountId);
            // 【4】NewAPI 的 quota 需要转换成实际余额：除以 500000，保留 2 位小数。
            history.setCurrentBalance(BigDecimal.valueOf(result.getQuota())
                    .divide(QUOTA_TO_BALANCE_DIVISOR, 2, RoundingMode.HALF_UP));
            history.setCreateTime(OffsetDateTime.now());
            history.setPlatform(context.platform().getName());
            historyList.add(history);
        }

        if (!historyList.isEmpty()) {
            accountBalanceHistoryMapper.insertBatch(historyList);
            log.info("批量插入余额历史, 数量={}", historyList.size());
        }
    }


    public NewApiResponse collectChannels(NewApiRequest request, Long platformId) {
        TaskContext context = prepareRequest(request);
        return collectChannels(request, context, loadProxyConfigMap(request.getBaseUrl()));
    }

    /**
     * 内部渠道采集实现（支持代理）
     */
    private NewApiResponse collectChannels(NewApiRequest request, TaskContext context, Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        List<NewApiResponse.ChannelResult> results = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        // 【1】逐账号读取分组信息，分组即当前平台的渠道。
        for (AccountCredential account : context.accounts()) {
            String url = buildUrl(request.getBaseUrl(), "/api/user/self/groups");
            String responseBody = getBody(url,
                    mergeHeaders(request.getBaseUrl(), account.userName(), request.getApiKey()),
                    proxyConfigMap.get(account.userName()));
            NewApiChannelResponse channelResponse = JSONUtil.toBean(responseBody, NewApiChannelResponse.class);
            if (channelResponse == null || channelResponse.getData() == null) {
                log.warn("渠道响应为空: {}", logContext(context, account.userName()));
                continue;
            }
            JSONObject data = channelResponse.getData();
            for (String groupName : data.keySet()) {
                JSONObject groupObject = data.getJSONObject(groupName);
                if (groupObject == null) {
                    continue;
                }
                NewApiResponse.ChannelResult channelResult = new NewApiResponse.ChannelResult();
                channelResult.setUserName(account.userName());
                channelResult.setGroupName(groupName);
                channelResult.setDesc(groupObject.getStr("desc"));
                channelResult.setRatio(groupObject.getDouble("ratio"));
                results.add(channelResult);
            }
        }

        // 【2】按分组名称去重并转换为平台费率历史，多个账号看到同名渠道时只保留首次结果。
        Map<String, NewApiResponse.ChannelResult> uniqueMap = new LinkedHashMap<>();
        for (NewApiResponse.ChannelResult cr : results) {
            uniqueMap.putIfAbsent(cr.getGroupName(), cr);
        }

        List<PlatformRateHistory> rateHistories = new ArrayList<>();
        for (Map.Entry<String, NewApiResponse.ChannelResult> entry : uniqueMap.entrySet()) {
            NewApiResponse.ChannelResult cr = entry.getValue();
            PlatformRateHistory rateHistory = new PlatformRateHistory();
            rateHistory.setPlatformId(context.platform().getId());
            rateHistory.setChannelName(cr.getGroupName());
            rateHistory.setCurrentRate(BigDecimal.valueOf(cr.getRatio() == null ? 0D : cr.getRatio()));
            rateHistory.setCreateTime(now);
            rateHistories.add(rateHistory);
        }

        if (!rateHistories.isEmpty()) {
            platformRateHistoryMapper.insertBatch(rateHistories);
            log.info("批量插入渠道费率历史, 数量={}", rateHistories.size());
        }

        NewApiResponse response = new NewApiResponse();
        response.setChannelResults(results);
        response.setRawBody(JSONUtil.toJsonStr(rateHistories));
        return response;
    }


    public NewApiResponse testModelAvailability(NewApiRequest request) {
        TaskContext context = prepareRequest(request);
        return testModelAvailabilityByAccounts(request, context, loadProxyConfigMap(request.getBaseUrl()));
    }

    /**
     * 内部模型测试实现（支持代理）
     * 注意：只请求模型是否存在，不判断返回内容（可扩展）
     */
    private NewApiResponse testModelAvailabilityByAccounts(NewApiRequest request,
                                                           TaskContext context,
                                                           Map<String, HttpsClient.ProxyConfig> proxyConfigMap) {
        List<NewApiResponse.AccountResult> results = new ArrayList<>();

        for (AccountCredential account : context.accounts()) {
            String url = buildUrl(request.getBaseUrl(), "/v1/models/" + request.getModel());
            String responseBody = getBody(url,
                    mergeHeaders(request.getBaseUrl(), account.userName(), request.getApiKey()),
                    proxyConfigMap.get(account.userName()));
            NewApiResponse.AccountResult accountResult = new NewApiResponse.AccountResult();
            accountResult.setUserName(account.userName());
            accountResult.setType("model");
            accountResult.setRawBody(responseBody);
            results.add(accountResult);
        }

        NewApiResponse response = new NewApiResponse();
        response.setAccountResults(results);
        response.setRawBody(JSONUtil.toJsonStr(results));
        return response;
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

    private Map<String, Long> loadAccountIdMap(List<AccountCredential> accounts, Long platformId) {
        List<String> usernames = accounts.stream()
                .map(AccountCredential::userName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        if (usernames.isEmpty()) {
            return Map.of();
        }
        return accountsMapper.selectByNames(usernames, platformId).stream()
                .collect(Collectors.toMap(Accounts::getUsername, Accounts::getId, (old, ignored) -> old));
    }

    private BigDecimal toDeductRate(Platform platform) {
        BigDecimal rechargeAmount = platform.getRechargeAmount() == null ? BigDecimal.ZERO : platform.getRechargeAmount();
        BigDecimal receivedAmount = platform.getReceivedAmount() == null ? BigDecimal.ZERO : platform.getReceivedAmount();
        if (receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return rechargeAmount.divide(receivedAmount, 8, RoundingMode.HALF_UP);
    }

    private TaskContext prepareRequest(NewApiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        Platform platform = findPlatform(request.getBaseUrl());
        List<Accounts> accounts = loadAccounts(platform.getId(), request.getBaseUrl());
        fillDefaultModel(request, accounts);
        return new TaskContext(platform, toCredentials(accounts));
    }

    private Platform findPlatform(String baseUrl) {
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

    private void fillDefaultModel(NewApiRequest request, List<Accounts> accounts) {
        if (StrUtil.isNotBlank(request.getModel())) {
            return;
        }
        accounts.stream()
                .map(Accounts::getTestModel)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .ifPresent(request::setModel);
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
}
