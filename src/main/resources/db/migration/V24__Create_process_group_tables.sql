CREATE TABLE process_group (
     id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
     process_statistic_code varchar(10) not null,
     description varchar(500),
     description_jp varchar(500),
     sort_order numeric(5,2) default 1,
     created_date timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
     created_by varchar(100) NULL,
     updated_date timestamp with time zone NULL,
     updated_by varchar(100) NULL,
     is_deleted boolean NOT NULL DEFAULT false,
     CONSTRAINT process_group_pkey PRIMARY KEY (id)
);

insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('W', 'Dán khung', 1, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('T', 'Đục lỗ', 2, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('U', 'Điền mực', 3, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('INLO', 'In lỗ', 4, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('HP TAN', 'In lỗ đơn lớp', 5, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('HP ZEN', 'In lỗ đa lớp', 6, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('INMACH', 'In mạch', 7, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('TAN', 'In mạch đơn lớp', 8, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('ZEN', 'In mạch đa lớp', 9, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('K', 'Gia áp', 10, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('GHEPLOP', 'Ghép lớp thường', 11, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('M', 'Ghép lớp thường đơn lớp', 12, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('M ALL', 'Ghép lớp thường tất cả các lớp', 13, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('M 熱圧着', 'Ghép lớp gia áp nhiệt', 14, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('TK', 'Tháo khung', 15, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('PET+', 'Dán PET', 16, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('PET-', 'Tháo PET', 17, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('SHN', 'Sấy hồng ngoại', 18, 'SYSTEM');
insert into process_group (process_statistic_code, description, sort_order, created_by) VALUES ('SNAP', 'Snap', 19, 'SYSTEM');
