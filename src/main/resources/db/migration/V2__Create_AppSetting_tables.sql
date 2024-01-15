CREATE TABLE app_setting
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    key VARCHAR(100) NOT NULL,
    value VARCHAR(1000),
    description VARCHAR(1000),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT app_setting_pkey PRIMARY KEY (id)
);