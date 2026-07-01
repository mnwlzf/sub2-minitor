package com.sub2.monitor.monitor.dto;

import lombok.Data;

@Data
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private Long platformId;
    private String platformName;
    private String platformType;
    private String testModel;
    private Boolean isCollect;
}
