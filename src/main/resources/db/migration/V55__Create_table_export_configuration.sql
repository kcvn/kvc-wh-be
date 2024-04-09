CREATE TABLE export_configuration (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	product_name varchar(50) NOT NULL,
    export_type varchar(50) NOT NULL,
    rate numeric(5, 2) NOT NULL,
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL,
	CONSTRAINT export_config_pkey PRIMARY KEY (id)
);