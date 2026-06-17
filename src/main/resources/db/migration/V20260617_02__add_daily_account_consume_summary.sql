CREATE TABLE IF NOT EXISTS daily_account_consume_summary (
    id BIGSERIAL PRIMARY KEY,
    summary_date DATE NOT NULL,
    platform_id BIGINT NOT NULL,
    platform_name VARCHAR(100) NOT NULL,
    account_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    start_balance NUMERIC(20, 2) NOT NULL,
    end_balance NUMERIC(20, 2) NOT NULL,
    platform_consume_amount NUMERIC(20, 2) NOT NULL,
    actual_consume_amount NUMERIC(20, 2) NOT NULL,
    first_balance_time TIMESTAMPTZ NOT NULL,
    last_balance_time TIMESTAMPTZ NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_daily_account_consume_summary UNIQUE(summary_date, account_id)
);

CREATE INDEX IF NOT EXISTS idx_daily_consume_summary_date
    ON daily_account_consume_summary(summary_date);

CREATE INDEX IF NOT EXISTS idx_daily_consume_platform_date
    ON daily_account_consume_summary(platform_id, summary_date);

INSERT INTO task_schedule (
    task_key,
    task_name,
    task_group,
    cron_expression,
    job_class,
    description,
    is_enabled,
    create_time,
    update_time
)
VALUES (
    'daily-data-summary',
    '每日数据汇总',
    'GENERAL',
    '0 0 0 * * ?',
    'com.sub2.monitor.scheduler.job.DailyDataSummaryJob',
    '每天北京时间 00:00 汇总前一天每个平台每个账号的余额消耗',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (task_key) DO NOTHING;
