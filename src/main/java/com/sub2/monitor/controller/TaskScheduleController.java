package com.sub2.monitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sub2.monitor.common.api.ApiResponse;
import com.sub2.monitor.common.api.PageResponse;
import com.sub2.monitor.dto.TaskScheduleRequest;
import com.sub2.monitor.entity.TaskExecutionLog;
import com.sub2.monitor.entity.TaskSchedule;
import com.sub2.monitor.scheduler.QuartzJobNames;
import com.sub2.monitor.scheduler.QuartzTaskFacade;
import com.sub2.monitor.scheduler.TaskDefinition;
import com.sub2.monitor.service.TaskExecutionLogService;
import com.sub2.monitor.service.TaskScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskScheduleController {

    private final TaskScheduleService taskScheduleService;
    private final TaskExecutionLogService taskExecutionLogService;
    private final QuartzTaskFacade quartzTaskFacade;

    public TaskScheduleController(TaskScheduleService taskScheduleService,
                                  TaskExecutionLogService taskExecutionLogService,
                                  QuartzTaskFacade quartzTaskFacade) {
        this.taskScheduleService = taskScheduleService;
        this.taskExecutionLogService = taskExecutionLogService;
        this.quartzTaskFacade = quartzTaskFacade;
    }

    @GetMapping
    public ApiResponse<PageResponse<TaskSchedule>> list(@RequestParam(defaultValue = "1") long pageNo,
                                                        @RequestParam(defaultValue = "20") long pageSize,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String taskGroup) {
        LambdaQueryWrapper<TaskSchedule> wrapper = new LambdaQueryWrapper<TaskSchedule>()
                .orderByDesc(TaskSchedule::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query.like(TaskSchedule::getTaskKey, keyword)
                    .or()
                    .like(TaskSchedule::getTaskName, keyword));
        }
        if (taskGroup != null && !taskGroup.isBlank()) {
            wrapper.eq(TaskSchedule::getTaskGroup, taskGroup);
        }
        Page<TaskSchedule> page = taskScheduleService.page(new Page<>(pageNo, pageSize), wrapper);
        PageResponse<TaskSchedule> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(page.getRecords());
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody TaskScheduleRequest request) {
        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, request.getTaskKey()));
        if (schedule == null) {
            schedule = new TaskSchedule();
            schedule.setTaskKey(request.getTaskKey());
            schedule.setTaskName(request.getTaskName());
            schedule.setTaskGroup(request.getTaskGroup());
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(request.getJobClass());
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(true));
            schedule.setCreateTime(OffsetDateTime.now());
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.save(schedule);
        } else {
            schedule.setTaskName(request.getTaskName());
            schedule.setTaskGroup(request.getTaskGroup());
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(request.getJobClass());
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(schedule.getIsEnabled()));
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.updateById(schedule);
        }

        if (Boolean.TRUE.equals(schedule.getIsEnabled())) {
            quartzTaskFacade.scheduleTask(TaskDefinition.builder()
                    .taskKey(schedule.getTaskKey())
                    .taskName(schedule.getTaskName())
                    .taskGroup(schedule.getTaskGroup())
                    .jobClassName(schedule.getJobClass())
                    .cronExpression(schedule.getCronExpression())
                    .description(schedule.getDescription())
                    .build());
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/balance-collection/init")
    public ApiResponse<Void> initBalanceCollection(@RequestBody TaskScheduleRequest request) {
        TaskDefinition taskDefinition = TaskDefinition.builder()
                .taskKey(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY)
                .taskName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_NAME)
                .taskGroup(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP)
                .jobClassName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_JOB_CLASS)
                .cronExpression(request.getCronExpression())
                .description(request.getDescription())
                .build();

        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY));
        if (schedule == null) {
            schedule = new TaskSchedule();
            schedule.setTaskKey(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY);
            schedule.setTaskName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_NAME);
            schedule.setTaskGroup(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(QuartzJobNames.BALANCE_CHANNEL_COLLECT_JOB_CLASS);
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(true);
            schedule.setCreateTime(OffsetDateTime.now());
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.save(schedule);
        } else {
            schedule.setTaskName(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_NAME);
            schedule.setTaskGroup(QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_GROUP);
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(QuartzJobNames.BALANCE_CHANNEL_COLLECT_JOB_CLASS);
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(true);
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.updateById(schedule);
        }

        quartzTaskFacade.startBalanceCollection(taskDefinition.getCronExpression());
        return ApiResponse.success(null);
    }

    @PostMapping("/daily-data-summary/init")
    public ApiResponse<Void> initDailyDataSummary(@RequestBody TaskScheduleRequest request) {
        TaskDefinition taskDefinition = TaskDefinition.builder()
                .taskKey(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_KEY)
                .taskName(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_NAME)
                .taskGroup(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_GROUP)
                .jobClassName(QuartzJobNames.DAILY_DATA_SUMMARY_JOB_CLASS)
                .cronExpression(request.getCronExpression())
                .description(request.getDescription())
                .build();

        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, QuartzJobNames.DAILY_DATA_SUMMARY_TASK_KEY));
        if (schedule == null) {
            schedule = new TaskSchedule();
            schedule.setTaskKey(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_KEY);
            schedule.setTaskName(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_NAME);
            schedule.setTaskGroup(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_GROUP);
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(QuartzJobNames.DAILY_DATA_SUMMARY_JOB_CLASS);
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(true);
            schedule.setCreateTime(OffsetDateTime.now());
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.save(schedule);
        } else {
            schedule.setTaskName(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_NAME);
            schedule.setTaskGroup(QuartzJobNames.DAILY_DATA_SUMMARY_TASK_GROUP);
            schedule.setCronExpression(request.getCronExpression());
            schedule.setJobClass(QuartzJobNames.DAILY_DATA_SUMMARY_JOB_CLASS);
            schedule.setDescription(request.getDescription());
            schedule.setIsEnabled(true);
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.updateById(schedule);
        }

        quartzTaskFacade.startDailyDataSummary(taskDefinition.getCronExpression());
        return ApiResponse.success(null);
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody TaskScheduleRequest request) {
        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, request.getTaskKey()));
        if (schedule == null) {
            return ApiResponse.failure(404, "task not found");
        }
        schedule.setTaskName(request.getTaskName());
        schedule.setTaskGroup(request.getTaskGroup());
        schedule.setCronExpression(request.getCronExpression());
        schedule.setJobClass(request.getJobClass());
        schedule.setDescription(request.getDescription());
        schedule.setIsEnabled(Optional.ofNullable(request.getIsEnabled()).orElse(schedule.getIsEnabled()));
        schedule.setUpdateTime(OffsetDateTime.now());
        taskScheduleService.updateById(schedule);
        quartzTaskFacade.scheduleTask(TaskDefinition.builder()
                .taskKey(schedule.getTaskKey())
                .taskName(schedule.getTaskName())
                .taskGroup(schedule.getTaskGroup())
                .jobClassName(schedule.getJobClass())
                .cronExpression(schedule.getCronExpression())
                .description(schedule.getDescription())
                .build());
        return ApiResponse.success(null);
    }

    @PostMapping("/run")
    public ApiResponse<Void> runTask(@RequestBody TaskScheduleRequest request) {
        quartzTaskFacade.runTaskNow(request.getTaskKey(), request.getTaskGroup());
        return ApiResponse.success(null);
    }

    @PostMapping("/pause")
    public ApiResponse<Void> pauseTask(@RequestBody TaskScheduleRequest request) {
        quartzTaskFacade.pauseTask(request.getTaskKey(), request.getTaskGroup());
        return ApiResponse.success(null);
    }

    @PostMapping("/resume")
    public ApiResponse<Void> resumeTask(@RequestBody TaskScheduleRequest request) {
        quartzTaskFacade.resumeTask(request.getTaskKey(), request.getTaskGroup());
        return ApiResponse.success(null);
    }

    @PostMapping("/remove")
    public ApiResponse<Void> removeTask(@RequestBody TaskScheduleRequest request) {
        quartzTaskFacade.removeTask(request.getTaskKey(), request.getTaskGroup());
        return ApiResponse.success(null);
    }

    @GetMapping("/preview")
    public ApiResponse<java.util.List<String>> preview(@RequestParam String cronExpression,
                                                       @RequestParam(defaultValue = "5") int count) {
        java.util.List<String> previews = new java.util.ArrayList<>();
        try {
            org.quartz.CronExpression expression = new org.quartz.CronExpression(cronExpression);
            java.util.Date next = new java.util.Date();
            for (int i = 0; i < count; i++) {
                next = expression.getNextValidTimeAfter(next);
                if (next == null) {
                    break;
                }
                previews.add(next.toInstant().toString());
            }
            return ApiResponse.success(previews);
        } catch (Exception exception) {
            return ApiResponse.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/balance-collection/cron")
    public ApiResponse<Void> updateBalanceCollectionCron(@RequestParam String cronExpression) {
        TaskSchedule schedule = taskScheduleService.getOne(new LambdaQueryWrapper<TaskSchedule>()
                .eq(TaskSchedule::getTaskKey, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY));
        if (schedule != null) {
            schedule.setCronExpression(cronExpression);
            schedule.setUpdateTime(OffsetDateTime.now());
            taskScheduleService.updateById(schedule);
        }
        quartzTaskFacade.startBalanceCollection(cronExpression);
        return ApiResponse.success(null);
    }

    @PostMapping("/balance-collection/run")
    public ApiResponse<Void> runBalanceCollectionNow() {
        quartzTaskFacade.runBalanceCollectionNow();
        return ApiResponse.success(null);
    }

    @GetMapping("/balance-collection/logs")
    public ApiResponse<PageResponse<TaskExecutionLog>> balanceCollectionLogs(@RequestParam(defaultValue = "1") long pageNo,
                                                                             @RequestParam(defaultValue = "20") long pageSize) {
        Page<TaskExecutionLog> page = taskExecutionLogService.page(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<TaskExecutionLog>()
                        .eq(TaskExecutionLog::getTaskKey, QuartzJobNames.BALANCE_CHANNEL_COLLECT_TASK_KEY)
                        .orderByDesc(TaskExecutionLog::getCreateTime));
        PageResponse<TaskExecutionLog> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(page.getRecords());
        return ApiResponse.success(response);
    }

    @GetMapping("/logs")
    public ApiResponse<PageResponse<TaskExecutionLog>> logs(@RequestParam(defaultValue = "1") long pageNo,
                                                            @RequestParam(defaultValue = "20") long pageSize,
                                                            @RequestParam(required = false) String taskKey) {
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<TaskExecutionLog>()
                .orderByDesc(TaskExecutionLog::getCreateTime);
        if (taskKey != null && !taskKey.isBlank()) {
            wrapper.eq(TaskExecutionLog::getTaskKey, taskKey);
        }
        Page<TaskExecutionLog> page = taskExecutionLogService.page(new Page<>(pageNo, pageSize), wrapper);
        PageResponse<TaskExecutionLog> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setRecords(page.getRecords());
        return ApiResponse.success(response);
    }
}
