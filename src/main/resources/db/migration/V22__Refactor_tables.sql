ALTER TABLE public.calculate_quantity RENAME TO calculate_quantity_result;
ALTER TABLE public.calculate_quantity_result RENAME COLUMN month_production_plan TO month_report;
ALTER TABLE public.calculate_quantity_result ALTER COLUMN month_report TYPE timestamp with time zone USING month_report::timestamp with time zone;
ALTER TABLE public.calculate_quantity_result RENAME COLUMN order_date TO order_date_from;
ALTER TABLE public.calculate_quantity_result ALTER COLUMN order_date_from TYPE timestamp with time zone USING order_date_from::timestamp with time zone;
ALTER TABLE public.calculate_quantity_result ADD order_date_to timestamp with time zone NOT NULL;
ALTER TABLE public.calculate_quantity_result ALTER COLUMN locked_date TYPE timestamp with time zone USING locked_date::timestamp with time zone;
ALTER TABLE public.calculate_quantity_result ALTER COLUMN calculate_date TYPE timestamp with time zone USING calculate_date::timestamp with time zone;


CREATE TABLE information_calculate_quantity (
    id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
    month_report timestamp with time zone NOT NULL,
    product_name varchar(30) NOT NULL,
    process_statistic_code varchar(10) NOT NULL,
    total_quantity int4,
    completion_rate numeric(5,2),
    order_date timestamp with time zone,
    process_count int,
    block_sh int,
    block_quantity int4,
    created_date timestamp with time zone,
    created_by varchar(50)
);