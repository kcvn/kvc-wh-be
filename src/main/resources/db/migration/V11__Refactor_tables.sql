ALTER TABLE public.completion_rate_process ALTER COLUMN process_code TYPE varchar(6) USING process_code::varchar(6);
ALTER TABLE public.completion_rate_process ALTER COLUMN "key" TYPE varchar(7) USING "key"::varchar(7);

ALTER TABLE public.auth_password_reset_token ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.auth_password_reset_token ALTER COLUMN user_id TYPE varchar(50) USING user_id::varchar(50);
ALTER TABLE public.auth_password_reset_token ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_password_reset_token ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.auth_role ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.auth_role ALTER COLUMN "name" TYPE varchar(300) USING "name"::varchar(300);
ALTER TABLE public.auth_role ALTER COLUMN description TYPE varchar(1000) USING description::varchar(1000);
ALTER TABLE public.auth_role ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_role ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.auth_role_claim ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.auth_role_claim ALTER COLUMN role_id TYPE varchar(50) USING role_id::varchar(50);
ALTER TABLE public.auth_role_claim ALTER COLUMN claim_type TYPE varchar(50) USING claim_type::varchar(50);
ALTER TABLE public.auth_role_claim ALTER COLUMN claim_value TYPE varchar USING claim_value::varchar;
ALTER TABLE public.auth_role_claim ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_role_claim ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.auth_user ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.auth_user ALTER COLUMN username TYPE varchar(100) USING username::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN "password" TYPE varchar(100) USING "password"::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN employee_code TYPE varchar(20) USING employee_code::varchar(20);
ALTER TABLE public.auth_user ALTER COLUMN email TYPE varchar(100) USING email::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN phone_number TYPE varchar(20) USING phone_number::varchar(20);
ALTER TABLE public.auth_user ALTER COLUMN full_name TYPE varchar(100) USING full_name::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN full_name_unsigned TYPE varchar(100) USING full_name_unsigned::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN avatar TYPE varchar(1000) USING avatar::varchar(1000);
ALTER TABLE public.auth_user ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_user ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.auth_user_claim ALTER COLUMN id TYPE varchar(50) USING id::varchar(50);
ALTER TABLE public.auth_user_claim ALTER COLUMN user_id TYPE varchar(50) USING user_id::varchar(50);
ALTER TABLE public.auth_user_claim ALTER COLUMN claim_type TYPE varchar(50) USING claim_type::varchar(50);
ALTER TABLE public.auth_user_claim ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_user_claim ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.auth_user_role ALTER COLUMN user_id TYPE varchar(50) USING user_id::varchar(50);
ALTER TABLE public.auth_user_role ALTER COLUMN role_id TYPE varchar(50) USING role_id::varchar(50);
ALTER TABLE public.auth_user_role ALTER COLUMN created_by TYPE varchar(100) USING created_by::varchar(100);
ALTER TABLE public.auth_user_role ALTER COLUMN updated_by TYPE varchar(100) USING updated_by::varchar(100);

ALTER TABLE public.completion_rate_process_product ALTER COLUMN "key" TYPE varchar(14) USING "key"::varchar(14);
ALTER TABLE public.completion_rate_process_product ALTER COLUMN product_name_shortcut TYPE varchar(7) USING product_name_shortcut::varchar(7);
ALTER TABLE public.completion_rate_process_product ALTER COLUMN process_code TYPE varchar(6) USING process_code::varchar(6);

ALTER TABLE public.completion_rate_product ALTER COLUMN product_name TYPE varchar(12) USING product_name::varchar(12);

ALTER TABLE public.product_process ALTER COLUMN process_name TYPE varchar(60) USING process_name::varchar(60);

