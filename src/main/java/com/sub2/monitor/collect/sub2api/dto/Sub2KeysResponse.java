package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2KeysResponse {

    private int code;
    private String message;
    private KeyPage data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyPage {
        private List<KeyItem> items;
        private int total;
        private int page;
        @JsonProperty("page_size")
        private int pageSize;
        private int pages;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyItem {
        private Long id;
        @JsonProperty("user_id")
        private Long userId;
        private String key;
        private String name;
        @JsonProperty("group_id")
        private Long groupId;
        private String status;
        @JsonProperty("ip_whitelist")
        private String ipWhitelist;
        @JsonProperty("ip_blacklist")
        private String ipBlacklist;
        @JsonProperty("last_used_at")
        private String lastUsedAt;
        private BigDecimal quota;
        @JsonProperty("quota_used")
        private BigDecimal quotaUsed;
        @JsonProperty("expires_at")
        private String expiresAt;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("rate_limit_5h")
        private BigDecimal rateLimit5h;
        @JsonProperty("rate_limit_1d")
        private BigDecimal rateLimit1d;
        @JsonProperty("rate_limit_7d")
        private BigDecimal rateLimit7d;
        @JsonProperty("usage_5h")
        private BigDecimal usage5h;
        @JsonProperty("usage_1d")
        private BigDecimal usage1d;
        @JsonProperty("usage_7d")
        private BigDecimal usage7d;
        @JsonProperty("window_5h_start")
        private String window5hStart;
        @JsonProperty("window_1d_start")
        private String window1dStart;
        @JsonProperty("window_7d_start")
        private String window7dStart;
        private GroupInfo group;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupInfo {
        private Long id;
        private String name;
        private String description;
        private String platform;
        @JsonProperty("rate_multiplier")
        private BigDecimal rateMultiplier;
        @JsonProperty("is_exclusive")
        private boolean exclusive;
        private String status;
        @JsonProperty("subscription_type")
        private String subscriptionType;
        @JsonProperty("daily_limit_usd")
        private BigDecimal dailyLimitUsd;
        @JsonProperty("weekly_limit_usd")
        private BigDecimal weeklyLimitUsd;
        @JsonProperty("monthly_limit_usd")
        private BigDecimal monthlyLimitUsd;
        @JsonProperty("allow_image_generation")
        private boolean allowImageGeneration;
        @JsonProperty("image_rate_independent")
        private boolean imageRateIndependent;
        @JsonProperty("image_rate_multiplier")
        private BigDecimal imageRateMultiplier;
        @JsonProperty("image_price_1k")
        private BigDecimal imagePrice1k;
        @JsonProperty("image_price_2k")
        private BigDecimal imagePrice2k;
        @JsonProperty("image_price_4k")
        private BigDecimal imagePrice4k;
        @JsonProperty("claude_code_only")
        private boolean claudeCodeOnly;
        @JsonProperty("fallback_group_id")
        private Long fallbackGroupId;
        @JsonProperty("fallback_group_id_on_invalid_request")
        private Long fallbackGroupIdOnInvalidRequest;
        @JsonProperty("allow_messages_dispatch")
        private boolean allowMessagesDispatch;
        @JsonProperty("require_oauth_only")
        private boolean requireOauthOnly;
        @JsonProperty("require_privacy_set")
        private boolean requirePrivacySet;
        @JsonProperty("rpm_limit")
        private int rpmLimit;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }
}
