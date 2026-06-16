package com.sub2.monitor.dto.sub2api;

import lombok.Data;

/**
 * sub2api 库中账号和代理的联合查询结果。
 *
 * <p>该对象不是主库实体，而是 {@code sub2api.accounts} 关联
 * {@code sub2api.proxies} 后返回给监控逻辑使用的只读 DTO。</p>
 */
@Data
public class Sub2ApiAccountProxy {
    /**
     * sub2api.accounts.id。
     */
    private Long accountId;

    /**
     * sub2api.accounts.name，当前实现用它和 NewApiRequest 中的 userName 匹配。
     */
    private String name;

    /**
     * 从 accounts.credentials JSON 中提取的 api_key。
     */
    private String apiKey;

    /**
     * 从 accounts.credentials JSON 中提取的 base_url。
     */
    private String baseUrl;

    /**
     * accounts.proxy_id；为空表示该账号不需要代理，后续网络请求直接连接。
     */
    private Long proxyId;

    /**
     * proxies.protocol；当前支持 http、socks、socks5。
     */
    private String proxyProtocol;

    /**
     * proxies.host。
     */
    private String proxyHost;

    /**
     * proxies.port。
     */
    private Integer proxyPort;

    /**
     * proxies.username，代理需要认证时使用。
     */
    private String proxyUsername;

    /**
     * proxies.password，代理需要认证时使用。
     */
    private String proxyPassword;
}
