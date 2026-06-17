package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("mail_recipient")
public class MailRecipient {
    private Long id;
    private String email;
    private String name;
    private Boolean isEnabled;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
