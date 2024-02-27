ALTER TABLE public.calculate_quantity_result ADD updated_date timestamp with time zone NULL;
ALTER TABLE public.calculate_quantity_result ADD updated_by varchar(100) NULL;
ALTER TABLE public.calculate_quantity_result ADD is_deleted bool NOT NULL DEFAULT FALSE;
ALTER TABLE public.calculate_quantity_result ADD CONSTRAINT calculate_quantity_result_pkey PRIMARY KEY (id);

ALTER TABLE public.information_calculate_quantity ADD CONSTRAINT information_calculate_quantity_pk PRIMARY KEY (id);
ALTER TABLE public.information_calculate_quantity ADD updated_date timestamp with time zone NULL;
ALTER TABLE public.information_calculate_quantity ADD updated_by varchar(100) NULL;
ALTER TABLE public.information_calculate_quantity ADD is_deleted bool NOT NULL DEFAULT FALSE;