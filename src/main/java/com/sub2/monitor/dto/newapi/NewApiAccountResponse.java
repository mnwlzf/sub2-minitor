package com.sub2.monitor.dto.newapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiAccountResponse {

    private AccountData data;
    private String message;
    private boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccountData {

        @JsonProperty("aff_code")
        private String affCode;

        @JsonProperty("aff_count")
        private int affCount;

        @JsonProperty("aff_history_quota")
        private long affHistoryQuota;

        @JsonProperty("aff_quota")
        private long affQuota;

        @JsonProperty("discord_id")
        private String discordId;

        @JsonProperty("display_name")
        private String displayName;

        private String email;

        @JsonProperty("github_id")
        private String githubId;

        private String group;
        private int id;

        @JsonProperty("inviter_id")
        private int inviterId;

        @JsonProperty("linux_do_id")
        private String linuxDoId;

        @JsonProperty("oidc_id")
        private String oidcId;

        private long quota;

        @JsonProperty("request_count")
        private int requestCount;

        private int role;
        private String setting;      // JSON 字符串

        @JsonProperty("sidebar_modules")
        private String sidebarModules;  // JSON 字符串

        private int status;

        @JsonProperty("stripe_customer")
        private String stripeCustomer;

        @JsonProperty("telegram_id")
        private String telegramId;

        @JsonProperty("used_quota")
        private long usedQuota;

        private String username;

        @JsonProperty("wechat_id")
        private String wechatId;

    }
}