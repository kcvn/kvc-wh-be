CREATE TABLE order_info
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    product_name VARCHAR(12) NOT NULL,
    frame_1 varchar(10) not null,
    layer_count int not null,
    pcs_sh int not null,
    block_sh int not null,
    sr_nosr varchar(10),
    version int not null default 0,
    order_date TIMESTAMP WITH TIME ZONE,
    quantity INT NOT NULL DEFAULT 0,
    is_latest boolean NOT NULL DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT order_info_pkey PRIMARY KEY (id)
);

CREATE TABLE order_version_dropdown
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    version varchar(10) not null,
    label varchar(15) not null,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT order_version_dropdown_pkey PRIMARY KEY (id)
);