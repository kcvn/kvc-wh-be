ALTER TABLE public.plan_detail ADD sheet_quantity int NULL;
ALTER TABLE public.plan_detail ADD block_quantity int NULL;
ALTER TABLE public.plan_detail DROP COLUMN quantity;
