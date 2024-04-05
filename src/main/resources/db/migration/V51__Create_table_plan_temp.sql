CREATE TABLE plan_temp (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_code varchar(50) NULL,
	description varchar(500) null,
	start_date TIMESTAMP WITH TIME ZONE NOT NULL,
	end_date TIMESTAMP WITH TIME ZONE NOT NULL,
	"version" int NOT NULL,
	is_active bool NOT NULL DEFAULT true,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT plan_temp_pkey PRIMARY KEY (id)
);

CREATE TABLE plan_product_temp (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_id varchar(50) NOT NULL,
	product_name varchar(30) NOT NULL,
	frame_1 varchar(10) NOT NULL,
	mold varchar(10) NOT NULL,
	pcs_sh int4 NOT NULL DEFAULT 0,
	block_sh int4 NOT NULL DEFAULT 0,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT plan_product_temp_pkey PRIMARY KEY (id),
	CONSTRAINT plan_product_temp_fkey FOREIGN KEY (plan_id) REFERENCES plan_temp(id)
);

CREATE TABLE plan_process_temp (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_id varchar(50) NOT NULL,
	plan_product_id varchar(50) NOT NULL,
	process_code varchar(6) NOT NULL,
	process_name varchar(60) NULL,
	process_convert_code varchar(10) NULL,
	layer_code varchar(2) NULL,
	completion_rate numeric(5, 2) NULL,
	inventory int NULL,
	parent_id varchar(50) NULL,
	unit varchar(20) NULL,
    process_sequence int NULL,
    process_name_jp varchar(100) NULL,
    process_group varchar(5) NULL,
    process_statistic_code varchar(10) NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,

	CONSTRAINT plan_process_temp_pkey PRIMARY KEY (id),
	CONSTRAINT plan_process_temp_fkey FOREIGN KEY (plan_product_id) REFERENCES plan_product_temp(id)
);

CREATE TABLE plan_detail_temp (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	plan_id varchar(50) NOT NULL,
	plan_product_id varchar(50) NOT NULL,
	plan_process_id varchar(50) NOT NULL,
	title varchar(30) NULL,
	plan_date TIMESTAMP WITH TIME ZONE NULL,
	sheet_quantity int NULL,
    block_quantity int NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,

	CONSTRAINT plan_detail_temp_pkey PRIMARY KEY (id),
	CONSTRAINT plan_detail_temp_fkey FOREIGN KEY (plan_process_id) REFERENCES plan_process_temp(id)
);

CREATE TABLE system_lock (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	type varchar(50) NOT NULL,
    is_lock bool NOT NULL DEFAULT false,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP WITH TIME ZONE NULL,
    updated_by varchar(100) NULL,
    is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT system_lock_pkey PRIMARY KEY (id)
);

ALTER TABLE public.plan ADD description varchar(500) NULL;
