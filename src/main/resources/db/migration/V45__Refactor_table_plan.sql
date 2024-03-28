ALTER TABLE public.plan DROP CONSTRAINT plan_order_fkey;
ALTER TABLE public.plan DROP COLUMN order_id;
ALTER TABLE public.plan DROP COLUMN order_code;
ALTER TABLE public.plan ADD plan_code varchar(50) NULL;

ALTER TABLE public.plan_process ADD process_group varchar(5) NULL;

