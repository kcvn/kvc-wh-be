CREATE TABLE equipment_productivity (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    equipment_code varchar(20) NOT NULL,
    description varchar(200),
    frame_1 varchar(10) NOT NULL,
    process_code varchar(6) not null,
    mold varchar(10),
    operating_rate numeric(5,2) not null default 0,
    time int not null default 0,
    count numeric(5,2) not null default 0,
    task numeric(5,2) not null default 0,
    sheet_hour_100 numeric(5,2) not null default 0,
    block_sh numeric(5,2) not null default 0,
    sheet_hour numeric(5,2) not null default 0,
    sheet_day numeric(5,2) not null default 0,
    set_day numeric(5,2) not null default 0,
    block_day numeric(5,2) not null default 0,
    created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP with time zone NULL,
    updated_by varchar(100) NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT equipment_productivity_pkey PRIMARY KEY (id)
);

CREATE TABLE plan (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	order_id varchar(50) not null,
    order_code varchar(50) not null,
    start_date TIMESTAMP with time zone not null,
    end_date TIMESTAMP with time zone not null,
    version int not null,
    is_active boolean not null default true,
	created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP with time zone NULL,
	updated_by varchar(100) NULL,
	is_deleted boolean NOT NULL DEFAULT false,
	CONSTRAINT plan_pkey PRIMARY KEY (id)
);

CREATE TABLE plan_product (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    plan_id varchar(50) NOT NULL,
    product_name varchar(30) not null,
    frame_1 varchar(10) not null,
    mold varchar(10) not null,
    pcs_sh int not null default 0,
    block_sh int not null default 0,
	created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP with time zone NULL,
	updated_by varchar(100) NULL,
	is_deleted boolean NOT NULL DEFAULT false,
	CONSTRAINT plan_product_pkey PRIMARY KEY (id)
);

CREATE TABLE plan_process (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_id varchar(50) NOT NULL,
    plan_product_id varchar(50) NOT NULL,
    process_code varchar(6) not null,
    process_name varchar(60),
    process_convert_code varchar(10),
    layer_code varchar(2),
    completion_rate numeric(5,2),
    inventory int,
    parent_id varchar(50) ,
	created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP with time zone NULL,
	updated_by varchar(100) NULL,
	is_deleted boolean NOT NULL DEFAULT false,
	CONSTRAINT plan_process_pkey PRIMARY KEY (id)
);

CREATE TABLE plan_detail (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_id varchar(50) NOT NULL,
    plan_product_id varchar(50) NOT NULL,
    plan_process_id varchar(50) NOT NULL,
    title varchar(10),
    plan_date TIMESTAMP with time zone,
    quantity int not null default 0,
	created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP with time zone NULL,
	updated_by varchar(100) NULL,
	is_deleted boolean NOT NULL DEFAULT false,
	CONSTRAINT plan_detail_pkey PRIMARY KEY (id)
);

ALTER TABLE plan ADD CONSTRAINT plan_order_fkey FOREIGN KEY (order_id) REFERENCES "order" (id);
ALTER TABLE plan_product ADD CONSTRAINT plan_product_fkey FOREIGN KEY (plan_id) REFERENCES plan (id);
ALTER TABLE plan_process ADD CONSTRAINT plan_process_fkey FOREIGN KEY (plan_product_id) REFERENCES plan_product (id);
ALTER TABLE plan_detail ADD CONSTRAINT plan_detail_fkey FOREIGN KEY (plan_process_id) REFERENCES plan_process (id);