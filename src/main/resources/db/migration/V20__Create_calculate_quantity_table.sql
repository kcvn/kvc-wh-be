CREATE TABLE calculate_quantity (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid() ,
	month_production_plan timestamptz NOT NULL,
	order_date varchar(30) NOT NULL,
	status bool NOT NULL DEFAULT FALSE,
	calculate_by varchar(30) NULL,
	calculate_date timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	locked_by varchar(30) NULL,
	locked_date timestamptz NULL
);