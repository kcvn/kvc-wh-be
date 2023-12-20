-- Function: common_update_stamp
CREATE FUNCTION common_update_stamp() RETURNS trigger AS $common_update_stamp$
    BEGIN
        NEW.updated_date := current_timestamp;
        RETURN NEW;
    END;
$common_update_stamp$ LANGUAGE plpgsql;

-- Table: auth_user
CREATE TABLE auth_user
(
    id VARCHAR NOT NULL DEFAULT GEN_RANDOM_UUID(),
    username VARCHAR NOT NULL,
    password VARCHAR NOT NULL,
	employee_code VARCHAR,
	email VARCHAR,
    phone_number VARCHAR,
    full_name VARCHAR,
    full_name_unsigned VARCHAR,
    date_of_birth DATE,
    avatar VARCHAR,
    status SMALLINT NOT NULL DEFAULT 1,
    is_super_admin BOOLEAN NOT NULL DEFAULT false,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_user_pkey PRIMARY KEY (id),
    CONSTRAINT auth_user_uniq_username UNIQUE (username, is_deleted),
    CONSTRAINT auth_user_uniq_employee_code UNIQUE (employee_code, is_deleted),
    CONSTRAINT auth_user_uniq_email UNIQUE (email, is_deleted)
);

-- Trigger: auth_user_stamp
CREATE TRIGGER auth_user_stamp BEFORE UPDATE ON auth_user
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();

-- insert admin user, default password: admin
INSERT INTO auth_user(username, password, is_super_admin, created_by)
	VALUES ('admin', '$2a$10$T1SsiqMn4IHFlhGgJyJo9.JYssXKt3wYSauKY50HG/QwTB8BoBqgK', true, 'SYSTEM');

-- Table: auth_role
CREATE TABLE auth_role
(
    id VARCHAR NOT NULL DEFAULT GEN_RANDOM_UUID(),
    name VARCHAR NOT NULL,
    description VARCHAR,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_role_pkey PRIMARY KEY (id),
    CONSTRAINT auth_role_uniq_name UNIQUE (name, is_deleted)
);

-- Trigger: auth_role_stamp
CREATE TRIGGER auth_role_stamp BEFORE UPDATE ON auth_role
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();

---- Table: auth_permission
--CREATE TABLE auth_permission
--(
--    code VARCHAR NOT NULL,
--    name VARCHAR NOT NULL,
--    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    created_by VARCHAR NOT NULL,
--    updated_date TIMESTAMP WITH TIME ZONE,
--    updated_by VARCHAR,
--    is_deleted boolean NOT NULL DEFAULT false,
--    CONSTRAINT auth_permission_pkey PRIMARY KEY (code),
--    CONSTRAINT auth_permission_uniq_name UNIQUE (name, is_deleted)
--);
--
---- Trigger: auth_permission_stamp
--CREATE TRIGGER auth_permission_stamp BEFORE UPDATE ON auth_permission
--    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();
--
---- Insert permissions
--INSERT INTO auth_permission(code, name, created_by) VALUES ('u.v', 'VIEW_USER', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('u.c', 'CREATE_USER', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('u.u', 'UPDATE_USER', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('u.d', 'DELETE_USER', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('r.v', 'VIEW_ROLE', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('r.c', 'CREATE_ROLE', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('r.u', 'UPDATE_ROLE', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('r.d', 'DELETE_ROLE', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('l.v', 'VIEW_LOG', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('rp.aootm.v', 'VIEW_REPORT_AVERAGE_OUTPUT_OF_TWO_MONTHS', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('rp.kpd.v', 'VIEW_REPORT_KTTN_PRODUCT_DELIVERY', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('ip.i.v', 'VIEW_IMPORT_INVENTORY', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('ip.o.v', 'VIEW_ODER_QUANTITY', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('ip.p.v', 'VIEW_PASS_RATE', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('ip.r.v', 'VIEW_WORK_RESULT', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('m.p.i.v', 'VIEW_MANAGEMENT_PRODUCT_INFO', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('m.p.c.f.v', 'VIEW_MANAGEMENT_PRODUCT_CREATION_FLOW', 'SYSTEM');
--INSERT INTO auth_permission(code, name, created_by) VALUES ('pl.p.v', 'VIEW_PLAN_PROCESS', 'SYSTEM');

-- Table: auth_user_role
CREATE TABLE auth_user_role
(
    user_id VARCHAR NOT NULL,
    role_id VARCHAR NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_user_role_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT auth_user_role_auth_role_id_fk FOREIGN KEY (role_id)
            REFERENCES auth_role (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,
        CONSTRAINT auth_user_role_auth_user_id_fk FOREIGN KEY (user_id)
            REFERENCES auth_user (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- Trigger: auth_user_role_stamp
CREATE TRIGGER auth_user_role_stamp BEFORE UPDATE ON auth_user_role
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();

-- Table: auth_user_claim
CREATE TABLE auth_user_claim
(
    id VARCHAR NOT NULL DEFAULT GEN_RANDOM_UUID(),
    user_id VARCHAR NOT NULL,
    claim_type VARCHAR NOT NULL,
    claim_value VARCHAR,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_user_claim_pkey PRIMARY KEY (id),
    CONSTRAINT auth_user_claim_auth_user_id_fk FOREIGN KEY (user_id)
            REFERENCES auth_user (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- Trigger: auth_user_claim_stamp
CREATE TRIGGER auth_user_claim_stamp BEFORE UPDATE ON auth_user_claim
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();

-- Table: auth_role_claim
CREATE TABLE auth_role_claim
(
    id VARCHAR NOT NULL DEFAULT GEN_RANDOM_UUID(),
    role_id VARCHAR NOT NULL,
    claim_type VARCHAR NOT NULL,
    claim_value VARCHAR,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_role_claim_pkey PRIMARY KEY (id),
    CONSTRAINT auth_role_claim_auth_role_id_fk FOREIGN KEY (role_id)
            REFERENCES auth_role (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- Trigger: auth_role_claim_stamp
CREATE TRIGGER auth_role_claim_stamp BEFORE UPDATE ON auth_role_claim
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();

-- Table: auth_password_reset_token
CREATE TABLE auth_password_reset_token
(
    id VARCHAR NOT NULL DEFAULT GEN_RANDOM_UUID(),
    user_id VARCHAR NOT NULL,
    token VARCHAR NOT NULL,
    expired_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR NOT NULL,
    updated_date TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR,
    is_deleted boolean NOT NULL DEFAULT false,
    CONSTRAINT auth_password_reset_token_pkey PRIMARY KEY (id),
    CONSTRAINT auth_password_reset_token_auth_user_id_fk FOREIGN KEY (user_id)
            REFERENCES auth_user (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- Trigger: auth_password_reset_token_stamp
CREATE TRIGGER auth_password_reset_token_stamp BEFORE UPDATE ON auth_password_reset_token
    FOR EACH ROW EXECUTE FUNCTION common_update_stamp();