package com.sub2.monitor.scheduler.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTaskLogInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS `scheduler_task_log` (
                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                      `task_id` bigint NOT NULL COMMENT '任务ID',
                      `task_name` varchar(100) NOT NULL COMMENT '任务名称',
                      `task_group` varchar(64) NOT NULL COMMENT '任务分组',
                      `task_type` varchar(64) NOT NULL COMMENT '任务类型',
                      `base_url` varchar(255) NOT NULL DEFAULT '' COMMENT '目标地址',
                      `success` tinyint NOT NULL DEFAULT 0 COMMENT '是否成功：1成功，0失败',
                      `message` varchar(500) DEFAULT NULL COMMENT '执行结果摘要',
                      `started_at` datetime NOT NULL COMMENT '开始时间',
                      `finished_at` datetime NOT NULL COMMENT '结束时间',
                      `duration_ms` bigint NOT NULL DEFAULT 0 COMMENT '执行耗时毫秒',
                      PRIMARY KEY (`id`),
                      KEY `idx_scheduler_task_log_task` (`task_id`),
                      KEY `idx_scheduler_task_log_started_at` (`started_at`),
                      KEY `idx_scheduler_task_log_success` (`success`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表'
                    """);
        } catch (Exception e) {
            log.warn("调度任务日志表初始化跳过，原因={}", e.getMessage());
        }
    }
}
