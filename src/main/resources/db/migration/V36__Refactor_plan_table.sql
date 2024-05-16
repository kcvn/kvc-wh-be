ALTER TABLE public.plan_detail ALTER COLUMN title TYPE varchar(30) USING title::varchar(30);
ALTER TABLE public.plan_process ADD process_sequence int NULL;