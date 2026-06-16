package com.sub2.monitor.scheduler.impl;

import com.sub2.monitor.scheduler.QuartzJobNames;
import com.sub2.monitor.scheduler.QuartzJobService;
import com.sub2.monitor.scheduler.TaskDefinition;
import com.sub2.monitor.entity.TaskSchedule;
import com.sub2.monitor.service.TaskScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class QuartzJobServiceImpl implements QuartzJobService {

    private final Scheduler scheduler;
    private final TaskScheduleService taskScheduleService;

    public QuartzJobServiceImpl(Scheduler scheduler, TaskScheduleService taskScheduleService) {
        this.scheduler = scheduler;
        this.taskScheduleService = taskScheduleService;
    }

    @Override
    public void schedule(TaskDefinition taskDefinition) {
        try {
            JobKey jobKey = JobKey.jobKey(taskDefinition.getTaskKey(), taskDefinition.getTaskGroup());
            TriggerKey triggerKey = TriggerKey.triggerKey(taskDefinition.getTaskKey(), taskDefinition.getTaskGroup());
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("taskKey", taskDefinition.getTaskKey());
            jobDataMap.put("taskName", taskDefinition.getTaskName());
            jobDataMap.put("taskGroup", taskDefinition.getTaskGroup());
            jobDataMap.put("cronExpression", taskDefinition.getCronExpression());
            jobDataMap.put("description", taskDefinition.getDescription());

            Class<?> jobClass = Class.forName(taskDefinition.getJobClassName());
            JobDetail jobDetail = JobBuilder.newJob(jobClass.asSubclass(org.quartz.Job.class))
                    .withIdentity(jobKey)
                    .storeDurably()
                    .usingJobData(jobDataMap)
                    .build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(taskDefinition.getCronExpression()))
                    .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.addJob(jobDetail, true, true);
                if (scheduler.checkExists(triggerKey)) {
                    scheduler.rescheduleJob(triggerKey, trigger);
                } else {
                    scheduler.scheduleJob(trigger);
                }
                return;
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception exception) {
            throw new IllegalStateException("schedule task failed", exception);
        }
    }

    @Override
    public void scheduleBalanceCollection(String cronExpression) {
        TaskDefinition taskDefinition = TaskDefinition.builder()
                .taskKey(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY)
                .taskName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_NAME)
                .taskGroup(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP)
                .jobClassName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_JOB_CLASS)
                .cronExpression(cronExpression)
                .description("定时采集余额与渠道")
                .build();
        schedule(taskDefinition);
    }

    @Override
    public void triggerNow(String taskKey, String taskGroup) {
        try {
            ensureJobExists(taskKey, taskGroup);
            scheduler.triggerJob(JobKey.jobKey(taskKey, taskGroup));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("trigger task failed", exception);
        }
    }

    @Override
    public void pause(String taskKey, String taskGroup) {
        try {
            scheduler.pauseJob(JobKey.jobKey(taskKey, taskGroup));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("pause task failed", exception);
        }
    }

    @Override
    public void resume(String taskKey, String taskGroup) {
        try {
            scheduler.resumeJob(JobKey.jobKey(taskKey, taskGroup));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("resume task failed", exception);
        }
    }

    @Override
    public void remove(String taskKey, String taskGroup) {
        try {
            scheduler.deleteJob(JobKey.jobKey(taskKey, taskGroup));
        } catch (SchedulerException exception) {
            throw new IllegalStateException("remove task failed", exception);
        }
    }

    @Override
    public boolean exists(String taskKey, String taskGroup) throws SchedulerException {
        return scheduler.checkExists(JobKey.jobKey(taskKey, taskGroup));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncEnabledTasksOnStartup() {
        try {
            List<TaskSchedule> schedules = taskScheduleService.list(new LambdaQueryWrapper<TaskSchedule>()
                    .eq(TaskSchedule::getIsEnabled, true));
            for (TaskSchedule schedule : schedules) {
                schedule(TaskDefinition.builder()
                        .taskKey(schedule.getTaskKey())
                        .taskName(schedule.getTaskName())
                        .taskGroup(schedule.getTaskGroup())
                        .jobClassName(schedule.getJobClass())
                        .cronExpression(schedule.getCronExpression())
                        .description(schedule.getDescription())
                        .build());
            }
        } catch (Exception exception) {
            log.error("sync scheduled tasks on startup failed", exception);
        }
    }

    private void ensureJobExists(String taskKey, String taskGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(taskKey, taskGroup);
        if (scheduler.checkExists(jobKey)) {
            return;
        }

        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, taskKey)
                .eq(TaskSchedule::getTaskGroup, taskGroup));
        if (schedule == null) {
            return;
        }

        schedule(TaskDefinition.builder()
                .taskKey(schedule.getTaskKey())
                .taskName(schedule.getTaskName())
                .taskGroup(schedule.getTaskGroup())
                .jobClassName(schedule.getJobClass())
                .cronExpression(schedule.getCronExpression())
                .description(schedule.getDescription())
                .build());
    }
}
