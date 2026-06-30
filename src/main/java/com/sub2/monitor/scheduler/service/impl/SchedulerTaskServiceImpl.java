package com.sub2.monitor.scheduler.service.impl;

import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.enums.SchedulerTaskType;
import com.sub2.monitor.scheduler.mapper.SchedulerTaskMapper;
import com.sub2.monitor.scheduler.service.SchedulerQuartzService;
import com.sub2.monitor.scheduler.service.SchedulerTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerTaskServiceImpl implements SchedulerTaskService {

    private static final String DEFAULT_TASK_GROUP = "monitor";

    private final SchedulerTaskMapper schedulerTaskMapper;
    private final SchedulerQuartzService schedulerQuartzService;

    @Override
    public List<SchedulerTask> listTasks() {
        return schedulerTaskMapper.selectAllTasks();
    }

    @Override
    @Transactional
    public SchedulerTask createTask(SchedulerTask task) {
        normalizeTask(task);
        validateTask(task);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        schedulerTaskMapper.insert(task);
        if (isEnabled(task)) {
            schedulerQuartzService.scheduleTask(task);
        }
        return schedulerTaskMapper.selectById(task.getId());
    }

    @Override
    @Transactional
    public SchedulerTask updateTask(Long id, SchedulerTask task) {
        SchedulerTask existingTask = getTaskOrThrow(id);
        existingTask.setTaskName(task.getTaskName());
        existingTask.setTaskGroup(task.getTaskGroup());
        existingTask.setTaskType(task.getTaskType());
        existingTask.setBaseUrl(task.getBaseUrl());
        existingTask.setCron(task.getCron());
        existingTask.setEnabled(task.getEnabled());
        existingTask.setNotifyEnabled(task.getNotifyEnabled());
        existingTask.setNotifySceneId(task.getNotifySceneId());
        existingTask.setNotifyTrigger(task.getNotifyTrigger());
        existingTask.setRemark(task.getRemark());
        normalizeTask(existingTask);
        validateTask(existingTask);
        existingTask.setUpdatedAt(LocalDateTime.now());
        schedulerTaskMapper.updateById(existingTask);
        schedulerQuartzService.deleteTask(existingTask.getId(), existingTask.getTaskGroup());
        if (isEnabled(existingTask)) {
            schedulerQuartzService.scheduleTask(existingTask);
        }
        return schedulerTaskMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        schedulerQuartzService.deleteTask(task.getId(), task.getTaskGroup());
        schedulerTaskMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void pauseTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        task.setEnabled(0);
        task.setUpdatedAt(LocalDateTime.now());
        schedulerTaskMapper.updateById(task);
        schedulerQuartzService.deleteTask(task.getId(), task.getTaskGroup());
    }

    @Override
    @Transactional
    public void resumeTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        task.setEnabled(1);
        task.setUpdatedAt(LocalDateTime.now());
        validateTask(task);
        schedulerTaskMapper.updateById(task);
        schedulerQuartzService.scheduleTask(task);
    }

    @Override
    public void triggerTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        schedulerQuartzService.triggerTask(task);
    }

    @Override
    public void syncAllEnabledTasks() {
        List<SchedulerTask> tasks = schedulerTaskMapper.selectEnabledTasks().stream()
                .filter(this::isSupportedTaskType)
                .toList();
        schedulerQuartzService.syncTasks(tasks);
    }

    private SchedulerTask getTaskOrThrow(Long id) {
        SchedulerTask task = schedulerTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        return task;
    }

    private void normalizeTask(SchedulerTask task) {
        if (!StringUtils.hasText(task.getTaskGroup())) {
            task.setTaskGroup(DEFAULT_TASK_GROUP);
        }
        if (!StringUtils.hasText(task.getTaskType())) {
            task.setTaskType(SchedulerTaskType.DATA_COLLECT.name());
        }
        if (task.getBaseUrl() == null) {
            task.setBaseUrl("");
        }
        if (task.getEnabled() == null) {
            task.setEnabled(0);
        }
        if (task.getNotifyEnabled() == null) {
            task.setNotifyEnabled(0);
        }
        if (!StringUtils.hasText(task.getNotifyTrigger())) {
            task.setNotifyTrigger("FAILURE");
        }
    }

    private void validateTask(SchedulerTask task) {
        if (!StringUtils.hasText(task.getTaskName())) {
            throw new IllegalArgumentException("taskName 不能为空");
        }
        if (!StringUtils.hasText(task.getTaskType())) {
            throw new IllegalArgumentException("taskType 不能为空");
        }
        if (!StringUtils.hasText(task.getCron())) {
            throw new IllegalArgumentException("cron 不能为空");
        }
        SchedulerTaskType.valueOf(task.getTaskType());
        schedulerQuartzService.validateCron(task.getCron());
        if (task.getNotifyEnabled() != null && task.getNotifyEnabled() == 1 && task.getNotifySceneId() == null) {
            throw new IllegalArgumentException("启用通知时 notifySceneId 不能为空");
        }
        if (!List.of("SUCCESS", "FAILURE", "ALWAYS").contains(task.getNotifyTrigger())) {
            throw new IllegalArgumentException("notifyTrigger 只能是 SUCCESS、FAILURE 或 ALWAYS");
        }
    }

    private boolean isEnabled(SchedulerTask task) {
        return task.getEnabled() != null && task.getEnabled() == 1;
    }

    private boolean isSupportedTaskType(SchedulerTask task) {
        try {
            SchedulerTaskType.valueOf(task.getTaskType());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
