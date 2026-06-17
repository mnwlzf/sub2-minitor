package com.sub2.monitor.scheduler;

import org.quartz.SchedulerException;

public interface QuartzJobService {
    void schedule(TaskDefinition taskDefinition);

    void scheduleBalanceCollection(String cronExpression);

    void scheduleDailyDataSummary(String cronExpression);

    void triggerNow(String taskKey, String taskGroup);

    void pause(String taskKey, String taskGroup);

    void resume(String taskKey, String taskGroup);

    void remove(String taskKey, String taskGroup);

    boolean exists(String taskKey, String taskGroup) throws SchedulerException;
}
