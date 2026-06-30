package com.sub2.monitor.scheduler.service;

import com.sub2.monitor.scheduler.entity.SchedulerTask;

import java.util.List;

public interface SchedulerQuartzService {

    void scheduleTask(SchedulerTask task);

    void deleteTask(Long taskId, String taskGroup);

    void triggerTask(SchedulerTask task);

    void syncTasks(List<SchedulerTask> tasks);

    void validateCron(String cron);
}
