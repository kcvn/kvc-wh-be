ALTER TABLE public.process_master ADD created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.process_master ADD created_by varchar(100) NULL;
ALTER TABLE public.process_master ADD updated_date timestamp NULL;
ALTER TABLE public.process_master ADD updated_by varchar(100) NULL;
ALTER TABLE public.process_master ADD is_deleted bool NOT NULL DEFAULT false;
ALTER TABLE public.process_master DROP COLUMN register_by;
ALTER TABLE public.process_master DROP COLUMN register_date;
ALTER TABLE public.process_master DROP COLUMN update_by;
ALTER TABLE public.process_master DROP COLUMN update_date;

ALTER TABLE public.app_setting ALTER COLUMN created_date TYPE timestamp USING created_date::timestamp;
ALTER TABLE public.app_setting ALTER COLUMN updated_date TYPE timestamp USING updated_date::timestamp;

ALTER TABLE public.process_procedure_structure ADD created_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.process_procedure_structure ADD created_by varchar(100) NULL;
ALTER TABLE public.process_procedure_structure ADD updated_date timestamp NULL;
ALTER TABLE public.process_procedure_structure ADD updated_by varchar(100) NULL;
ALTER TABLE public.process_procedure_structure ADD is_deleted bool NOT NULL DEFAULT false;
ALTER TABLE public.process_procedure_structure DROP COLUMN register_by;
ALTER TABLE public.process_procedure_structure DROP COLUMN register_date;
ALTER TABLE public.process_procedure_structure DROP COLUMN update_by;
ALTER TABLE public.process_procedure_structure DROP COLUMN update_date;

DROP INDEX public.idx_unique_product_process;
ALTER TABLE public.product_process DROP COLUMN product_name;
ALTER TABLE public.product_process DROP COLUMN layer_code;
ALTER TABLE public.product_process DROP COLUMN process_code;
ALTER TABLE public.product_process ADD process_procedure_structure_id varchar(50) NOT NULL;

CREATE UNIQUE INDEX idx_unique_product_process ON product_process (process_procedure_structure_id);

