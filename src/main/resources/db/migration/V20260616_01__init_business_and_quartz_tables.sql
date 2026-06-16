-- 业务表
CREATE TABLE IF NOT EXISTS platform (
    id BIGSERIAL PRIMARY KEY,
    base_url VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(10) NOT NULL,
    recharge_amount NUMERIC(20, 2) NOT NULL,
    received_amount NUMERIC(20, 2) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password TEXT NOT NULL,
    platform_id BIGINT NOT NULL,
    test_model VARCHAR(100),
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS account_balance_history (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    platform_id BIGINT NOT NULL,
    platform VARCHAR(100) NOT NULL,
    current_balance NUMERIC(20, 2) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform_rate_history (
    id BIGSERIAL PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    channel_name VARCHAR(100) NOT NULL,
    current_rate NUMERIC(10, 4) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS smtp_config (
    id BIGSERIAL PRIMARY KEY,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INTEGER NOT NULL,
    smtp_username VARCHAR(255) NOT NULL,
    smtp_password TEXT NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    use_ssl BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL UNIQUE,
    config_value TEXT,
    config_desc VARCHAR(255),
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS task_schedule (
    id BIGSERIAL PRIMARY KEY,
    task_key VARCHAR(128) NOT NULL UNIQUE,
    task_name VARCHAR(128) NOT NULL,
    task_group VARCHAR(64) NOT NULL,
    cron_expression VARCHAR(120) NOT NULL,
    job_class VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS task_execution_log (
    id BIGSERIAL PRIMARY KEY,
    task_key VARCHAR(128) NOT NULL,
    task_name VARCHAR(128) NOT NULL,
    cron_expression VARCHAR(120),
    trigger_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    fire_time TIMESTAMPTZ,
    finish_time TIMESTAMPTZ,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_accounts_platform_id ON accounts(platform_id);
CREATE INDEX IF NOT EXISTS idx_balance_account_id ON account_balance_history(account_id);
CREATE INDEX IF NOT EXISTS idx_balance_platform_id ON account_balance_history(platform_id);
CREATE INDEX IF NOT EXISTS idx_rate_platform_id ON platform_rate_history(platform_id);
CREATE INDEX IF NOT EXISTS idx_rate_channel_name ON platform_rate_history(channel_name);
CREATE INDEX IF NOT EXISTS idx_task_schedule_enabled ON task_schedule(is_enabled);
CREATE INDEX IF NOT EXISTS idx_task_schedule_group ON task_schedule(task_group);
CREATE INDEX IF NOT EXISTS idx_task_log_task_key ON task_execution_log(task_key);
CREATE INDEX IF NOT EXISTS idx_task_log_create_time ON task_execution_log(create_time DESC);

-- Quartz 标准表（PostgreSQL）
CREATE TABLE IF NOT EXISTS qrtz_job_details (
    sched_name VARCHAR(120) NOT NULL,
    job_name VARCHAR(200) NOT NULL,
    job_group VARCHAR(200) NOT NULL,
    description VARCHAR(250) NULL,
    job_class_name VARCHAR(250) NOT NULL,
    is_durable BOOLEAN NOT NULL,
    is_nonconcurrent BOOLEAN NOT NULL,
    is_update_data BOOLEAN NOT NULL,
    requests_recovery BOOLEAN NOT NULL,
    job_data BYTEA NULL,
    PRIMARY KEY (sched_name, job_name, job_group)
);

CREATE TABLE IF NOT EXISTS qrtz_triggers (
    sched_name VARCHAR(120) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    job_name VARCHAR(200) NOT NULL,
    job_group VARCHAR(200) NOT NULL,
    description VARCHAR(250) NULL,
    next_fire_time BIGINT NULL,
    prev_fire_time BIGINT NULL,
    priority INTEGER NULL,
    trigger_state VARCHAR(16) NOT NULL,
    trigger_type VARCHAR(8) NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NULL,
    calendar_name VARCHAR(200) NULL,
    misfire_instr SMALLINT NULL,
    job_data BYTEA NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_simple_triggers (
    sched_name VARCHAR(120) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    repeat_count BIGINT NOT NULL,
    repeat_interval BIGINT NOT NULL,
    times_triggered BIGINT NOT NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_cron_triggers (
    sched_name VARCHAR(120) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    cron_expression VARCHAR(120) NOT NULL,
    time_zone_id VARCHAR(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_blob_triggers (
    sched_name VARCHAR(120) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    blob_data BYTEA NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_simprop_triggers (
    sched_name VARCHAR(120) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    str_prop_1 VARCHAR(512),
    str_prop_2 VARCHAR(512),
    str_prop_3 VARCHAR(512),
    int_prop_1 INTEGER,
    int_prop_2 INTEGER,
    long_prop_1 BIGINT,
    long_prop_2 BIGINT,
    dec_prop_1 NUMERIC(13, 4),
    dec_prop_2 NUMERIC(13, 4),
    bool_prop_1 BOOLEAN,
    bool_prop_2 BOOLEAN,
    time_zone_id VARCHAR(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_calendars (
    sched_name VARCHAR(120) NOT NULL,
    calendar_name VARCHAR(200) NOT NULL,
    calendar BYTEA NOT NULL,
    PRIMARY KEY (sched_name, calendar_name)
);

CREATE TABLE IF NOT EXISTS qrtz_paused_trigger_grps (
    sched_name VARCHAR(120) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    PRIMARY KEY (sched_name, trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_fired_triggers (
    sched_name VARCHAR(120) NOT NULL,
    entry_id VARCHAR(95) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    instance_name VARCHAR(200) NOT NULL,
    fired_time BIGINT NOT NULL,
    sched_time BIGINT NOT NULL,
    priority INTEGER NOT NULL,
    state VARCHAR(16) NOT NULL,
    job_name VARCHAR(200) NULL,
    job_group VARCHAR(200) NULL,
    is_nonconcurrent BOOLEAN NOT NULL,
    requests_recovery BOOLEAN NULL,
    PRIMARY KEY (sched_name, entry_id)
);

CREATE TABLE IF NOT EXISTS qrtz_scheduler_state (
    sched_name VARCHAR(120) NOT NULL,
    instance_name VARCHAR(200) NOT NULL,
    last_checkin_time BIGINT NOT NULL,
    checkin_interval BIGINT NOT NULL,
    PRIMARY KEY (sched_name, instance_name)
);

CREATE TABLE IF NOT EXISTS qrtz_locks (
    sched_name VARCHAR(120) NOT NULL,
    lock_name VARCHAR(40) NOT NULL,
    PRIMARY KEY (sched_name, lock_name)
);

ALTER TABLE qrtz_triggers
    ADD CONSTRAINT fk_qrtz_triggers_job_details
        FOREIGN KEY (sched_name, job_name, job_group)
        REFERENCES qrtz_job_details (sched_name, job_name, job_group);

ALTER TABLE qrtz_simple_triggers
    ADD CONSTRAINT fk_qrtz_simple_triggers_triggers
        FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group);

ALTER TABLE qrtz_cron_triggers
    ADD CONSTRAINT fk_qrtz_cron_triggers_triggers
        FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group);

ALTER TABLE qrtz_simprop_triggers
    ADD CONSTRAINT fk_qrtz_simprop_triggers_triggers
        FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group);

ALTER TABLE qrtz_blob_triggers
    ADD CONSTRAINT fk_qrtz_blob_triggers_triggers
        FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group);

CREATE INDEX IF NOT EXISTS idx_qrtz_t_j ON qrtz_triggers(sched_name, job_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_jg ON qrtz_triggers(sched_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_c ON qrtz_triggers(sched_name, calendar_name);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_g ON qrtz_triggers(sched_name, trigger_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_state ON qrtz_triggers(sched_name, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_t_n_state ON qrtz_triggers(sched_name, trigger_name, trigger_group, trigger_state);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_trig_inst_name ON qrtz_fired_triggers(sched_name, instance_name);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_inst_job_req_rcvry ON qrtz_fired_triggers(sched_name, requests_recovery);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_j_g ON qrtz_fired_triggers(sched_name, job_name, job_group);
CREATE INDEX IF NOT EXISTS idx_qrtz_ft_t_g ON qrtz_fired_triggers(sched_name, trigger_name, trigger_group);
