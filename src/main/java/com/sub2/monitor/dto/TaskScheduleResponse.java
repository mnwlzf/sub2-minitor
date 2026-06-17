package com.sub2.monitor.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskScheduleResponse {
    private Long id;
    private String taskKey;
    private String taskName;
    private String taskGroup;
    private String cronExpression;
    private String jobClass;
    private String description;
    private Boolean isEnabled;
    private String notificationSceneKey;
    private OffsetDateTime createTime;
    private OffsetDateTime updateTime;
}
