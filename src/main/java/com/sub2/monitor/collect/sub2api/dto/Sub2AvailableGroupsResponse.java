package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2AvailableGroupsResponse extends Mono<Sub2AvailableGroupsResponse> {
    private int code;
    private String message;
    private List<GroupItem> data;

    @Override
    public void subscribe(CoreSubscriber<? super Sub2AvailableGroupsResponse> actual) {

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupItem {
        private int id;
        private String name;
        private String description;
        private String platform;
        @JsonProperty("rate_multiplier")
        private double rateMultiplier;
        @JsonProperty("is_exclusive")
        private boolean exclusive;
        private String status;
        @JsonProperty("subscription_type")
        private String subscriptionType;
        @JsonProperty("daily_limit_usd")
        private double dailyLimitUsd;
        @JsonProperty("weekly_limit_usd")
        private double weeklyLimitUsd;
        @JsonProperty("monthly_limit_usd")
        private double monthlyLimitUsd;
        @JsonProperty("allow_image_generation")
        private boolean allowImageGeneration;
        @JsonProperty("image_rate_independent")
        private boolean imageRateIndependent;
        @JsonProperty("image_rate_multiplier")
        private double imageRateMultiplier;
        @JsonProperty("image_price_1k")
        private Double imagePrice1k;
        @JsonProperty("image_price_2k")
        private Double imagePrice2k;
        @JsonProperty("image_price_4k")
        private Double imagePrice4k;
        @JsonProperty("claude_code_only")
        private boolean claudeCodeOnly;
        @JsonProperty("fallback_group_id")
        private Integer fallbackGroupId;
        @JsonProperty("fallback_group_id_on_invalid_request")
        private Integer fallbackGroupIdOnInvalidRequest;
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
