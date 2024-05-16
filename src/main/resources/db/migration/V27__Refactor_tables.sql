DROP TABLE public.information_calculate_quantity;
ALTER TABLE public.calculate_quantity_result RENAME COLUMN order_date_from TO start_date;
ALTER TABLE public.calculate_quantity_result RENAME COLUMN order_date_to TO end_date;
ALTER TABLE public.calculate_quantity_result ADD order_date_from_to varchar(50) NULL;
ALTER TABLE public.calculate_quantity_result ADD created_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.calculate_quantity_result ADD created_by varchar(100) NULL;


CREATE TABLE public.information_calculate_quantity_detail (
	id varchar(50) DEFAULT gen_random_uuid() NOT NULL,
	month_report timestamp with time zone NOT NULL,
	product_name varchar(30) NOT NULL,
	process_statistic_code varchar(10) NOT NULL,
	quantity_process_statistic int4 NULL,
	completion_rate numeric(5, 2) NULL,
	order_date timestamp with time zone NULL,
	process_count int4 NULL,
	block_sh int4 NULL,
	block_quantity int4 NULL,
	information_calculate_quantity_id varchar(50) NOT NULL,
	created_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(50) NULL,
	updated_date timestamp with time zone NULL,
	updated_by varchar(100) NULL,
	is_deleted bool DEFAULT false NOT NULL,
	CONSTRAINT information_calculate_quantity_pk PRIMARY KEY (id)
);


