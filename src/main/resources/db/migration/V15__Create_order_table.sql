CREATE TABLE "order"
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    order_code VARCHAR(50) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT order_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_order ON "order" (order_code, start_date, end_date, version);

CREATE TABLE order_detail
(
    id VARCHAR(50) NOT NULL DEFAULT GEN_RANDOM_UUID(),
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE,
    quantity INT NOT NULL DEFAULT 0,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT order_detail_pkey PRIMARY KEY (id)
);