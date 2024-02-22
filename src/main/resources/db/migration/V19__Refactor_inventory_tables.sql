DROP TABLE IF EXISTS public.inventory_product;

CREATE TABLE inventory_product (
     id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
     code varchar(50) not null,
     inventory_date TIMESTAMP with time zone NOT NULL ,
     process_procedure_structure_id varchar(50) NOT NULL,
     product_quantity INT NOT NULL default 0,
     sheet_quantity int not null default 0,
     order_code varchar(100),
     tape_lot_no varchar(100),
     created_date TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
     created_by varchar(100) NULL,
     updated_date TIMESTAMP with time zone NULL,
     updated_by varchar(100) NULL,
     is_deleted boolean NOT NULL DEFAULT false,
     CONSTRAINT inventory_product_pkey PRIMARY KEY (id)
);