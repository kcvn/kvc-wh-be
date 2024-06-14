CREATE TABLE inventory_semi_product (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    inventory_date TIMESTAMP with time zone NOT NULL,
    product_name varchar(12) not null,
    tape_lot_no varchar(100),
    set_quantity int,
    block_quantity int,
    sum_block_quantity int,
    ng_block_quantity int,
    success_block_quantity int,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT inventory_semi_product_pkey PRIMARY KEY (id)
);