ALTER TABLE public.completion_rate_process ALTER COLUMN rate TYPE numeric(5, 2) USING rate::numeric(5, 2);

ALTER TABLE public.completion_rate_process_product ALTER COLUMN rate TYPE numeric(5, 2) USING rate::numeric(5, 2);

ALTER TABLE public.completion_rate_product ALTER COLUMN rate TYPE numeric(5, 2) USING rate::numeric(5, 2);
