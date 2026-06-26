package com.sub2.monitor.collect.newApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiLoginRes {

    private UserInfo data;

    private String message;

    private Boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {

        @JsonProperty("display_name")
        private String displayName;

        private String group;

        private Long id;

        private Integer role;

        private Integer status;

        private String username;
    }
}
