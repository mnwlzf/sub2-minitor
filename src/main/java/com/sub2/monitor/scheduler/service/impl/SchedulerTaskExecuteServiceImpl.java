package com.sub2.monitor.scheduler.service.impl;

import com.sub2.monitor.collect.service.PlatformCollectBizService;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.enums.SchedulerTaskType;
import com.sub2.monitor.scheduler.mapper.SchedulerTaskMapper;
import com.sub2.monitor.scheduler.service.SchedulerTaskExecuteService;
import com.sub2.monitor.scheduler.service.SchedulerTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerTaskExecuteServiceImpl implements SchedulerTaskExecuteService {

    private final SchedulerTaskMapper schedulerTaskMapper;
    private final PlatformCollectBizService platformCollectBizService;
    private final SchedulerTaskLogService schedulerTaskLogService;

    @Override
    public void executeTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        SchedulerTaskType taskType = SchedulerTaskType.valueOf(task.getTaskType());
        log.info("开始执行调度任务，taskId={}，taskType={}，baseUrl={}", task.getId(), taskType, task.getBaseUrl());
        LocalDateTime startedAt = LocalDateTime.now();
        boolean success = false;
        String message = "执行完成";
        switch (taskType) {
            case DATA_COLLECT -> {
                try {
                    executeDataCollect(task);
                    success = true;
                } catch (Exception e) {
                    message = e.getMessage() == null ? "执行失败" : e.getMessage();
                    throw e;
                } finally {
                    try {
                        schedulerTaskLogService.saveExecutionLog(task, startedAt, LocalDateTime.now(), success, message);
                    } catch (Exception logException) {
                        log.warn("记录调度任务日志失败，taskId={}", task.getId(), logException);
                    }
                }
            }
        }
    }

    private void executeDataCollect(SchedulerTask task) {
        log.info("开始执行数据采集任务，taskId={}", task.getId());
        platformCollectBizService.collectEnabledPlatforms();
    }

    private SchedulerTask getTaskOrThrow(Long id) {
        SchedulerTask task = schedulerTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        return task;
    }
}
