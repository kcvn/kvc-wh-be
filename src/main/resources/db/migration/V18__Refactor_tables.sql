ALTER TABLE public.completion_rate_product ADD effective_date timestamp with time zone NOT NULL;
ALTER TABLE public.completion_rate_product ADD expiration_date timestamp with time zone NULL;