ALTER TABLE public.equipment_productivity RENAME COLUMN process_code TO grp_process;
ALTER TABLE public.equipment_productivity ALTER COLUMN grp_process TYPE varchar(5) USING grp_process::varchar(5);
