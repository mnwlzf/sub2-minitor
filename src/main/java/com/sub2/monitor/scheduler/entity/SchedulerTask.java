package com.sub2.monitor.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scheduler_task")
public class SchedulerTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_name")
    private String taskName;

    @TableField("task_group")
    private String taskGroup;

    @TableField("task_type")
    private String taskType;

    @TableField("base_url")
    private String baseUrl;

    private String cron;

    private Integer enabled;

    @TableField("notify_enabled")
    private Integer notifyEnabled;

    @TableField("notify_scene_id")
    private Long notifySceneId;

    @TableField("notify_trigger")
    private String notifyTrigger;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
