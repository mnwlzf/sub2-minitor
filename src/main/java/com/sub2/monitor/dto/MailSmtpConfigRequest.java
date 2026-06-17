package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class MailSmtpConfigRequest {
    private Long id;
    private String configName;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromEmail;
    private String fromName;
    private Boolean useTls;
    private Boolean useSsl;
    private Boolean isEnabled;
    private Boolean isDefault;
}
