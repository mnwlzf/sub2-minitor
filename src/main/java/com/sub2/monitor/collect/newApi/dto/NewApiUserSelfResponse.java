package com.sub2.monitor.collect.newApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiUserSelfResponse {

    private UserInfo data;
    private String message;
    private Boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private Long quota;
        @JsonProperty("used_quota")
        private Long usedQuota;
    }
}
