ALTER TABLE public.export_configuration
    ADD created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD created_by VARCHAR(100) NULL,
    ADD updated_date TIMESTAMP WITH TIME ZONE NULL,
    ADD updated_by VARCHAR(100) NULL,
    ADD is_deleted BOOLEAN NOT NULL DEFAULT false
