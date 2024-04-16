ALTER TABLE public.tape_inventory ADD stocktaking_day TIMESTAMP WITH TIME ZONE NOT NULL;
ALTER TABLE public.tape_en_route ADD coupon_code varchar(20) NOT NULL;
