SET NAMES utf8mb4;

ALTER TABLE `collect_group`
  ADD COLUMN `key_count` int NOT NULL DEFAULT 0 COMMENT '使用该分组的密钥数量' AFTER `raw_json`,
  ADD COLUMN `used_by_key` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否被密钥使用：1是，0否' AFTER `key_count`;

ALTER TABLE `collect_group`
  ADD KEY `idx_collect_group_used_by_key` (`used_by_key`);
