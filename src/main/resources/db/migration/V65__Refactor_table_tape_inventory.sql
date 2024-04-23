ALTER TABLE public.tape_en_route ALTER COLUMN unit DROP NOT NULL;

ALTER TABLE public.tape_inventory ALTER COLUMN tape_in_warehouse DROP NOT NULL;
ALTER TABLE public.tape_inventory ALTER COLUMN tape_in_department DROP NOT NULL;
ALTER TABLE public.tape_inventory ALTER COLUMN tape_ng DROP NOT NULL;

