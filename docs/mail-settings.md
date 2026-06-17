# 邮件设置与通知场景

邮件模块分为两层：

- SMTP 配置：负责“如何发信”。
- 通知场景收件人：负责“某类业务邮件发给谁”。

业务代码不要直接读取 SMTP 表，也不要硬编码收件人。统一调用 `MailService.sendByScene(sceneKey, subject, content)`。

## 数据表

### mail_smtp_config

保存默认 SMTP 发信配置。

关键字段：

- `host`：SMTP 主机，例如 `smtp.qq.com`。
- `port`：SMTP 端口，例如 `465` 或 `587`。
- `username`：SMTP 用户名。
- `password_encrypted`：加密后的 SMTP 密码，不返回前端。
- `from_email`：发件人邮箱。
- `from_name`：发件人名称。
- `use_tls`：启用 STARTTLS，常见于 587。
- `use_ssl`：启用 SSL，常见于 465。
- `is_enabled`：是否启用。
- `is_default`：默认配置。当前业务只使用默认 SMTP。

密码使用 `app.mail.secret` 派生 AES key 加密。生产环境应配置稳定的 `app.mail.secret`，否则密钥变化后旧密码无法解密。

### mail_notification_scene

通知场景表。每类业务邮件对应一个 `scene_key`。

初始化场景：

- `balance_alert`：余额告警
- `collect_failed`：采集失败
- `daily_report`：每日报表
- `rate_changed`：倍率变更
- `account_exception`：账号异常

新增业务邮件时，优先新增场景，而不是新增 SMTP 配置。

### mail_recipient

全局收件人池。收件人可以被多个通知场景复用。

### mail_scene_recipient

通知场景和收件人的关联表。

`recipient_type` 支持：

- `TO`
- `CC`
- `BCC`

### mail_send_log

邮件发送日志。

`status` 支持：

- `SUCCESS`
- `FAILED`
- `SKIPPED`

## 后端入口

### 配置接口

控制器：`MailSettingsController`

接口前缀：`/api/mail-settings`

主要接口：

- `GET /smtp`：获取默认 SMTP 配置，不返回密码明文。
- `PUT /smtp`：保存默认 SMTP 配置。密码留空时保留旧密码。
- `POST /smtp/test`：测试 SMTP 连接和认证。
- `GET /recipients`：查询收件人。
- `POST /recipients`：新增收件人。
- `PUT /recipients`：更新收件人。
- `DELETE /recipients/{id}`：删除收件人。
- `GET /scenes`：查询通知场景及已绑定收件人。
- `POST /scenes/recipients`：给场景绑定收件人。
- `DELETE /scenes/recipients/{relationId}`：移除场景收件人。

### 业务发送入口

服务接口：`MailService`

```java
mailService.sendByScene(
        "collect_failed",
        "余额采集失败",
        "<p>平台 A 采集失败，请检查账号或代理。</p>"
);
```

发送流程：

1. 查询 `mail_notification_scene`，场景不存在或停用则记录 `SKIPPED`。
2. 查询默认启用的 `mail_smtp_config`。
3. 查询场景绑定的启用收件人，按 TO / CC / BCC 分组。
4. 发送邮件。
5. 写入 `mail_send_log`。

## 前端页面

页面：`web/src/views/MailSettingsView.vue`

路由：`/mail-settings`

页面包含两个 Tab：

- SMTP 设置：维护 SMTP 主机、端口、用户名、密码、发件人、TLS/SSL。
- 通知收件人：维护收件人，并按场景绑定 TO/CC/BCC。

前端 API 定义在 `web/src/api/monitor.ts`。

## 新增邮件功能的步骤

1. 在迁移脚本中新增 `mail_notification_scene` 数据，或通过数据库插入新场景。
2. 在“邮件设置 / 通知收件人”页面给该场景绑定收件人。
3. 在业务服务中注入 `MailService`。
4. 调用 `sendByScene(sceneKey, subject, content)`。
5. 根据需要在 `mail_send_log` 查看发送结果。

不要在业务代码里直接创建 `JavaMailSenderImpl`，也不要在业务代码里直接查询收件人表。
