package com.sub2.monitor.strategy;

import com.sub2.monitor.client.HttpsClient;
import com.sub2.monitor.dto.sub2api.Sub2ApiAccountProxy;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共 API 策略基类。
 *
 * 维护原则：
 * 【1】这里只放 newapi / sub2api 都稳定复用的底层能力，例如 URL、请求头、会话、代理请求分发。
 * 【2】不要把具体业务流程抽到这里，避免阅读策略代码时来回跳转。
 * 【3】策略类负责表达“先登录、再采集、最后落库”的业务顺序；基类只负责降低重复样板代码。
 *
 * 新增平台策略时建议遵循：
 * 【1】先在新策略类中完整写出业务流程，确保登录、余额、渠道、模型等步骤读起来连续。
 * 【2】平台独有的字段映射、余额换算、响应结构解析优先留在策略类内部，避免过早抽象。
 * 【3】只有当某段逻辑被多个策略稳定复用，且业务含义一致时，再提升到本基类。
 * 【4】注释重点说明业务原因和扩展位置，不要简单复述代码做了什么。
 */
public abstract class AbstractApiStrategy<RQ, RS> {

    protected final HttpsClient httpsClient;

    protected AbstractApiStrategy(HttpsClient httpsClient) {
        this.httpsClient = httpsClient;
    }

