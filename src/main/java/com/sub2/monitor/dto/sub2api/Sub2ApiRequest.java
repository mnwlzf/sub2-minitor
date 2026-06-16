package com.sub2.monitor.dto.sub2api;

import lombok.Data;

/**
 * sub2api 请求参数对象。
 *
 * 实际采集只需要平台 baseUrl；账号信息由策略按 baseUrl 从 accounts 表加载。
 */
@Data
public class Sub2ApiRequest {

    /**
     * 接口基础地址，例如：https://example.com
     */
    private String baseUrl;

    /**
     * API Key，用于鉴权。
     */
    private String apiKey;

    /**
     * 模型名称，例如：gpt-4o-mini。
     */
    private String model;
}
