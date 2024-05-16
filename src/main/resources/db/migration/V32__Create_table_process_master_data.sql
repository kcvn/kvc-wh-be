CREATE TABLE process_master_data (
     id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
     process_code VARCHAR(6),
     group_process_code VARCHAR(6),
     "type" VARCHAR(50),
     value VARCHAR(50),
     unit VARCHAR(10),
     created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
     created_by VARCHAR(50),
     updated_date TIMESTAMP WITH TIME ZONE,
     updated_by VARCHAR(50),
     is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
     CONSTRAINT process_master_data_key PRIMARY KEY (id)
);



