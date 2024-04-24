ALTER TABLE public.plan_detail_temp ADD order_date timestamp with time zone NULL;
ALTER TABLE public.plan_detail_temp ADD has_inventory bool NULL;

ALTER TABLE public.plan_detail ADD order_date timestamp with time zone NULL;
ALTER TABLE public.plan_detail ADD has_inventory bool NULL;