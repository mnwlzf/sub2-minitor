package com.sub2.monitor.collect.newApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiTokensResponse {

    private Boolean success;
    private String message;
    private TokenPage data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenPage {
        private int page;
        @JsonProperty("page_size")
        private int pageSize;
        private int total;
        private List<TokenItem> items;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenItem {
        private Long id;
        @JsonProperty("user_id")
        private Long userId;
        private String key;
        private Integer status;
        private String name;
        @JsonProperty("created_time")
        private Long createdTime;
        @JsonProperty("accessed_time")
        private Long accessedTime;
        @JsonProperty("expired_time")
        private Long expiredTime;
        @JsonProperty("remain_quota")
        private Long remainQuota;
        @JsonProperty("unlimited_quota")
        private Boolean unlimitedQuota;
        @JsonProperty("model_limits_enabled")
        private Boolean modelLimitsEnabled;
        @JsonProperty("model_limits")
        private String modelLimits;
        @JsonProperty("allow_ips")
        private String allowIps;
        @JsonProperty("used_quota")
        private Long usedQuota;
        private String group;
        @JsonProperty("cross_group_retry")
        private Boolean crossGroupRetry;
        @JsonProperty("DeletedAt")
        private String deletedAt;
    }
}
