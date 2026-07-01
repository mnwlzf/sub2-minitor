package com.sub2.monitor.mail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mail_smtp_config")
public class MailSmtpConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_name")
    private String configName;

    private String host;

    private Integer port;

    private String username;

    private String password;

    @TableField("from_email")
    private String fromEmail;

    @TableField("from_name")
    private String fromName;

    private Integer enabled;

    @TableField("use_tls")
    private Integer useTls;

    @TableField("use_ssl")
    private Integer useSsl;

    @TableField("is_default")
    private Integer isDefault;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
