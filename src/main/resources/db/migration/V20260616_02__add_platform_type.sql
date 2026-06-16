ALTER TABLE public.platform
    ADD COLUMN IF NOT EXISTS type VARCHAR(10);

COMMENT ON COLUMN public.platform.type IS '平台';
