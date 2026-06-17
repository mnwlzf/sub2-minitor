CREATE TABLE IF NOT EXISTS mail_smtp_config (
    id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL DEFAULT '默认 SMTP',
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    username VARCHAR(255) NOT NULL,
    password_encrypted TEXT,
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(100),
    use_tls BOOLEAN NOT NULL DEFAULT TRUE,
    use_ssl BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mail_smtp_default
    ON mail_smtp_config(is_default)
    WHERE is_default = TRUE;

CREATE TABLE IF NOT EXISTS mail_notification_scene (
    id BIGSERIAL PRIMARY KEY,
    scene_key VARCHAR(100) NOT NULL UNIQUE,
    scene_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS mail_recipient (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS mail_scene_recipient (
    id BIGSERIAL PRIMARY KEY,
    scene_key VARCHAR(100) NOT NULL,
    recipient_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL DEFAULT 'TO',
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(scene_key, recipient_id, recipient_type)
);

CREATE INDEX IF NOT EXISTS idx_mail_scene_recipient_scene_key ON mail_scene_recipient(scene_key);
CREATE INDEX IF NOT EXISTS idx_mail_scene_recipient_recipient_id ON mail_scene_recipient(recipient_id);

CREATE TABLE IF NOT EXISTS mail_send_log (
    id BIGSERIAL PRIMARY KEY,
    scene_key VARCHAR(100),
    subject VARCHAR(255) NOT NULL,
    to_emails TEXT,
    cc_emails TEXT,
    bcc_emails TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    send_time TIMESTAMPTZ,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mail_send_log_scene_key ON mail_send_log(scene_key);
CREATE INDEX IF NOT EXISTS idx_mail_send_log_create_time ON mail_send_log(create_time DESC);

INSERT INTO mail_notification_scene(scene_key, scene_name, description)
VALUES
    ('balance_alert', '余额告警', '账号余额低于阈值或余额异常变化时发送'),
    ('collect_failed', '采集失败', '余额、倍率或分组采集失败时发送'),
    ('daily_report', '每日报表', '每日汇总平台、账号余额和消耗情况'),
    ('rate_changed', '倍率变更', '平台渠道倍率发生变化时发送'),
    ('account_exception', '账号异常', '账号登录失败、代理不可用或接口异常时发送')
ON CONFLICT (scene_key) DO NOTHING;
