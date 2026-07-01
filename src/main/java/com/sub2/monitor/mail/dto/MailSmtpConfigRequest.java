package com.sub2.monitor.mail.dto;

import lombok.Data;

@Data
public class MailSmtpConfigRequest {

    private String configName;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromEmail;
    private String fromName;
    private Integer enabled;
    private Integer useTls;
    private Integer useSsl;
    private Integer isDefault;
    private String remark;
}
