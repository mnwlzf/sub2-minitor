package com.sub2.monitor.scheduler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskDefinition {
    private String taskKey;
    private String taskName;
    private String taskGroup;
    private String jobClassName;
    private String cronExpression;
    private String description;
}
