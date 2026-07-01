package com.sub2.monitor.mail.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mail_notify_scene")
public class MailNotifyScene {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("scene_name")
    private String sceneName;

    private String description;

    private Integer enabled;

    @TableField("smtp_config_id")
    private Long smtpConfigId;

    @TableField("subject_template")
    private String subjectTemplate;

    @TableField("content_template")
    private String contentTemplate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
