package com.sub2.monitor.scheduler.job;

import com.sub2.monitor.scheduler.service.SchedulerTaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@DisallowConcurrentExecution
public class CollectJob implements Job {

    public static final String TASK_ID = "taskId";

    @Autowired
    private SchedulerTaskService schedulerTaskService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long taskId = jobDataMap.getLong(TASK_ID);
        try {
            schedulerTaskService.executeTask(taskId);
        } catch (Exception e) {
            log.error("Quartz 执行任务失败，taskId={}", taskId, e);
            throw new JobExecutionException(e);
        }
    }
}
