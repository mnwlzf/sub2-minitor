package com.sub2.monitor.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MailSmtpConfigResponse {
    private Long id;
    private String configName;
    private String host;
    private Integer port;
    private String username;
    private Boolean passwordConfigured;
    private String fromEmail;
    private String fromName;
    private Boolean useTls;
    private Boolean useSsl;
    private Boolean isEnabled;
    private Boolean isDefault;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
