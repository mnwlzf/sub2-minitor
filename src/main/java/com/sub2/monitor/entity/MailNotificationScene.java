package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("mail_notification_scene")
public class MailNotificationScene {
    private Long id;
    private String sceneKey;
    private String sceneName;
    private String description;
    private Boolean isEnabled;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
