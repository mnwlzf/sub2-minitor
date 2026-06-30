package com.sub2.monitor.scheduler.service.impl;

import com.sub2.monitor.collect.service.PlatformCollectBizService;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.enums.SchedulerTaskType;
import com.sub2.monitor.scheduler.mapper.SchedulerTaskMapper;
import com.sub2.monitor.scheduler.service.SchedulerTaskExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerTaskExecuteServiceImpl implements SchedulerTaskExecuteService {

    private final SchedulerTaskMapper schedulerTaskMapper;
    private final PlatformCollectBizService platformCollectBizService;

    @Override
    public void executeTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        SchedulerTaskType taskType = SchedulerTaskType.valueOf(task.getTaskType());
        log.info("开始执行调度任务，taskId={}，taskType={}，baseUrl={}", task.getId(), taskType, task.getBaseUrl());
        switch (taskType) {
            case DATA_COLLECT -> executeDataCollect(task);
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
