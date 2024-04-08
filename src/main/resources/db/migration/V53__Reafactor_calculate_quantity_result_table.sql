ALTER TABLE public.information_calculate_quantity DROP COLUMN month_number;
ALTER TABLE public.information_calculate_quantity DROP COLUMN year_number;

ALTER TABLE public.calculate_quantity_result ADD month_number INT  NULL;
ALTER TABLE public.calculate_quantity_result ADD year_number INT  NULL;