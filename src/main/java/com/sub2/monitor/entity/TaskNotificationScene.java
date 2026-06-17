package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("task_notification_scene")
public class TaskNotificationScene {
    private Long id;
    private String taskKey;
    private String sceneKey;
    private Boolean isEnabled;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
