package com.sub2.monitor.scheduler.service;

import com.sub2.monitor.scheduler.dto.SchedulerTaskLogQueryRequest;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.entity.SchedulerTaskLog;

import java.time.LocalDateTime;
import java.util.List;

public interface SchedulerTaskLogService {

    List<SchedulerTaskLog> listLogs(SchedulerTaskLogQueryRequest request);

    void saveExecutionLog(
            SchedulerTask task,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            boolean success,
            String message
    );
}
