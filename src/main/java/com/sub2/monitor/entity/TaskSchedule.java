package com.sub2.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sub2.monitor.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_schedule")
public class TaskSchedule extends BaseEntity {
    private String taskKey;
    private String taskName;
    private String taskGroup;
    private String cronExpression;
    private String jobClass;
    private String description;
    private Boolean isEnabled;
    private OffsetDateTime updateTime;
}
