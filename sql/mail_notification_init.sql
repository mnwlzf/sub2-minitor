SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mail_smtp_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_name` varchar(100) NOT NULL DEFAULT 'default' COMMENT '配置名称',
  `host` varchar(255) NOT NULL COMMENT 'SMTP主机',
  `port` int NOT NULL COMMENT 'SMTP端口',
  `username` varchar(255) NOT NULL COMMENT 'SMTP用户名',
  `password` varchar(512) DEFAULT NULL COMMENT 'SMTP密码，后续建议加密存储',
  `from_email` varchar(255) NOT NULL COMMENT '发件人邮箱',
  `from_name` varchar(100) DEFAULT NULL COMMENT '发件人名称',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `use_tls` tinyint NOT NULL DEFAULT 0 COMMENT '是否使用STARTTLS：1是，0否',
  `use_ssl` tinyint NOT NULL DEFAULT 1 COMMENT '是否使用SSL：1是，0否',
  `is_default` tinyint NOT NULL DEFAULT 1 COMMENT '是否默认配置：1是，0否',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail_smtp_config_name` (`config_name`),
  KEY `idx_mail_smtp_config_enabled` (`enabled`),
  KEY `idx_mail_smtp_config_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SMTP配置表';

CREATE TABLE IF NOT EXISTS `mail_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `email` varchar(255) NOT NULL COMMENT '收件人邮箱',
  `recipient_name` varchar(100) DEFAULT NULL COMMENT '收件人名称',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail_recipient_email` (`email`),
  KEY `idx_mail_recipient_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件收件人表';

CREATE TABLE IF NOT EXISTS `mail_notify_scene` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scene_code` varchar(64) NOT NULL COMMENT '通知场景编码',
  `scene_name` varchar(100) NOT NULL COMMENT '通知场景名称',
  `description` varchar(255) DEFAULT NULL COMMENT '通知场景描述',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `smtp_config_id` bigint DEFAULT NULL COMMENT '关联SMTP配置ID',
  `subject_template` varchar(255) DEFAULT NULL COMMENT '邮件标题模板',
  `content_template` text DEFAULT NULL COMMENT '邮件正文模板',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail_notify_scene_code` (`scene_code`),
  KEY `idx_mail_notify_scene_enabled` (`enabled`),
  KEY `idx_mail_notify_scene_smtp` (`smtp_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件通知场景表';

CREATE TABLE IF NOT EXISTS `mail_notify_scene_recipient` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scene_id` bigint NOT NULL COMMENT '通知场景ID',
  `recipient_id` bigint NOT NULL COMMENT '收件人ID',
  `recipient_type` varchar(10) NOT NULL DEFAULT 'TO' COMMENT '收件人类型：TO主送，CC抄送，BCC密送',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mail_scene_recipient_type` (`scene_id`, `recipient_id`, `recipient_type`),
  KEY `idx_mail_scene_recipient_scene` (`scene_id`),
  KEY `idx_mail_scene_recipient_recipient` (`recipient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知场景收件人关系表';

INSERT INTO `mail_smtp_config` (`config_name`, `host`, `port`, `username`, `password`, `from_email`, `from_name`, `enabled`, `use_tls`, `use_ssl`, `is_default`, `remark`)
VALUES ('default', 'smtp.qq.com', 465, '3097553108@qq.com', NULL, '3097553108@qq.com', 'Sub2 Monitor', 1, 0, 1, 1, '默认SMTP配置')
ON DUPLICATE KEY UPDATE
  `host` = VALUES(`host`),
  `port` = VALUES(`port`),
  `username` = VALUES(`username`),
  `from_email` = VALUES(`from_email`),
  `from_name` = VALUES(`from_name`),
  `enabled` = VALUES(`enabled`),
  `use_tls` = VALUES(`use_tls`),
  `use_ssl` = VALUES(`use_ssl`),
  `is_default` = VALUES(`is_default`),
  `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `mail_notify_scene` (`scene_code`, `scene_name`, `description`, `enabled`, `smtp_config_id`, `subject_template`, `content_template`)
VALUES
('BALANCE_ALERT', '余额告警', '账号余额低于阈值或余额异常变化时发送', 1, NULL, 'Sub2 Monitor 余额告警', NULL),
('COLLECT_FAILED', '采集失败', '余额、倍率或分组采集失败时发送', 1, NULL, 'Sub2 Monitor 采集失败', NULL),
('DAILY_REPORT', '每日报表', '每日汇总平台、账号余额和消耗情况', 1, NULL, 'Sub2 Monitor 每日报表', NULL),
('RATIO_CHANGED', '渠道及倍率变更', '平台渠道倍率发生变化时发送', 1, NULL, 'Sub2 Monitor 渠道及倍率变更', NULL),
('ACCOUNT_ERROR', '账号异常', '账号登录失败、代理不可用或接口异常时发送', 1, NULL, 'Sub2 Monitor 账号异常', NULL)
ON DUPLICATE KEY UPDATE
  `scene_name` = VALUES(`scene_name`),
  `description` = VALUES(`description`),
  `enabled` = VALUES(`enabled`),
  `subject_template` = VALUES(`subject_template`),
  `updated_at` = CURRENT_TIMESTAMP;
