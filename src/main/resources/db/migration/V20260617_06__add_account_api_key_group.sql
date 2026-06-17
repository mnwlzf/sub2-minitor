CREATE TABLE IF NOT EXISTS account_api_key_group (
    id BIGSERIAL PRIMARY KEY,
    platform_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    platform_type VARCHAR(20) NOT NULL,
    remote_key_id VARCHAR(100) NOT NULL,
    key_name VARCHAR(255),
    key_status VARCHAR(50),
    group_name VARCHAR(255),
    current_rate NUMERIC(20, 8),
    actual_rate NUMERIC(20, 8),
    today_actual_cost NUMERIC(20, 8),
    total_actual_cost NUMERIC(20, 8),
    used_amount NUMERIC(20, 8),
    remain_amount NUMERIC(20, 8),
    collect_time TIMESTAMPTZ NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_account_api_key_group UNIQUE(platform_id, account_id, remote_key_id)
);

CREATE INDEX IF NOT EXISTS idx_account_api_key_group_account
    ON account_api_key_group(account_id);

CREATE INDEX IF NOT EXISTS idx_account_api_key_group_platform_group
    ON account_api_key_group(platform_id, group_name);
