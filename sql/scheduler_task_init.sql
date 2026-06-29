SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `scheduler_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称，同一任务分组内唯一',
  `task_group` varchar(64) NOT NULL DEFAULT 'monitor' COMMENT 'Quartz任务分组',
  `task_type` varchar(64) NOT NULL COMMENT '任务类型，对应SchedulerTaskType枚举',
  `base_url` varchar(255) NOT NULL COMMENT '采集平台基础地址',
  `cron` varchar(64) NOT NULL COMMENT 'Quartz Cron表达式',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `notify_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用任务通知：1启用，0停用',
  `notify_scene_id` bigint DEFAULT NULL COMMENT '任务执行后关联的通知场景ID',
  `notify_trigger` varchar(20) NOT NULL DEFAULT 'FAILURE' COMMENT '通知触发时机：SUCCESS成功，FAILURE失败，ALWAYS总是',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheduler_task_name_group` (`task_name`, `task_group`),
  KEY `idx_scheduler_task_enabled` (`enabled`),
  KEY `idx_scheduler_task_type` (`task_type`),
  KEY `idx_scheduler_task_notify_scene` (`notify_scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务业务配置表';

INSERT INTO `scheduler_task` (`task_name`, `task_group`, `task_type`, `base_url`, `cron`, `enabled`, `notify_enabled`, `notify_scene_id`, `notify_trigger`, `remark`)
VALUES
('sub2-login', 'monitor', 'SUB2_LOGIN', 'https://codex.trovebox.online', '0 */30 * * * ?', 0, 0, NULL, 'FAILURE', 'Sub2 登录刷新'),
('sub2-groups', 'monitor', 'SUB2_GROUPS', 'https://codex.trovebox.online', '0 0 */1 * * ?', 0, 0, NULL, 'FAILURE', 'Sub2 分组采集'),
('sub2-keys', 'monitor', 'SUB2_KEYS', 'https://codex.trovebox.online', '0 0/20 * * * ?', 0, 0, NULL, 'FAILURE', 'Sub2 Key 采集'),
('newapi-login', 'monitor', 'NEWAPI_LOGIN', 'https://ggniao.com', '0 */30 * * * ?', 0, 0, NULL, 'FAILURE', 'NewApi 登录刷新'),
('newapi-groups', 'monitor', 'NEWAPI_GROUPS', 'https://ggniao.com', '0 0/15 * * * ?', 0, 0, NULL, 'FAILURE', 'NewApi 分组采集'),
('newapi-tokens', 'monitor', 'NEWAPI_TOKENS', 'https://ggniao.com', '0 0/20 * * * ?', 0, 0, NULL, 'FAILURE', 'NewApi Token 采集')
ON DUPLICATE KEY UPDATE
  `task_type` = VALUES(`task_type`),
  `base_url` = VALUES(`base_url`),
  `cron` = VALUES(`cron`),
  `notify_enabled` = VALUES(`notify_enabled`),
  `notify_scene_id` = VALUES(`notify_scene_id`),
  `notify_trigger` = VALUES(`notify_trigger`),
  `remark` = VALUES(`remark`),
  `updated_at` = CURRENT_TIMESTAMP;
