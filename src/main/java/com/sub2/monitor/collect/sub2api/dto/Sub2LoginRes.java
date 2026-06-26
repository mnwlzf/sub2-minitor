package com.sub2.monitor.collect.sub2api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Sub2LoginRes {

    private Integer code;

    private String message;

    private DataInfo data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataInfo {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        private UserInfo user;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {

        private Long id;

        private String email;

        private String username;

        private String role;

        private BigDecimal balance;

        private Integer concurrency;

        private String status;

        @JsonProperty("allowed_groups")
        private List<Integer> allowedGroups;

        @JsonProperty("last_active_at")
        private String lastActiveAt;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        @JsonProperty("balance_notify_enabled")
        private Boolean balanceNotifyEnabled;

        @JsonProperty("balance_notify_threshold_type")
        private String balanceNotifyThresholdType;

        @JsonProperty("balance_notify_threshold")
        private BigDecimal balanceNotifyThreshold;

        @JsonProperty("balance_notify_extra_emails")
        private List<String> balanceNotifyExtraEmails;

        @JsonProperty("total_recharged")
        private BigDecimal totalRecharged;

        @JsonProperty("rpm_limit")
        private Integer rpmLimit;
    }
}
