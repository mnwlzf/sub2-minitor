package com.sub2.monitor.scheduler.service.impl;

import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.job.CollectJob;
import com.sub2.monitor.scheduler.service.SchedulerQuartzService;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerQuartzServiceImpl implements SchedulerQuartzService {

    private final Scheduler scheduler;

    @Override
    public void scheduleTask(SchedulerTask task) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(CollectJob.class)
                    .withIdentity(buildJobKey(task.getId(), task.getTaskGroup()))
                    .usingJobData(buildJobDataMap(task.getId()))
                    .build();

            CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(buildTriggerKey(task.getId(), task.getTaskGroup()))
                    .forJob(jobDetail)
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCron()))
                    .build();

            if (scheduler.checkExists(jobDetail.getKey())) {
                scheduler.deleteJob(jobDetail.getKey());
            }
            scheduler.scheduleJob(jobDetail, cronTrigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("调度任务失败", e);
        }
    }

    @Override
    public void deleteTask(Long taskId, String taskGroup) {
        try {
            scheduler.deleteJob(buildJobKey(taskId, taskGroup));
        } catch (SchedulerException e) {
            throw new IllegalStateException("删除 Quartz 任务失败", e);
        }
    }

    @Override
    public void triggerTask(SchedulerTask task) {
        ensureScheduled(task);
        try {
            scheduler.triggerJob(buildJobKey(task.getId(), task.getTaskGroup()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("立即执行任务失败", e);
        }
    }

    @Override
    public void syncTasks(List<SchedulerTask> tasks) {
        clearAllJobs();
        tasks.forEach(this::scheduleTask);
    }

    @Override
    public void validateCron(String cron) {
        CronScheduleBuilder.cronSchedule(cron);
    }

    private void ensureScheduled(SchedulerTask task) {
        if (task.getEnabled() == null || task.getEnabled() != 1) {
            throw new IllegalStateException("任务已禁用，不能立即执行");
        }
        try {
            JobKey jobKey = buildJobKey(task.getId(), task.getTaskGroup());
            if (!scheduler.checkExists(jobKey)) {
                scheduleTask(task);
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("检查 Quartz 任务失败", e);
        }
    }

    private void clearAllJobs() {
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("清理 Quartz 任务失败", e);
        }
    }

    private JobDataMap buildJobDataMap(Long taskId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CollectJob.TASK_ID, taskId);
        return jobDataMap;
    }

    private JobKey buildJobKey(Long taskId, String taskGroup) {
        return JobKey.jobKey("task:" + taskId, taskGroup);
    }

    private TriggerKey buildTriggerKey(Long taskId, String taskGroup) {
        return TriggerKey.triggerKey("trigger:" + taskId, taskGroup);
    }
}
