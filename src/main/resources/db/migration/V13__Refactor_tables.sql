ALTER TABLE public.app_setting ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.app_setting ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.auth_user ALTER COLUMN avatar TYPE varchar USING avatar::varchar;

ALTER TABLE public.common_category ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.common_category ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.completion_rate_process ALTER COLUMN effective_date TYPE timestamp with time zone USING effective_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process ALTER COLUMN expiration_date TYPE timestamp with time zone USING expiration_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.completion_rate_process_product ALTER COLUMN effective_date TYPE timestamp with time zone USING effective_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process_product ALTER COLUMN expiration_date TYPE timestamp with time zone USING expiration_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process_product ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.completion_rate_process_product ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.completion_rate_product ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.completion_rate_product ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.process_master ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.process_master ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.process_procedure_structure ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.process_procedure_structure ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.product ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.product ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.product_process ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.product_process ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;

ALTER TABLE public.sync_history ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.sync_history ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;
ALTER TABLE public.sync_history ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.sync_history ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.sync_history ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.work_result ALTER COLUMN summary_result_date TYPE timestamp with time zone USING summary_result_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_date TYPE timestamp with time zone USING work_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_time TYPE timestamp with time zone USING work_time::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_start_date TYPE timestamp with time zone USING work_start_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_start_time TYPE timestamp with time zone USING work_start_time::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_end_time TYPE timestamp with time zone USING work_end_time::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN work_end_date TYPE timestamp with time zone USING work_end_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN end_date TYPE timestamp with time zone USING end_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN created_date TYPE timestamp with time zone USING created_date::timestamp with time zone;
ALTER TABLE public.work_result ALTER COLUMN updated_date TYPE timestamp with time zone USING updated_date::timestamp with time zone;


