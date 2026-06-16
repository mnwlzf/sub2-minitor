package com.sub2.monitor.dto.sub2api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2ApiChannelResponse {
    private int code;
    private String message;
    private List<ChannelData> data;

    public boolean isSuccess() {
        return code == 0 && data != null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelData {
        private Long id;
        private String name;
        private String description;
        private String platform;

        @JsonProperty("rate_multiplier")
        private BigDecimal rateMultiplier;

        @JsonProperty("is_exclusive")
        private Boolean exclusive;

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
        private Boolean allowImageGeneration;

        @JsonProperty("image_rate_independent")
        private Boolean imageRateIndependent;

        @JsonProperty("image_rate_multiplier")
        private BigDecimal imageRateMultiplier;

        @JsonProperty("image_price_1k")
        private BigDecimal imagePrice1k;

        @JsonProperty("image_price_2k")
        private BigDecimal imagePrice2k;

        @JsonProperty("image_price_4k")
        private BigDecimal imagePrice4k;

        @JsonProperty("claude_code_only")
        private Boolean claudeCodeOnly;

        @JsonProperty("fallback_group_id")
        private Long fallbackGroupId;

        @JsonProperty("fallback_group_id_on_invalid_request")
        private Long fallbackGroupIdOnInvalidRequest;

        @JsonProperty("allow_messages_dispatch")
        private Boolean allowMessagesDispatch;

        @JsonProperty("require_oauth_only")
        private Boolean requireOauthOnly;

        @JsonProperty("require_privacy_set")
        private Boolean requirePrivacySet;

        @JsonProperty("rpm_limit")
        private Integer rpmLimit;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;
    }
}
