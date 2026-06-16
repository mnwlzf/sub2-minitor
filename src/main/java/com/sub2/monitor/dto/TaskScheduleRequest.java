package com.sub2.monitor.dto;

import lombok.Data;

@Data
public class TaskScheduleRequest {
    private String taskKey;
    private String taskName;
    private String taskGroup;
    private String cronExpression;
    private String jobClass;
    private String description;
    private Boolean isEnabled;
}
