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
  MODIFY COLUMN `base_url` varchar(255) NOT NULL COMMENT '采集平台基础地址',
  MODIFY COLUMN `cron` varchar(64) NOT NULL COMMENT 'Quartz Cron表达式',
  MODIFY COLUMN `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  MODIFY COLUMN `notify_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用任务通知：1启用，0停用',
  MODIFY COLUMN `notify_scene_id` bigint DEFAULT NULL COMMENT '任务执行后关联的通知场景ID',
  MODIFY COLUMN `notify_trigger` varchar(20) NOT NULL DEFAULT 'FAILURE' COMMENT '通知触发时机：SUCCESS成功，FAILURE失败，ALWAYS总是',
  MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  MODIFY COLUMN `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

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
