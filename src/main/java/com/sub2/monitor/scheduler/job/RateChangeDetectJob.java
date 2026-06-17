package com.sub2.monitor.scheduler.job;

import com.sub2.monitor.entity.TaskExecutionLog;
import com.sub2.monitor.scheduler.executor.RateChangeDetectExecutor;
import com.sub2.monitor.service.TaskNotificationSceneService;
import com.sub2.monitor.service.TaskExecutionLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@DisallowConcurrentExecution
public class RateChangeDetectJob implements Job {

    private final RateChangeDetectExecutor rateChangeDetectExecutor;
    private final TaskExecutionLogService taskExecutionLogService;
    private final TaskNotificationSceneService taskNotificationSceneService;

    public RateChangeDetectJob(RateChangeDetectExecutor rateChangeDetectExecutor,
                               TaskExecutionLogService taskExecutionLogService,
                               TaskNotificationSceneService taskNotificationSceneService) {
        this.rateChangeDetectExecutor = rateChangeDetectExecutor;
        this.taskExecutionLogService = taskExecutionLogService;
        this.taskNotificationSceneService = taskNotificationSceneService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        OffsetDateTime fireTime = OffsetDateTime.now();
        String taskKey = context.getMergedJobDataMap().getString("taskKey");
        String taskName = context.getMergedJobDataMap().getString("taskName");
        String cronExpression = context.getMergedJobDataMap().getString("cronExpression");
        String triggerType = context.getTrigger() == null ? "AUTO" : context.getTrigger().getClass().getSimpleName();

        try {
            String notificationSceneKey = taskNotificationSceneService.getEnabledSceneKey(taskKey);
            RateChangeDetectExecutor.DetectResult result = rateChangeDetectExecutor.detectAll(notificationSceneKey);
            String message = result.message()
                    + ", platformCount=" + result.platformCount()
                    + ", checkedPlatformCount=" + result.checkedPlatformCount()
                    + ", changedPlatformCount=" + result.changedPlatformCount();
            saveExecutionLog(taskKey, taskName, cronExpression, triggerType, "SUCCESS", message, fireTime, OffsetDateTime.now());
            log.info("rate change detect job executed, taskKey={}, taskName={}", taskKey, taskName);
        } catch (Exception exception) {
            saveExecutionLog(taskKey, taskName, cronExpression, triggerType, "FAILED", exception.getMessage(), fireTime, OffsetDateTime.now());
            throw new JobExecutionException(exception);
        }
    }

    private void saveExecutionLog(String taskKey,
                                  String taskName,
                                  String cronExpression,
                                  String triggerType,
                                  String status,
                                  String message,
                                  OffsetDateTime fireTime,
                                  OffsetDateTime finishTime) {
        TaskExecutionLog logEntry = new TaskExecutionLog();
        logEntry.setTaskKey(taskKey);
        logEntry.setTaskName(taskName);
        logEntry.setCronExpression(cronExpression);
        logEntry.setTriggerType(triggerType);
        logEntry.setStatus(status);
        logEntry.setMessage(message);
        logEntry.setFireTime(fireTime);
        logEntry.setFinishTime(finishTime);
        taskExecutionLogService.save(logEntry);
    }
}
