CREATE TABLE coupon_code_dropdown (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    key_value varchar(50) NOT NULL,
    label varchar(50) NOT NULL,
	created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP WITH TIME ZONE NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT  coupon_code_dropdown_pkey PRIMARY KEY (id)
);