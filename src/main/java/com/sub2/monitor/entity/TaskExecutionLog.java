package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sub2.monitor.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_execution_log")
public class TaskExecutionLog extends BaseEntity {
    private String taskKey;
    private String taskName;
    private String cronExpression;
    private String triggerType;
    private String status;
    private String message;
    private OffsetDateTime fireTime;
    private OffsetDateTime finishTime;
}
