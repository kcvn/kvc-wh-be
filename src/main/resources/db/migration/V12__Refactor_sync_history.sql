ALTER TABLE public.sync_history ALTER COLUMN created_date TYPE timestamp USING created_date::timestamp;
ALTER TABLE public.sync_history ALTER COLUMN updated_date TYPE timestamp USING updated_date::timestamp;
