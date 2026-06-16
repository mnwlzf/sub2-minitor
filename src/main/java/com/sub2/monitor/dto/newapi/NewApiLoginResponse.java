package com.sub2.monitor.dto.newapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * newapi 登录接口响应。
 *
 * 对应结构：
 * {
 *   "data": { "display_name", "group", "id", "role", "status", "username" },
 *   "message": "",
 *   "success": true
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewApiLoginResponse {

    private LoginData data;
    private String message;
    private boolean success;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginData {
        private String displayName;
        private String group;
        private int id;
        private int role;
        private int status;
        private String username;
    }
}
