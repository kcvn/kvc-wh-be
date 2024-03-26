CREATE TABLE tape_info
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    product_name VARCHAR(12) NOT NULL,
    month VARCHAR(2) NOT NULL,
    year VARCHAR(4) NOT NULL,
    request_date_start TIMESTAMP WITH TIME ZONE NOT NULL,
    request_date_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tape_shared VARCHAR(20) NOT NULL,
    type_tape VARCHAR(20) NOT NULL,
    quantity_tape INT NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    into_money DOUBLE PRECISION NOT NULL,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT tape_info_pkey PRIMARY KEY (id)
);