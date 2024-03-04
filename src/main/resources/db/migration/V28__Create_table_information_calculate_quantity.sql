CREATE TABLE information_calculate_quantity (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	month_report timestamp with time zone NOT NULL,
	product_name varchar(50) NOT NULL,
	process_statistic varchar(50) NOT NULL,
	total_quantity_of_process int4 NOT NULL,
	created_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(50) NULL,
	updated_date timestamp with time zone NULL,
	updated_by varchar(50) NULL,
	is_deleted bool NOT NULL DEFAULT FALSE,
	calculate_quantity_result_id varchar(50) NOT NULL,
	CONSTRAINT information_calculate_quantity_pkey PRIMARY KEY (id),
	FOREIGN KEY (calculate_quantity_result_id) REFERENCES calculate_quantity_result(id)
);

ALTER TABLE public.information_calculate_quantity_detail ADD CONSTRAINT information_calculate_quantity_detail_information_calculate_quantity_fk FOREIGN KEY (information_calculate_quantity_id) REFERENCES public.information_calculate_quantity(id);