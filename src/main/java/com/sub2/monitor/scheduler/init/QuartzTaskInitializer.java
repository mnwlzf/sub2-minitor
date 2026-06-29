package com.sub2.monitor.scheduler.init;

import com.sub2.monitor.scheduler.service.SchedulerTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuartzTaskInitializer implements ApplicationRunner {

    private final SchedulerTaskService schedulerTaskService;

    public QuartzTaskInitializer(SchedulerTaskService schedulerTaskService) {
        this.schedulerTaskService = schedulerTaskService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            schedulerTaskService.syncAllEnabledTasks();
        } catch (Exception e) {
            log.warn("Quartz 任务初始化跳过，通常是 scheduler_task 表尚未创建，原因={}", e.getMessage());
        }
    }
}
