ALTER TABLE public.order_info ADD is_change_quantity bool NOT NULL DEFAULT false;

CREATE TABLE tape_en_route (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    export_type varchar(10) NOT NULL,
    purchase_order varchar(20) NOT NULL,
    item_code varchar(20) NULL,
    unit varchar(20) NOT NULL,
    description varchar(50) NOT NULL,
    spec varchar(50) NOT NULL,
    transmit varchar(5) NOT NULL,
    ordered_quantity int NOT NULL,
    delivered_quantity int NOT NULL,
    response_date TIMESTAMP WITH TIME ZONE NOT NULL,
    tape_lot varchar(20) NOT NULL,
    quantity int NOT NULL,
    number_of_boxes int NOT NULL,
    integration_confirmation bool NOT NULL DEFAULT false,
    contact_status varchar(20) NOT NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT  tape_en_route_pkey PRIMARY KEY (id)
);

CREATE TABLE tape_inventory (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    export_type varchar(10) NOT NULL,
    product_name varchar(20) NOT NULL,
    product_name_short_cut varchar(20) NOT NULL,
    tape_in_warehouse int NOT NULL,
    tape_in_department int NOT NULL,
    tape_ng int NOT NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT  tape_inventory_pkey PRIMARY KEY (id)
);