package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("mail_send_log")
public class MailSendLog {
    private Long id;
    private String sceneKey;
    private String subject;
    private String toEmails;
    private String ccEmails;
    private String bccEmails;
    private String status;
    private String errorMessage;
    private OffsetDateTime sendTime;
    private OffsetDateTime createTime;
}
