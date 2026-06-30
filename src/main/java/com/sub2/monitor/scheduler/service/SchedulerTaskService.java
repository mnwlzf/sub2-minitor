package com.sub2.monitor.scheduler.service;

import com.sub2.monitor.scheduler.entity.SchedulerTask;

import java.util.List;

public interface SchedulerTaskService {

    List<SchedulerTask> listTasks();

    SchedulerTask createTask(SchedulerTask task);

    SchedulerTask updateTask(Long id, SchedulerTask task);

    void deleteTask(Long id);

    void pauseTask(Long id);

    void resumeTask(Long id);

    void triggerTask(Long id);

    void syncAllEnabledTasks();
}
