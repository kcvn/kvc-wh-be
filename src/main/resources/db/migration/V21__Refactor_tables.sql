DROP INDEX public.idx_unique_completion_rate_product;
CREATE INDEX completion_rate_product_product_name_idx ON public.completion_rate_product (product_name,effective_date);


CREATE INDEX completion_rate_process_key_idx ON public.completion_rate_process ("key",process_code,layer_code,effective_date);
DROP INDEX public.idx_unique_completion_rate_process;


CREATE INDEX completion_rate_process_product_key_idx ON public.completion_rate_process_product ("key",product_name_shortcut,process_code,layer_code,effective_date);
DROP INDEX public.idx_unique_completion_rate_process_product;
