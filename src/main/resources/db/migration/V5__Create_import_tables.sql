CREATE TABLE product (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	name varchar(30) NOT NULL,
	export_type varchar(20) NOT NULL,
	size varchar(100) NOT NULL,
	frame_1 VARCHAR(10) NOT NULL,
	frame_2 VARCHAR(10) NOT NULL,
	mold VARCHAR(10) NOT NULL,
	product_line VARCHAR(100),
	sr_nosr VARCHAR(10),
    pcs_sh INT NOT NULL,
    sh_block INT NOT NULL,
    layer_count INT NOT NULL,
    ring_jig VARCHAR(5),
    process INT,
    snap_mold VARCHAR(100),
    tape_common VARCHAR(5),
    tape_type VARCHAR(10),
    product_layer_detail VARCHAR(2000),
	created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP NULL,
	updated_by varchar(100) NULL,
	is_deleted boolean NOT NULL DEFAULT false,
	CONSTRAINT product_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_product_name ON product (name);

CREATE TABLE completion_rate_product (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    product_name VARCHAR(20) NOT NULL,
    rate DECIMAL(3,2) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP NULL,
    updated_by varchar(100) NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT completion_rate_product_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_completion_rate_product ON completion_rate_product (product_name);

CREATE TABLE completion_rate_process (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    key VARCHAR(20) NOT NULL,
    process_code VARCHAR(10) NOT NULL,
    layer_code VARCHAR(2) NOT NULL,
    rate DECIMAL(3,2) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP NULL,
    updated_by varchar(100) NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT completion_rate_process_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_completion_rate_process ON completion_rate_process (key, process_code, layer_code);

CREATE TABLE completion_rate_process_product (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    key VARCHAR(20) NOT NULL,
    product_name_shortcut VARCHAR(10) NOT NULL,
    process_code VARCHAR(10) NOT NULL,
    layer_code VARCHAR(2) NOT NULL,
    rate DECIMAL(3,2) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP NULL,
    updated_by varchar(100) NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT completion_rate_process_product_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_completion_rate_process_product ON completion_rate_process_product (key, product_name_shortcut, process_code, layer_code);