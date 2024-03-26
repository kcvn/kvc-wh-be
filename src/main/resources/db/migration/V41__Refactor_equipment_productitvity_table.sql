ALTER TABLE public.equipment_productivity RENAME COLUMN sheet_day TO sltb_sheet;
ALTER TABLE public.equipment_productivity RENAME COLUMN sheet_hour TO sltb_hour;
ALTER TABLE public.equipment_productivity RENAME COLUMN set_day TO sltb_set;
ALTER TABLE public.equipment_productivity RENAME COLUMN block_day TO sltb_block;
ALTER TABLE public.equipment_productivity ADD cap_hour numeric(10, 2) NULL DEFAULT 0;
ALTER TABLE public.equipment_productivity ADD cap_sheet numeric(10, 2) NULL DEFAULT 0;
ALTER TABLE public.equipment_productivity ADD cap_set numeric(10, 2) NULL DEFAULT 0;
ALTER TABLE public.equipment_productivity ADD cap_block numeric(10, 2) NULL DEFAULT 0;
ALTER TABLE public.equipment_productivity ADD machine_number int NULL DEFAULT 0;
