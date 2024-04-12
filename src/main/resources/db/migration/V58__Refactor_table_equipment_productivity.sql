ALTER TABLE public.equipment_productivity RENAME COLUMN machine_number TO quantity_machine;
ALTER TABLE public.equipment_productivity ADD production_start_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.equipment_productivity ADD production_end_date TIMESTAMP WITH TIME ZONE NULL;
