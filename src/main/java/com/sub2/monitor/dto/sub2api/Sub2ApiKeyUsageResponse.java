package com.sub2.monitor.dto.sub2api;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2ApiKeyUsageResponse {
    private int code;
    private String message;
    private UsageData data;

    public boolean isSuccess() {
        return code == 0 && data != null && data.getStats() != null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageData {
        private JSONObject stats;
    }
}
