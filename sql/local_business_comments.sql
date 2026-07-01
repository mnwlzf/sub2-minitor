SET NAMES utf8mb4;

ALTER TABLE `accounts`
  COMMENT = '账号表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `username` varchar(50) DEFAULT NULL COMMENT '账号用户名',
  MODIFY COLUMN `email` varchar(50) DEFAULT NULL COMMENT '登录邮箱',
  MODIFY COLUMN `password` varchar(50) NOT NULL COMMENT '登录密码',
  MODIFY COLUMN `platform_id` bigint NOT NULL COMMENT '账号所属平台ID',
  MODIFY COLUMN `platform_name` varchar(50) DEFAULT NULL COMMENT '账号所属平台名称',
  MODIFY COLUMN `test_model` varchar(50) DEFAULT NULL COMMENT '账号测试模型',
  MODIFY COLUMN `is_collect` tinyint(1) DEFAULT '1' COMMENT '是否参与采集：1是，0否';

ALTER TABLE `platform`
  COMMENT = '平台表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `plaform_name` varchar(50) DEFAULT NULL COMMENT '平台名称',
  MODIFY COLUMN `base_url` varchar(100) NOT NULL COMMENT '平台基础地址',
  MODIFY COLUMN `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `type` varchar(10) NOT NULL COMMENT '平台类型：NEWAPI或SUB2API';

ALTER TABLE `scheduler_task`
  COMMENT = '定时任务业务配置表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `task_name` varchar(100) NOT NULL COMMENT '任务名称，同一任务分组内唯一',
  MODIFY COLUMN `task_group` varchar(64) NOT NULL DEFAULT 'monitor' COMMENT 'Quartz任务分组',
  MODIFY COLUMN `task_type` varchar(64) NOT NULL COMMENT '任务类型，对应SchedulerTaskType枚举',
  MODIFY COLUMN `base_url` varchar(255) NOT NULL DEFAULT '' COMMENT '采集平台基础地址，数据采集任务可为空',
  MODIFY COLUMN `cron` varchar(64) NOT NULL COMMENT 'Quartz Cron表达式',
  MODIFY COLUMN `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `notify_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用任务通知：1启用，0停用',
  MODIFY COLUMN `notify_scene_id` bigint DEFAULT NULL COMMENT '任务执行后关联的通知场景ID',
  MODIFY COLUMN `notify_trigger` varchar(20) NOT NULL DEFAULT 'FAILURE' COMMENT '通知触发时机：SUCCESS成功，FAILURE失败，ALWAYS总是',
  MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

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
  `last_collected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近采集时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collect_group_platform_name` (`platform_id`, `group_name`),
  KEY `idx_collect_group_platform` (`platform_id`),
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

ALTER TABLE `mail_smtp_config`
  COMMENT = 'SMTP配置表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `config_name` varchar(100) NOT NULL DEFAULT 'default' COMMENT '配置名称',
  MODIFY COLUMN `host` varchar(255) NOT NULL COMMENT 'SMTP主机',
  MODIFY COLUMN `port` int NOT NULL COMMENT 'SMTP端口',
  MODIFY COLUMN `username` varchar(255) NOT NULL COMMENT 'SMTP用户名',
  MODIFY COLUMN `password` varchar(512) DEFAULT NULL COMMENT 'SMTP密码，后续建议加密存储',
  MODIFY COLUMN `from_email` varchar(255) NOT NULL COMMENT '发件人邮箱',
  MODIFY COLUMN `from_name` varchar(100) DEFAULT NULL COMMENT '发件人名称',
  MODIFY COLUMN `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `use_tls` tinyint NOT NULL DEFAULT 0 COMMENT '是否使用STARTTLS：1是，0否',
  MODIFY COLUMN `use_ssl` tinyint NOT NULL DEFAULT 1 COMMENT '是否使用SSL：1是，0否',
  MODIFY COLUMN `is_default` tinyint NOT NULL DEFAULT 1 COMMENT '是否默认配置：1是，0否',
  MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `mail_recipient`
  COMMENT = '邮件收件人表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `email` varchar(255) NOT NULL COMMENT '收件人邮箱',
  MODIFY COLUMN `recipient_name` varchar(100) DEFAULT NULL COMMENT '收件人名称',
  MODIFY COLUMN `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `mail_notify_scene`
  COMMENT = '邮件通知场景表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `scene_code` varchar(64) NOT NULL COMMENT '通知场景编码',
  MODIFY COLUMN `scene_name` varchar(100) NOT NULL COMMENT '通知场景名称',
  MODIFY COLUMN `description` varchar(255) DEFAULT NULL COMMENT '通知场景描述',
  MODIFY COLUMN `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `smtp_config_id` bigint DEFAULT NULL COMMENT '关联SMTP配置ID',
  MODIFY COLUMN `subject_template` varchar(255) DEFAULT NULL COMMENT '邮件标题模板',
  MODIFY COLUMN `content_template` text DEFAULT NULL COMMENT '邮件正文模板',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `mail_notify_scene_recipient`
  COMMENT = '通知场景收件人关系表',
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `scene_id` bigint NOT NULL COMMENT '通知场景ID',
  MODIFY COLUMN `recipient_id` bigint NOT NULL COMMENT '收件人ID',
  MODIFY COLUMN `recipient_type` varchar(10) NOT NULL DEFAULT 'TO' COMMENT '收件人类型：TO主送，CC抄送，BCC密送',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
