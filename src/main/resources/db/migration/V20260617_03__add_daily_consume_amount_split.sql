ALTER TABLE daily_account_consume_summary
    ADD COLUMN IF NOT EXISTS platform_consume_amount NUMERIC(20, 2);

ALTER TABLE daily_account_consume_summary
    ADD COLUMN IF NOT EXISTS actual_consume_amount NUMERIC(20, 2);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'daily_account_consume_summary'
          AND column_name = 'consume_amount'
    ) THEN
        EXECUTE '
            UPDATE daily_account_consume_summary
            SET platform_consume_amount = COALESCE(platform_consume_amount, consume_amount),
                actual_consume_amount = COALESCE(actual_consume_amount, consume_amount)
            WHERE platform_consume_amount IS NULL
               OR actual_consume_amount IS NULL
        ';
    END IF;
END $$;

UPDATE daily_account_consume_summary
SET platform_consume_amount = COALESCE(platform_consume_amount, 0),
    actual_consume_amount = COALESCE(actual_consume_amount, 0)
WHERE platform_consume_amount IS NULL
   OR actual_consume_amount IS NULL;

ALTER TABLE daily_account_consume_summary
    ALTER COLUMN platform_consume_amount SET NOT NULL;

ALTER TABLE daily_account_consume_summary
    ALTER COLUMN actual_consume_amount SET NOT NULL;

ALTER TABLE daily_account_consume_summary
    DROP COLUMN IF EXISTS consume_amount;
