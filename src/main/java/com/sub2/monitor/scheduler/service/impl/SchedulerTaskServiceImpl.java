package com.sub2.monitor.scheduler.service.impl;

import com.sub2.monitor.collect.newApi.service.NewApiCollectService;
import com.sub2.monitor.collect.sub2api.service.Sub2CollectService;
import com.sub2.monitor.scheduler.entity.SchedulerTask;
import com.sub2.monitor.scheduler.enums.SchedulerTaskType;
import com.sub2.monitor.scheduler.job.CollectJob;
import com.sub2.monitor.scheduler.mapper.SchedulerTaskMapper;
import com.sub2.monitor.scheduler.service.SchedulerTaskService;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SchedulerTaskServiceImpl implements SchedulerTaskService {

    private static final String DEFAULT_TASK_GROUP = "monitor";

    private final Scheduler scheduler;
    private final SchedulerTaskMapper schedulerTaskMapper;
    private final Sub2CollectService sub2CollectService;
    private final NewApiCollectService newApiCollectService;

    public SchedulerTaskServiceImpl(
            Scheduler scheduler,
            SchedulerTaskMapper schedulerTaskMapper,
            Sub2CollectService sub2CollectService,
            NewApiCollectService newApiCollectService
    ) {
        this.scheduler = scheduler;
        this.schedulerTaskMapper = schedulerTaskMapper;
        this.sub2CollectService = sub2CollectService;
        this.newApiCollectService = newApiCollectService;
    }

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
            scheduleTask(task);
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
        deleteQuartzTask(existingTask.getId(), existingTask.getTaskGroup());
        if (isEnabled(existingTask)) {
            scheduleTask(existingTask);
        }
        return schedulerTaskMapper.selectById(id);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        deleteQuartzTask(task.getId(), task.getTaskGroup());
        schedulerTaskMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void pauseTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        task.setEnabled(0);
        task.setUpdatedAt(LocalDateTime.now());
        schedulerTaskMapper.updateById(task);
        deleteQuartzTask(task.getId(), task.getTaskGroup());
    }

    @Override
    @Transactional
    public void resumeTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        task.setEnabled(1);
        task.setUpdatedAt(LocalDateTime.now());
        validateTask(task);
        schedulerTaskMapper.updateById(task);
        scheduleTask(task);
    }

    @Override
    public void triggerTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        ensureScheduled(task);
        try {
            scheduler.triggerJob(buildJobKey(task.getId(), task.getTaskGroup()));
        } catch (SchedulerException e) {
            throw new IllegalStateException("立即执行任务失败", e);
        }
    }

    @Override
    public void syncAllEnabledTasks() {
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    scheduler.deleteJob(jobKey);
                }
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("清理 Quartz 任务失败", e);
        }

        for (SchedulerTask task : schedulerTaskMapper.selectEnabledTasks()) {
            scheduleTask(task);
        }
    }

    @Override
    public void executeTask(Long id) {
        SchedulerTask task = getTaskOrThrow(id);
        SchedulerTaskType taskType = SchedulerTaskType.valueOf(task.getTaskType());
        log.info("开始执行调度任务，taskId={}，taskType={}，baseUrl={}", task.getId(), taskType, task.getBaseUrl());
        switch (taskType) {
            case SUB2_LOGIN -> sub2CollectService.login(task.getBaseUrl());
            case SUB2_GROUPS -> sub2CollectService.collectSub2AvailableGroups(task.getBaseUrl());
            case SUB2_KEYS -> sub2CollectService.collectSub2Keys(task.getBaseUrl());
            case NEWAPI_LOGIN -> newApiCollectService.login(task.getBaseUrl());
            case NEWAPI_GROUPS -> newApiCollectService.collectGroups(task.getBaseUrl());
            case NEWAPI_TOKENS -> newApiCollectService.collectNewApiKeys(task.getBaseUrl());
            default -> throw new IllegalArgumentException("不支持的任务类型: " + task.getTaskType());
        }
    }

    private void ensureScheduled(SchedulerTask task) {
        if (!isEnabled(task)) {
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

    private void scheduleTask(SchedulerTask task) {
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

    private void deleteQuartzTask(Long taskId, String taskGroup) {
        try {
            scheduler.deleteJob(buildJobKey(taskId, taskGroup));
        } catch (SchedulerException e) {
            throw new IllegalStateException("删除 Quartz 任务失败", e);
        }
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
        if (!StringUtils.hasText(task.getBaseUrl())) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        if (!StringUtils.hasText(task.getCron())) {
            throw new IllegalArgumentException("cron 不能为空");
        }
        SchedulerTaskType.valueOf(task.getTaskType());
        CronScheduleBuilder.cronSchedule(task.getCron());
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
