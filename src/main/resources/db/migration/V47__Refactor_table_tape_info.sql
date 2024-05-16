ALTER TABLE public.tape_info DROP COLUMN month;
ALTER TABLE public.tape_info DROP COLUMN year;
ALTER TABLE public.tape_info ADD month_report INT NOT NULL;
ALTER TABLE public.tape_info ADD year_report INT NOT NULL;