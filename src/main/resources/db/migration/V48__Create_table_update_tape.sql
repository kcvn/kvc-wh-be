CREATE TABLE update_tape
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    delivery_type VARCHAR(12),
    delivery_quantity INT NOT NULL,
    response_date TIMESTAMP WITH TIME ZONE NOT NULL,
    generic_name VARCHAR(100),
    short_name VARCHAR(100),
    delivery_type_short_name VARCHAR(100),
    delivery_type_in_cpn VARCHAR(12),
    short_name_in_cpn VARCHAR(100),
    inventory_number INT,
    nama_inventory INT,
    log_pd1_inventory INT,
    total_ng INT,
    shared_tape VARCHAR(100),
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT update_tape_pkey PRIMARY KEY (id)
);
