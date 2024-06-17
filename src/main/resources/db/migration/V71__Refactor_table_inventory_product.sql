ALTER TABLE public.inventory_product DROP COLUMN IF EXISTS process_procedure_structure_id;
ALTER TABLE public.inventory_product ADD success_quantity int4 NULL;
ALTER TABLE public.inventory_product ADD ins_30_day_quantity int4 NULL;
ALTER TABLE public.inventory_product ADD product_name varchar(12) NULL;
ALTER TABLE public.inventory_product ADD process_code varchar(6) NULL;
ALTER TABLE public.inventory_product ADD layer_code varchar(2) NULL;
