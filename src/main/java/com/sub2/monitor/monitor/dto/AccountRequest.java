package com.sub2.monitor.monitor.dto;

import lombok.Data;

@Data
public class AccountRequest {
    private Long platformId;
    private String username;
    private String email;
    private String password;
    private String testModel;
    private Boolean isCollect;
}
