package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("mail_smtp_config")
public class MailSmtpConfig {
    private Long id;
    private String configName;
    private String host;
    private Integer port;
    private String username;
    private String passwordEncrypted;
    private String fromEmail;
    private String fromName;
    private Boolean useTls;
    private Boolean useSsl;
    private Boolean isEnabled;
    private Boolean isDefault;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
