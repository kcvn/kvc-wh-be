ALTER TABLE public.plan ADD "month" int NOT NULL default 0;
ALTER TABLE public.plan ADD "year" int NOT NULL default 0;

ALTER TABLE public.plan_temp ADD "month" int NOT NULL default 0;
ALTER TABLE public.plan_temp ADD "year" int NOT NULL default 0;

CREATE TABLE plan_calendar_config (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	month int not null,
	year int not null,
	start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by varchar(100) NULL,
    updated_date TIMESTAMP WITH TIME ZONE NULL,
    updated_by varchar(100) NULL,
    is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT plan_calendar_config_pkey PRIMARY KEY (id)
);