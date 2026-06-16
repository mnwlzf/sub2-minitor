package com.sub2.monitor.dto.newapi;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * newapi 渠道信息响应。
 *
 * data 的结构是动态 key 对象，例如：
 * {
 *   "data": {
 *     "gpt-image-2": { "desc": "生图专用", "ratio": 1 }
 *   }
 * }
 *
 * 这里直接把 data 接成 JSONObject，后续在业务层遍历 key/value。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiChannelResponse {

    private JSONObject data;
    private String message;
    private boolean success;
}
