package com.sub2.monitor.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AccountResponse {
    private Long id;
    private String username;
    private Long platformId;
    private String platformName;
    private String platformBaseUrl;
    private String platformType;
    private String testModel;
    private OffsetDateTime createTime;
}