    /**
     * 拼接完整请求地址。
     *
     * 【1】baseUrl 必填，避免后续请求打到错误地址。
     * 【2】自动处理 baseUrl 末尾是否带 `/`，调用方只需要传入标准 path。
     */
    protected String buildUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + path : baseUrl + path;
    }

    /**
     * 构建基础请求头。
     *
     * 【1】apiKey 不为空时写入 Bearer Authorization。
     * 【2】统一声明 JSON 请求体编码，避免各策略重复设置。
     */
    protected Map<String, String> buildHeaders(String apiKey) {
        Map<String, String> headers = new HashMap<>();
        if (apiKey != null && !apiKey.isBlank()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }
        headers.put("Content-Type", "application/json; charset=UTF-8");
        return headers;
    }

    /**
     * 合并基础请求头与当前 baseUrl + username 对应的会话请求头。
     *
     * 【1】先构建基础请求头。
     * 【2】再合并登录后缓存的 Cookie、Authorization 或用户标识。
     * 【3】会话维度是 baseUrl + username，避免不同平台或账号互相覆盖。
     */
    protected Map<String, String> mergeHeaders(String baseUrl, String userName, String apiKey) {
        Map<String, String> headers = new HashMap<>(buildHeaders(apiKey));
        Map<String, String> sessionHeaders = httpsClient.getSessionHeaders(buildSessionKey(baseUrl, userName));
        if (sessionHeaders != null && !sessionHeaders.isEmpty()) {
            headers.putAll(sessionHeaders);
        }
        return headers;
    }

    /**
     * 从响应头中提取多个 Set-Cookie。
     *
     * 【1】响应头大小写不固定，因此按 key 忽略大小写查找。
     * 【2】只保留 cookie 主体，丢弃 path、expires 等属性。
     * 【3】去重后返回，便于后续拼成 Cookie 请求头。
     */
    protected List<String> extractCookies(Map<String, List<String>> responseHeaders) {
        List<String> setCookieValues = null;
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            if ("set-cookie".equalsIgnoreCase(entry.getKey())) {
                setCookieValues = entry.getValue();
                break;
            }
        }
        if (setCookieValues == null || setCookieValues.isEmpty()) {
            return List.of();
        }
        return setCookieValues.stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 保存某个 baseUrl + username 对应的会话上下文。
     *
     * 【1】适用于只依赖 Cookie 的平台。
     * 【2】没有 Cookie 时不写入缓存，避免清空已有登录态。
     */
    protected void saveSessionContext(String baseUrl, String userName, List<String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return;
        }
        Map<String, String> sessionHeaders = new HashMap<>();
        sessionHeaders.put("Cookie", String.join("; ", cookies));
        httpsClient.putSessionHeaders(buildSessionKey(baseUrl, userName), sessionHeaders);
    }

    /**
     * 保存某个 baseUrl + username 对应的会话上下文，并附带用户标识。
     *
     * @param baseUrl 基础地址
     * @param userName 用户名
     * @param userId 用户标识
     * @param cookies Cookie 列表
     *
     * 【1】适用于 newapi 这类需要 Cookie + 用户标识的场景。
     * 【2】用户标识写入 `new-api-user`，后续请求会通过 mergeHeaders 自动带上。
     */
    protected void saveSessionContext(String baseUrl, String userName, String userId, List<String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return;
        }
        Map<String, String> sessionHeaders = new HashMap<>();
        sessionHeaders.put("Cookie", String.join("; ", cookies));
        if (userId != null && !userId.isBlank()) {
            sessionHeaders.put("new-api-user", userId);
        }
        httpsClient.putSessionHeaders(buildSessionKey(baseUrl, userName), sessionHeaders);
    }

    /**
     * 生成会话缓存键。
     *
     * 【1】baseUrl 是平台维度。
     * 【2】userName 是账号维度。
     * 【3】二者组合后可以同时支持多平台、多账号登录态缓存。
     */
    protected String buildSessionKey(String baseUrl, String userName) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (userName == null || userName.isBlank()) {
            return baseUrl;
        }
        return baseUrl + "::" + userName;
    }

    /**
     * 按代理配置发送 GET 请求。
     *
     * 【1】调用方先准备好 URL 和 headers。
     * 【2】这里统一判断是否走代理。
     * 【3】返回完整响应，适合需要读取响应头或状态码的场景。
     */
    protected HttpsClient.HttpResult get(String url, Map<String, String> headers, HttpsClient.ProxyConfig proxyConfig) {
        if (isProxyDisabled(proxyConfig)) {
            return httpsClient.getDirect(url, headers);
        }
        return httpsClient.getWithProxy(url, headers, proxyConfig);
    }

    /**
     * 按代理配置发送 GET 请求，并只返回响应体。
     *
     * 【1】适合只关心 body 的采集接口。
     * 【2】代理选择规则和完整 GET 保持一致。
     */
    protected String getBody(String url, Map<String, String> headers, HttpsClient.ProxyConfig proxyConfig) {
        if (isProxyDisabled(proxyConfig)) {
            return httpsClient.getBodyDirect(url, headers);
        }
        return httpsClient.getBodyWithProxy(url, headers, proxyConfig);
    }

    /**
     * 按代理配置发送 POST 请求。
     *
     * 【1】调用方先准备好 URL、headers 和 body。
     * 【2】这里统一判断是否走代理。
     * 【3】登录、提交类请求都复用这一套网络分发规则。
     */
    protected HttpsClient.HttpResult post(String url, Map<String, String> headers, Object body,
                                          HttpsClient.ProxyConfig proxyConfig) {
        if (isProxyDisabled(proxyConfig)) {
            return httpsClient.postDirect(url, headers, body);
        }
        return httpsClient.postWithProxy(url, headers, body, proxyConfig);
    }

    /**
     * 保存完整会话头。
     *
     * 【1】适用于 sub2api 这类登录后同时需要 Authorization、Cookie、自定义用户头的场景。
     * 【2】调用方负责组装业务头，这里只负责按账号维度缓存。
     * 【3】后续请求统一通过 mergeHeaders 合并登录态。
     */
    protected void saveSessionHeaders(String baseUrl, String userName, Map<String, String> sessionHeaders) {
        if (sessionHeaders == null || sessionHeaders.isEmpty()) {
            return;
        }
        httpsClient.putSessionHeaders(buildSessionKey(baseUrl, userName), sessionHeaders);
    }

    /**
     * 根据 sub2api 账号代理记录构建统一代理配置。
     *
     * 【1】这是代理配置的纯转换逻辑，不包含数据源查询。
     * 【2】缺少 proxyId、host、port 时返回 null。
     * 【3】协议只接受 http、socks5、socks；其他协议走直连，避免创建错误连接。
     */
    protected HttpsClient.ProxyConfig buildProxyConfig(Sub2ApiAccountProxy accountProxy, Logger logger) {
        if (accountProxy.getProxyId() == null
                || accountProxy.getProxyPort() == null
                || accountProxy.getProxyHost() == null
                || accountProxy.getProxyHost().isBlank()) {
            return null;
        }

        String proxyProtocol = accountProxy.getProxyProtocol();
        if (proxyProtocol != null
                && !proxyProtocol.isBlank()
                && !"http".equalsIgnoreCase(proxyProtocol)
                && !"socks5".equalsIgnoreCase(proxyProtocol)
                && !"socks".equalsIgnoreCase(proxyProtocol)) {
            logger.warn("不支持的代理协议，将使用直连: account={}, protocol={}",
                    accountProxy.getName(), proxyProtocol);
            return null;
        }

        return HttpsClient.ProxyConfig.builder()
                .enabled(true)
                .type(proxyProtocol)
                .host(accountProxy.getProxyHost())
                .port(accountProxy.getProxyPort())
                .build();
    }

    /**
     * 把代理记录列表转换成账号名到代理配置的映射。
     *
     * 【1】策略类负责查询代理记录。
     * 【2】基类只负责把记录转换成运行期需要的 Map。
     * 【3】账号名重复时保留第一条，让行为稳定、可预期。
     */
    protected Map<String, HttpsClient.ProxyConfig> toProxyConfigMap(List<Sub2ApiAccountProxy> accountProxies,
                                                                    Logger logger) {
        Map<String, HttpsClient.ProxyConfig> resultMap = new LinkedHashMap<>();
        for (Sub2ApiAccountProxy proxy : accountProxies) {
            if (proxy.getName() == null) {
                continue;
            }
            HttpsClient.ProxyConfig config = buildProxyConfig(proxy, logger);
            if (config != null) {
                resultMap.putIfAbsent(proxy.getName(), config);
            }
        }
        return resultMap;
    }

    private boolean isProxyDisabled(HttpsClient.ProxyConfig proxyConfig) {
        return proxyConfig == null || !proxyConfig.isEnabled();
    }

}
