package com.sub2.monitor.mail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mail_notify_scene_recipient")
public class MailNotifySceneRecipient {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("recipient_id")
    private Long recipientId;

    @TableField("recipient_type")
    private String recipientType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
