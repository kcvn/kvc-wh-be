CREATE TABLE plan_color_config (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    index int NOT NULL default 0,
    color varchar(50) NOT NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT plan_color_config_pkey PRIMARY KEY (id)
);