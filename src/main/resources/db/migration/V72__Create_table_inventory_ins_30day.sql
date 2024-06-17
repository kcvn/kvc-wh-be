CREATE TABLE inventory_ins_30day (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    inventory_date TIMESTAMP with time zone NOT NULL,
    code varchar(50) NOT NULL,
    product_name varchar(12) not null,
    process_code varchar(6) NULL,
	layer_code varchar(2) NULL,
    product_quantity int4 NOT NULL DEFAULT 0,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT inventory_ins_30day_pkey PRIMARY KEY (id)
);