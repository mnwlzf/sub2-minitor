package com.sub2.monitor.scheduler.job;

import com.sub2.monitor.entity.TaskExecutionLog;
import com.sub2.monitor.scheduler.executor.DailyDataSummaryExecutor;
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
public class DailyDataSummaryJob implements Job {

    private final DailyDataSummaryExecutor dailyDataSummaryExecutor;
    private final TaskExecutionLogService taskExecutionLogService;

    public DailyDataSummaryJob(DailyDataSummaryExecutor dailyDataSummaryExecutor,
                               TaskExecutionLogService taskExecutionLogService) {
        this.dailyDataSummaryExecutor = dailyDataSummaryExecutor;
        this.taskExecutionLogService = taskExecutionLogService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        OffsetDateTime fireTime = OffsetDateTime.now();
        String taskKey = context.getMergedJobDataMap().getString("taskKey");
        String taskName = context.getMergedJobDataMap().getString("taskName");
        String cronExpression = context.getMergedJobDataMap().getString("cronExpression");
        String triggerType = context.getTrigger() == null ? "AUTO" : context.getTrigger().getClass().getSimpleName();

        try {
            DailyDataSummaryExecutor.SummaryResult result = dailyDataSummaryExecutor.summarizeYesterday();
            String message = "每日数据汇总完成, summaryDate=" + result.summaryDate()
                    + ", rowCount=" + result.rowCount()
                    + ", totalPlatformConsume=" + result.totalPlatformConsume()
                    + ", totalActualConsume=" + result.totalActualConsume()
                    + ", detail=" + result.message();
            saveExecutionLog(taskKey, taskName, cronExpression, triggerType, "SUCCESS", message, fireTime, OffsetDateTime.now());
            log.info("daily data summary job executed, taskKey={}, taskName={}", taskKey, taskName);
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
