CREATE TABLE inventory_product (
     id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
     inventory_date DATE NOT NULL ,
     processed_products_number INT,
     raw_material_sheets_number INT,
     purchase_order VARCHAR(255),
     raw_material_batch varchar(50),
     management_number INT,
     process_procedure_structure_id varchar(50) NOT NULL,
     created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     created_by varchar(100) NULL,
     updated_date TIMESTAMP NULL,
     updated_by varchar(100) NULL,
     is_deleted boolean NOT NULL DEFAULT false,
     CONSTRAINT inventory_product_pkey PRIMARY KEY (id)
);