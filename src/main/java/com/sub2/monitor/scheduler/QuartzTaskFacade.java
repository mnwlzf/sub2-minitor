package com.sub2.monitor.scheduler;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

@Service
public class QuartzTaskFacade {

    private final QuartzJobService quartzJobService;

    public QuartzTaskFacade(QuartzJobService quartzJobService) {
        this.quartzJobService = quartzJobService;
    }

    public void startBalanceCollection(String cronExpression) {
        quartzJobService.scheduleBalanceCollection(cronExpression);
    }

    public void startDailyDataSummary(String cronExpression) {
        quartzJobService.scheduleDailyDataSummary(cronExpression);
    }

    public void startRateChangeDetect(String cronExpression) {
        quartzJobService.scheduleRateChangeDetect(cronExpression);
    }

    public void scheduleTask(TaskDefinition taskDefinition) {
        quartzJobService.schedule(taskDefinition);
    }

    public void runBalanceCollectionNow() {
        quartzJobService.triggerNow(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
    }

    public void runRateChangeDetectNow() {
        quartzJobService.triggerNow(QuartzJobNames.RATE_CHANGE_DETECT_TASK_KEY, QuartzJobNames.RATE_CHANGE_DETECT_TASK_GROUP);
    }

    public void runTaskNow(String taskKey, String taskGroup) {
        quartzJobService.triggerNow(taskKey, taskGroup);
    }

    public void pauseBalanceCollection() {
        quartzJobService.pause(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
    }

    public void pauseTask(String taskKey, String taskGroup) {
        quartzJobService.pause(taskKey, taskGroup);
    }

    public void resumeBalanceCollection() {
        quartzJobService.resume(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
    }

    public void resumeTask(String taskKey, String taskGroup) {
        quartzJobService.resume(taskKey, taskGroup);
    }

    public void removeBalanceCollection() {
        quartzJobService.remove(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
    }

    public void removeTask(String taskKey, String taskGroup) {
        quartzJobService.remove(taskKey, taskGroup);
    }

    public boolean isBalanceCollectionActive() throws SchedulerException {
        return quartzJobService.exists(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
    }

    public boolean isTaskActive(String taskKey, String taskGroup) throws SchedulerException {
        return quartzJobService.exists(taskKey, taskGroup);
    }
}
