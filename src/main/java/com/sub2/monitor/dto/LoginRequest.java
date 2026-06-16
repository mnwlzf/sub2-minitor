package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String email;
    private String password;
}
