create sequence roles_role_id_seq;

create table roles
(
  role_id int default nextval('roles_role_id_seq'::regclass) not null
    constraint roles_pkey
    primary key,
  role_name varchar(50) not null,
  role_description varchar(100)
);

create unique index roles_role_name_uindex on roles (role_name);

insert into roles(role_name) values('ROLE_ADMIN');
insert into roles(role_name) values('ROLE_USER');

-----------------------------

create sequence users_user_id_seq;

create table users
(
  user_id bigint default nextval('users_user_id_seq'::regclass) not null
    constraint users_pkey
    primary key,
  username varchar(50) not null,
  password varchar(100) not null
);

create unique index users_username_uindex on users (username);

insert into users(username, password) values('admin', '$2a$10$T1SsiqMn4IHFlhGgJyJo9.JYssXKt3wYSauKY50HG/QwTB8BoBqgK');

-----------------------------

create table user_roles
(
  user_id bigint not null
    constraint user_roles_users_user_id_fk
    references users,
  role_id int not null
    constraint user_roles_roles_role_id_fk
    references roles,
  constraint user_roles_user_id_role_id_pk
  primary key (user_id, role_id)
);

insert into user_roles(user_id, role_id) values(1, 1);
insert into user_roles(user_id, role_id) values(1, 2);

-----------------------------

create sequence groups_group_id_seq;

create table groups
(
	group_id int default nextval('groups_group_id_seq'::regclass) not null
		constraint groups_pkey
			primary key,
	group_name varchar(50) not null,
	group_description varchar(100)
);

-----------------------------

create table group_members
(
	group_id int not null
		constraint group_members_groups_group_id_fk
			references groups,
	user_id bigint not null
		constraint group_members_users_user_id_fk
			references users,
	constraint group_members_group_id_user_id_pk
		primary key (group_id, user_id)
);

-----------------------------

create sequence permissions_permission_id_seq;

create table permissions
(
	permission_id int default nextval('permissions_permission_id_seq'::regclass) not null
		constraint permissions_pkey
			primary key,
	permission_name varchar(50) not null,
	permission_description varchar(100)
);

-----------------------------

create table role_permissions
(
	role_id int not null
		constraint role_permissions_roles_role_id_fk
			references roles,
	permission_id int not null
		constraint role_permissions_permissions_permission_id_fk
			references permissions,
	constraint role_permissions_role_id_permission_id_pk
		primary key (role_id, permission_id)
);

-----------------------------

create sequence functions_function_id_seq;

create table functions
(
	function_id int default nextval('functions_function_id_seq'::regclass) not null
		constraint functions_pkey
			primary key,
	function_name varchar(50) not null
);

-----------------------------

create table user_function_permissions
(
	user_id bigint not null
		constraint user_function_permissions_users_user_id_fk
			references users,
	function_id int not null
		constraint user_function_permissions_functions_function_id_fk
			references functions,
	permission_id int not null
		constraint user_function_permissions_permissions_permission_id_fk
			references permissions,
	constraint user_function_permissions_user_id_function_id_permission_id_pk
		primary key (user_id, function_id, permission_id)
);

-------------------------------------


