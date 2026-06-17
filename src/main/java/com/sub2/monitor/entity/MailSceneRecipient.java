package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("mail_scene_recipient")
public class MailSceneRecipient {
    private Long id;
    private String sceneKey;
    private Long recipientId;
    private String recipientType;
    private OffsetDateTime createTime;
}
