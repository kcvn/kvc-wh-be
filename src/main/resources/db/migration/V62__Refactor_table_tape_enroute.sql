ALTER TABLE public.tape_en_route ALTER COLUMN tape_lot DROP NOT NULL;
ALTER TABLE public.tape_en_route ALTER COLUMN quantity DROP NOT NULL;
ALTER TABLE public.tape_en_route ALTER COLUMN number_of_boxes DROP NOT NULL;
ALTER TABLE public.tape_en_route ALTER COLUMN integration_confirmation DROP NOT NULL;
ALTER TABLE public.tape_en_route ALTER COLUMN contact_status DROP NOT NULL;
