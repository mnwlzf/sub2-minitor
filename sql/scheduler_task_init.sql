SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `scheduler_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称，同一任务分组内唯一',
  `task_group` varchar(64) NOT NULL DEFAULT 'monitor' COMMENT 'Quartz任务分组',
  `task_type` varchar(64) NOT NULL COMMENT '任务类型，对应SchedulerTaskType枚举',
  `base_url` varchar(255) NOT NULL DEFAULT '' COMMENT '采集平台基础地址，数据采集任务可为空',
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

CREATE TABLE IF NOT EXISTS `collect_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` bigint NOT NULL COMMENT '平台ID',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型：SUB2API、NEWAPI',
  `base_url` varchar(255) NOT NULL COMMENT '平台基础地址',
  `collect_type` varchar(32) NOT NULL COMMENT '采集类型：GROUPS、KEYS、TOKENS',
  `success` tinyint NOT NULL DEFAULT 0 COMMENT '是否成功：1成功，0失败',
  `item_count` int NOT NULL DEFAULT 0 COMMENT '本次响应条目数',
  `message` varchar(500) DEFAULT NULL COMMENT '响应消息',
  `payload_json` json DEFAULT NULL COMMENT '采集响应JSON',
  `collected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collect_snapshot_platform_type` (`platform_id`, `collect_type`),
  KEY `idx_collect_snapshot_platform` (`platform_id`),
  KEY `idx_collect_snapshot_type` (`platform_type`, `collect_type`),
  KEY `idx_collect_snapshot_collected_at` (`collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集结果快照表';

CREATE TABLE IF NOT EXISTS `collect_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` bigint NOT NULL COMMENT '平台ID',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型：SUB2API、NEWAPI',
  `base_url` varchar(255) NOT NULL COMMENT '平台基础地址',
  `group_name` varchar(128) NOT NULL COMMENT '分组名称',
  `description` varchar(500) DEFAULT NULL COMMENT '分组描述',
  `rate_multiplier` decimal(18,8) DEFAULT NULL COMMENT '倍率：Sub2 rate_multiplier，NewApi ratio',
  `status` varchar(32) DEFAULT NULL COMMENT '分组状态',
  `raw_json` json DEFAULT NULL COMMENT '分组原始JSON',
  `key_count` int NOT NULL DEFAULT 0 COMMENT '使用该分组的密钥数量',
  `used_by_key` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否被密钥使用：1是，0否',
  `last_collected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近采集时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collect_group_platform_name` (`platform_id`, `group_name`),
  KEY `idx_collect_group_platform` (`platform_id`),
  KEY `idx_collect_group_used_by_key` (`used_by_key`),
  KEY `idx_collect_group_type` (`platform_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集分组当前状态表';

CREATE TABLE IF NOT EXISTS `account_balance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id` bigint DEFAULT NULL COMMENT '账号ID',
  `platform_id` bigint NOT NULL COMMENT '平台ID',
  `platform_type` varchar(32) NOT NULL COMMENT '平台类型：SUB2API、NEWAPI',
  `base_url` varchar(255) NOT NULL COMMENT '平台基础地址',
  `account_identity` varchar(255) DEFAULT NULL COMMENT '账号标识：邮箱或用户名',
  `balance` decimal(20,8) NOT NULL COMMENT '采集时余额',
  `total_consumption` decimal(20,8) DEFAULT NULL COMMENT '平台返回的历史累计消耗',
  `consumption_amount` decimal(20,8) NOT NULL DEFAULT 0 COMMENT '本次累计消耗增量',
  `recharge_amount` decimal(20,8) NOT NULL DEFAULT 0 COMMENT '本次余额增加量，计入充值/到账',
  `collected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_balance_record_account` (`account_id`),
  KEY `idx_balance_record_platform` (`platform_id`),
  KEY `idx_balance_record_collected_at` (`collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号余额采集记录表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

DELETE FROM `scheduler_task`
WHERE `task_type` IN (
  'SUB2_LOGIN',
  'SUB2_GROUPS',
  'SUB2_KEYS',
  'NEWAPI_LOGIN',
  'NEWAPI_GROUPS',
  'NEWAPI_TOKENS'
);

INSERT INTO `scheduler_task` (`task_name`, `task_group`, `task_type`, `base_url`, `cron`, `enabled`, `notify_enabled`, `notify_scene_id`, `notify_trigger`, `remark`)
VALUES
('data-collect', 'monitor', 'DATA_COLLECT', '', '0 */30 * * * ?', 0, 0, NULL, 'FAILURE', '数据采集')
ON DUPLICATE KEY UPDATE
  `task_type` = VALUES(`task_type`),
  `base_url` = VALUES(`base_url`),
  `cron` = VALUES(`cron`),
  `notify_enabled` = VALUES(`notify_enabled`),
  `notify_scene_id` = VALUES(`notify_scene_id`),
  `notify_trigger` = VALUES(`notify_trigger`),
  `remark` = VALUES(`remark`),
  `updated_at` = CURRENT_TIMESTAMP;
