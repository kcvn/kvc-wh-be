ALTER TABLE public.completion_rate_product ADD effective_date timestamptz NOT NULL;
ALTER TABLE public.completion_rate_product ADD expiration_date timestamptz NULL;