CREATE TABLE IF NOT EXISTS task_notification_scene (
    id BIGSERIAL PRIMARY KEY,
    task_key VARCHAR(128) NOT NULL UNIQUE,
    scene_key VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_task_notification_scene_task_key
    ON task_notification_scene(task_key);

CREATE INDEX IF NOT EXISTS idx_task_notification_scene_scene_key
    ON task_notification_scene(scene_key);

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
    'rate-change-detect',
    '分组及倍率变化采集',
    'RATE',
    '0 5/10 * * * ?',
    'com.sub2.monitor.scheduler.job.RateChangeDetectJob',
    '每 10 分钟对比每个平台最新两批分组及倍率变化，与采集任务错开 5 分钟，变化时通过 rate_changed 场景发送邮件',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (task_key) DO UPDATE
SET task_name = EXCLUDED.task_name,
    task_group = EXCLUDED.task_group,
    cron_expression = EXCLUDED.cron_expression,
    job_class = EXCLUDED.job_class,
    description = EXCLUDED.description,
    is_enabled = TRUE,
    update_time = NOW();

INSERT INTO task_notification_scene (
    task_key,
    scene_key,
    is_enabled,
    create_time,
    update_time
)
VALUES (
    'rate-change-detect',
    'rate_changed',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (task_key) DO UPDATE
SET scene_key = EXCLUDED.scene_key,
    is_enabled = TRUE,
    update_time = NOW();
