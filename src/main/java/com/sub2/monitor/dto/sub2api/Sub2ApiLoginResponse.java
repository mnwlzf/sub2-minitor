package com.sub2.monitor.dto.sub2api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sub2ApiLoginResponse {
    private int code;
    private String message;
    private LoginData data;

    public boolean isSuccess() {
        return code == 0 && data != null && data.accessToken != null && !data.accessToken.isBlank();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        private User user;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private Long id;
        private String email;
        private String username;
        private String role;
        private BigDecimal balance;
        private Integer concurrency;
        private String status;

        @JsonProperty("allowed_groups")
        private List<Long> allowedGroups;

        @JsonProperty("last_active_at")
        private OffsetDateTime lastActiveAt;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("balance_notify_enabled")
        private Boolean balanceNotifyEnabled;

        @JsonProperty("balance_notify_threshold_type")
        private String balanceNotifyThresholdType;

        @JsonProperty("balance_notify_threshold")
        private BigDecimal balanceNotifyThreshold;

        @JsonProperty("balance_notify_extra_emails")
        private String balanceNotifyExtraEmails;

        @JsonProperty("total_recharged")
        private BigDecimal totalRecharged;

        @JsonProperty("rpm_limit")
        private Integer rpmLimit;
    }
}
