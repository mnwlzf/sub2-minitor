package com.sub2.monitor.dto.sub2api;

import lombok.Data;

/**
 * sub2api 响应结果对象。
 *
 * 用于承载 sub2api 接口返回的原始字段或业务解析结果。
 */
@Data
public class Sub2ApiResponse {

    /**
     * 原始响应内容。
     */
    private String rawBody;
}
