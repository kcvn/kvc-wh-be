CREATE TABLE public.common_category (
	id varchar(50) NOT NULL DEFAULT gen_random_uuid(),
	"type" varchar(100) NOT NULL,
	value varchar(50) NOT NULL,
	description varchar(100) NULL,
	sort_order int4 NOT NULL DEFAULT 1,
	created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by varchar(100) NULL,
	updated_date TIMESTAMP NULL,
	updated_by varchar(100) NULL,
	is_deleted bool NOT NULL DEFAULT false,
	CONSTRAINT common_category_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_unique_type_value ON public.common_category ("type",value);

INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('07b10496-1911-417b-9c6e-934b7f05021a', 'LOAI_XUAT_HANG', 'Shipback', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ccfaf3b1-9d1b-4a39-ba98-b7758034d123', 'LOAI_XUAT_HANG', 'Direct', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c1eba4db-c388-4516-98ce-44b0e1180f41', 'KHUNG_1', 'ML', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('194231f7-772b-4f41-ab8f-f5b509870287', 'KHUNG_1', 'MU', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('9ed7cb18-7815-4407-b9e5-878393dc153e', 'KHUNG_1', 'SWR', NULL, 3, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('a4aafc6b-3978-4f65-becb-eea42922c0b5', 'KHUNG_2', 'S', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d9e1ff7e-14de-4b0b-a3fe-37b045ca30dc', 'KHUNG_2', 'SU', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('7bb75a6b-728a-4537-b70a-36e889c538d5', 'KHUON_DUC', 'KVC', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('1d382ade-9024-4813-8769-aa655daae8d3', 'KHUON_DUC', 'ML', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('34e089b8-6a7d-43d8-ac00-a6331e89b3e6', 'KHUON_DUC', 'SKE', NULL, 3, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('9f96367b-a394-45ac-b340-01c2955b386a', 'KHUON_DUC', 'SWR', NULL, 4, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bf15a4cf-78a5-4f6e-82d6-617e5f232647', 'KHUON_DUC', 'SUR', NULL, 5, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('9e30b519-d040-4d79-9d04-8f8ef52ce3a8', 'SR_OR_NSR', 'SR', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('f529d403-bfc7-46d1-b628-d3599e2b43da', 'SR_OR_NSR', 'NSR', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6b22d0ef-fa1e-4c69-bf50-6d81529170e2', 'SR_OR_NSR', 'CSP', NULL, 3, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d3df114f-0c1b-4b31-8ac4-8f2a74f0c90b', 'RING_JIG', 'JIG', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bcd1283f-e02b-4187-bfc0-2e0ca1eb3479', 'RING_JIG', 'RING', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('332f4de1-9a92-47b3-aac7-83c24fba647b', 'LOAI_TAPE', 'A440', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b863f3b3-792e-4ef7-94ca-49d246d43838', 'LOAI_TAPE', 'A443', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('5271f9fd-9e1b-41ce-8cdf-2354546e7b30', 'LOAI_TAPE', 'AO700', NULL, 3, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c1b88432-8b17-4f5f-902e-c641d678f70c', 'LOAI_TAPE', 'AO700R', NULL, 4, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e5f115fb-90d7-4908-84cb-2df20d7cdd7c', 'LOAI_TAPE', 'H443K', NULL, 5, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e6c42c24-d09d-49e5-8225-20adf140a06a', 'LOAI_TAPE', 'H700R', NULL, 6, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('048ca4b9-9509-499a-bb38-52073db5f29e', 'TAPE_DUNG_CHUNG', 'A', NULL, 1, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bf3bb572-4779-4b8b-84c4-047d0dcda4f9', 'TAPE_DUNG_CHUNG', 'AA', NULL, 2, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('aa0e43f2-cbd2-47d7-97ee-2cb8805369ab', 'TAPE_DUNG_CHUNG', 'AB', NULL, 3, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('596b5c1f-8a6f-4273-8903-81df1ca70fe7', 'TAPE_DUNG_CHUNG', 'AE', NULL, 4, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6da71ffc-df10-4f33-ab16-49fdc1100818', 'TAPE_DUNG_CHUNG', 'AG', NULL, 5, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6f47124f-e081-42bd-a015-22111e358d3b', 'TAPE_DUNG_CHUNG', 'AH', NULL, 6, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('823e1b8c-980b-4faa-abbe-0c3320dd681d', 'TAPE_DUNG_CHUNG', 'AI', NULL, 7, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4b508b0c-0991-4704-bf6e-5cada736524a', 'TAPE_DUNG_CHUNG', 'AJ', NULL, 8, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('10d46ef2-dcbb-496e-814d-8c30fb74311b', 'TAPE_DUNG_CHUNG', 'AK', NULL, 9, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('fe1ce10d-8eb7-4e6d-9c0e-d590662b7742', 'TAPE_DUNG_CHUNG', 'AL', NULL, 10, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('cefe0489-5fbe-4dbe-8a41-339c64d9548c', 'TAPE_DUNG_CHUNG', 'AM', NULL, 11, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('487e6304-9669-4fbf-b9ae-d54a9b88b410', 'TAPE_DUNG_CHUNG', 'AN', NULL, 12, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c638ab13-22ab-4b4b-ba9e-2a3afcefbb05', 'TAPE_DUNG_CHUNG', 'AO', NULL, 13, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('2a5ed6c3-448c-4a1b-90a7-d8886a55ed61', 'TAPE_DUNG_CHUNG', 'AP', NULL, 14, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('042a346e-4931-4b50-881a-a998ec271a30', 'TAPE_DUNG_CHUNG', 'AQ', NULL, 15, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b03cde95-dc3b-45fb-a5be-eeb0380f1044', 'TAPE_DUNG_CHUNG', 'AR', NULL, 16, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('11b82170-f915-41d0-955d-9e6495a073a0', 'TAPE_DUNG_CHUNG', 'AS', NULL, 17, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('2bbfd536-cedf-486d-9104-4985795fc4c2', 'TAPE_DUNG_CHUNG', 'AT', NULL, 18, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('234a54e4-0356-4e5e-85fd-f1d47d4d2604', 'TAPE_DUNG_CHUNG', 'AU', NULL, 19, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e6f4d7dc-d99f-4101-8cbc-723b61003dd6', 'TAPE_DUNG_CHUNG', 'AV', NULL, 20, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('55f32fa1-b55d-421a-ba8b-1af5e5b295cf', 'TAPE_DUNG_CHUNG', 'AW', NULL, 21, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('fdf7f71e-ac3e-4921-af32-0ec1fd1cd020', 'TAPE_DUNG_CHUNG', 'AX', NULL, 22, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c0a38b5a-81ea-40a6-bd9e-c795ae488ee6', 'TAPE_DUNG_CHUNG', 'AY', NULL, 23, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('07deadf2-7e07-45f9-8c54-683bfb57994e', 'TAPE_DUNG_CHUNG', 'AZ', NULL, 24, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('1897832b-9c3e-415a-8c76-7def2388d31e', 'TAPE_DUNG_CHUNG', 'B', NULL, 25, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('922cfb81-d9eb-481b-96e3-73bdd4d9b69c', 'TAPE_DUNG_CHUNG', 'BA', NULL, 26, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('7f9a1deb-60f5-43e5-9c26-02f358adac42', 'TAPE_DUNG_CHUNG', 'BB', NULL, 27, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d42e5831-a1e9-4d4a-9d38-a99025dd1523', 'TAPE_DUNG_CHUNG', 'BC', NULL, 28, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('efd514c8-016d-4991-88a9-49cf391aab2f', 'TAPE_DUNG_CHUNG', 'BD', NULL, 29, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('f2a265db-c2af-40ae-9a43-f72edafe412b', 'TAPE_DUNG_CHUNG', 'BE', NULL, 30, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('f3ec7b64-68db-468e-adcc-36c10b987c05', 'TAPE_DUNG_CHUNG', 'BF', NULL, 31, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('616eb269-d6a3-4b41-a0af-4369a73e4c1f', 'TAPE_DUNG_CHUNG', 'BG', NULL, 32, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4102c2fb-252b-4f6d-8614-2eaaa36a24c0', 'TAPE_DUNG_CHUNG', 'BH', NULL, 33, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('26e21441-95ff-48f2-a4f4-1663c6229bfc', 'TAPE_DUNG_CHUNG', 'BI', NULL, 34, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('12e9e89d-056f-4510-b439-6068bf673797', 'TAPE_DUNG_CHUNG', 'BJ', NULL, 35, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('3ffe11e9-ffa7-491f-b0e9-a8ffce3dcf41', 'TAPE_DUNG_CHUNG', 'BK', NULL, 36, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4ae9ed07-2a0c-461f-ae1b-3b072c7909c9', 'TAPE_DUNG_CHUNG', 'BL', NULL, 37, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('024e03ef-93ed-4789-905d-fb76fd389676', 'TAPE_DUNG_CHUNG', 'BM', NULL, 38, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('2a6827ef-c947-48a8-8993-d569386d290b', 'TAPE_DUNG_CHUNG', 'BN', NULL, 39, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('648c7209-8cf5-4500-a91d-61b5b2d067a1', 'TAPE_DUNG_CHUNG', 'BO', NULL, 40, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6310e82e-e206-4a2b-bfbd-ee0191164659', 'TAPE_DUNG_CHUNG', 'BP', NULL, 41, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d7d98380-00d6-40dc-95b3-14318e4acd23', 'TAPE_DUNG_CHUNG', 'BQ', NULL, 42, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('87e6dfa0-e062-4b99-a321-fa7bbfc943f1', 'TAPE_DUNG_CHUNG', 'BR', NULL, 43, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6f75ff45-e472-4421-9fee-d84c69607865', 'TAPE_DUNG_CHUNG', 'BS', NULL, 44, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ef06e233-e3c3-44a3-be28-d651ac001f19', 'TAPE_DUNG_CHUNG', 'BT', NULL, 45, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('0702947e-1467-49ac-9044-2a2e490f95ae', 'TAPE_DUNG_CHUNG', 'BU', NULL, 46, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e0657591-93a1-4613-9d98-7cfd2d9450d6', 'TAPE_DUNG_CHUNG', 'BV', NULL, 47, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('a2666bac-b489-46ea-8ade-0374e2817af9', 'TAPE_DUNG_CHUNG', 'BW', NULL, 48, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('155c0192-09c2-4b5d-bf93-4f7d9304d3f5', 'TAPE_DUNG_CHUNG', 'BY', NULL, 49, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('65bb2252-eeba-4842-9aa3-481178775dc1', 'TAPE_DUNG_CHUNG', 'BZ', NULL, 50, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('32011753-31d7-4488-bb3b-99fce76b0972', 'TAPE_DUNG_CHUNG', 'C', NULL, 51, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('9a2a61ad-7489-4333-8858-ea9392566b97', 'TAPE_DUNG_CHUNG', 'CA', NULL, 52, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('870cccb4-87a0-4202-94ae-cb260a21ccd9', 'TAPE_DUNG_CHUNG', 'CB', NULL, 53, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('53d7ce32-3702-43d4-b756-d2e3af350c8f', 'TAPE_DUNG_CHUNG', 'CC', NULL, 54, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4d636d58-68e6-4e9a-a278-40b76e3d76df', 'TAPE_DUNG_CHUNG', 'CD', NULL, 55, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b6d1a377-e86b-4bc4-9cd5-2e1663ca6998', 'TAPE_DUNG_CHUNG', 'CE', NULL, 56, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('32e3b09d-bbb6-47f0-811b-7dfc0ad07892', 'TAPE_DUNG_CHUNG', 'CF', NULL, 57, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('1c74884e-c0b4-45ca-9bf0-1dac8aec9efa', 'TAPE_DUNG_CHUNG', 'CG', NULL, 58, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('a4f2aa32-10e7-406e-b507-ce1e50522423', 'TAPE_DUNG_CHUNG', 'CL', NULL, 59, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('76ee42d5-2d3e-481a-99df-2fce7ce75b51', 'TAPE_DUNG_CHUNG', 'CM', NULL, 60, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c241cd50-e586-4f54-8723-166dad514067', 'TAPE_DUNG_CHUNG', 'CN', NULL, 61, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('55ad41d4-6312-40d3-a6c2-61a98be0cf54', 'TAPE_DUNG_CHUNG', 'CO', NULL, 62, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('280a2cf7-988b-48aa-aa25-d94ad1e107dd', 'TAPE_DUNG_CHUNG', 'CQ1', NULL, 63, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('8a895416-6e0d-4916-8f47-40facc796264', 'TAPE_DUNG_CHUNG', 'CR', NULL, 64, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('a602b80b-39f1-4205-bf57-4bad21079980', 'TAPE_DUNG_CHUNG', 'CS', NULL, 65, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('3ca46760-154a-43b0-8d0a-66e3d0dc65fa', 'TAPE_DUNG_CHUNG', 'CT', NULL, 66, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('09614896-2729-4f1a-bb9d-27ea412c99b9', 'TAPE_DUNG_CHUNG', 'CU', NULL, 67, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4870327c-3244-4b3f-a27a-406ecc61995b', 'TAPE_DUNG_CHUNG', 'CV', NULL, 68, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('2857998d-f681-4aa4-865e-c98d282db999', 'TAPE_DUNG_CHUNG', 'CW', NULL, 69, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('f3eb9503-326c-45de-96d8-cd4f19bc79af', 'TAPE_DUNG_CHUNG', 'CX', NULL, 70, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6b256a06-88f3-4788-86b6-b6a4a6f40cba', 'TAPE_DUNG_CHUNG', 'CY', NULL, 71, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e296552d-762c-4cf5-91c3-4a5a7a037d5c', 'TAPE_DUNG_CHUNG', 'CZ', NULL, 72, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('89ef0933-6c52-4914-b6a6-80e3595b7db4', 'TAPE_DUNG_CHUNG', 'DA', NULL, 73, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('cb896430-d1c1-4b86-8bb3-979ffa2ba1d8', 'TAPE_DUNG_CHUNG', 'DB', NULL, 74, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('504de179-6b0a-47e3-9ac7-ce15f15bcf41', 'TAPE_DUNG_CHUNG', 'DC', NULL, 75, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('0df73c69-08c6-4898-b929-b5236e1efa5d', 'TAPE_DUNG_CHUNG', 'DD', NULL, 76, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b06e2163-420d-4bf0-9c1d-173c7a0938e1', 'TAPE_DUNG_CHUNG', 'DE', NULL, 77, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('500e3797-4ea6-45cb-afca-05c3b172a38b', 'TAPE_DUNG_CHUNG', 'DF', NULL, 78, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('a45744ba-41b1-4437-8ac2-8494b2ef03ca', 'TAPE_DUNG_CHUNG', 'DG', NULL, 79, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('8c8f7b99-2480-41a1-a0c3-048ef463cef0', 'TAPE_DUNG_CHUNG', 'DH', NULL, 80, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('1ae6c62f-913e-474d-b8ab-ac2b5b506844', 'TAPE_DUNG_CHUNG', 'DI', NULL, 81, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ca5b2e7b-f5a6-4ef5-bf14-893fd0e5de6a', 'TAPE_DUNG_CHUNG', 'DJ', NULL, 82, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('5ac9096a-4b91-42b6-9a9e-fa82810288c5', 'TAPE_DUNG_CHUNG', 'DK', NULL, 83, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('12e7d5e1-b0fc-4049-8da2-fe8c6f76fc1d', 'TAPE_DUNG_CHUNG', 'DL', NULL, 84, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ca1507ae-1ef6-48fa-b806-f51f0b6f2143', 'TAPE_DUNG_CHUNG', 'DM', NULL, 85, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('236cd1a2-272f-4480-92ee-ce2e00bda28e', 'TAPE_DUNG_CHUNG', 'DN', NULL, 86, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c7abd215-d292-4ce6-a3f2-cb133420fe21', 'TAPE_DUNG_CHUNG', 'DO', NULL, 87, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('00f7f0d0-124a-4a26-bf2a-d325eeb03002', 'TAPE_DUNG_CHUNG', 'DP', NULL, 88, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bda7d5b8-8726-4778-bd66-df2784ecd7a2', 'TAPE_DUNG_CHUNG', 'DQ', NULL, 89, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('0248e973-da3b-4088-93d5-0e28c3a42a89', 'TAPE_DUNG_CHUNG', 'DR', NULL, 90, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('6a1e2e14-9d30-48fa-9a96-23e401346b8d', 'TAPE_DUNG_CHUNG', 'DS', NULL, 91, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('426fb2b5-f595-4890-b762-940bb00d654b', 'TAPE_DUNG_CHUNG', 'DT', NULL, 92, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('8daedfa2-9260-452a-b874-61439f765c9f', 'TAPE_DUNG_CHUNG', 'DU', NULL, 93, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('50b8beb1-2d3f-4ed9-ab64-55e93ae5bdf6', 'TAPE_DUNG_CHUNG', 'DV', NULL, 94, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('158cc022-0a9d-44ef-abef-34faa18f736f', 'TAPE_DUNG_CHUNG', 'DW', NULL, 95, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b0a8f280-e65d-44e6-9e28-b9372bb45110', 'TAPE_DUNG_CHUNG', 'DX', NULL, 96, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('1dfb46a6-3102-4fe9-b43b-714a4adb361a', 'TAPE_DUNG_CHUNG', 'DY', NULL, 97, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('f895efc3-b7b9-465e-bee1-328154a5d711', 'TAPE_DUNG_CHUNG', 'DZ', NULL, 98, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('7526790e-5211-4145-95a3-200a21999146', 'TAPE_DUNG_CHUNG', 'EA', NULL, 99, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d2ce0579-d509-459a-b1a4-029f2f91a4dc', 'TAPE_DUNG_CHUNG', 'EB', NULL, 100, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('69f8edd6-0884-4570-b987-b7e69cc66207', 'TAPE_DUNG_CHUNG', 'EC', NULL, 101, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('789eb4b6-1407-4a59-9808-f3aec4da2840', 'TAPE_DUNG_CHUNG', 'ED', NULL, 102, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('16772d32-42fc-4504-84ba-e59b0004eebb', 'TAPE_DUNG_CHUNG', 'EF', NULL, 103, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ab5eb779-351d-47ce-bbb9-a69a2de9f07c', 'TAPE_DUNG_CHUNG', 'EG', NULL, 104, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('05553107-1537-4feb-877d-d854a1cca97e', 'TAPE_DUNG_CHUNG', 'EH', NULL, 105, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ccb84644-23b9-4cbb-994a-f8bf25b2b0fe', 'TAPE_DUNG_CHUNG', 'EI', NULL, 106, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('d351a35a-5e1a-4988-b40c-0a56c50c3213', 'TAPE_DUNG_CHUNG', 'EJ', NULL, 107, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4208d5fa-b7c4-44c2-831f-e9619e6bd0c2', 'TAPE_DUNG_CHUNG', 'EK', NULL, 108, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c7c50eb4-a509-4921-9879-97f2153f7489', 'TAPE_DUNG_CHUNG', 'EL', NULL, 109, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c59de07a-55a0-4925-ad30-2823f97cb75a', 'TAPE_DUNG_CHUNG', 'EM', NULL, 110, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bd87bd62-e660-430e-b21c-0b53cefeef4c', 'TAPE_DUNG_CHUNG', 'EN', NULL, 111, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('0c449bdb-a0b4-4bbd-81d8-9d0f82faef97', 'TAPE_DUNG_CHUNG', 'EO', NULL, 112, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ce401fee-505e-47a0-82d3-e307221126ee', 'TAPE_DUNG_CHUNG', 'EP', NULL, 113, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('5ac31808-4c0a-4653-85df-75b967faf4be', 'TAPE_DUNG_CHUNG', 'EQ', NULL, 114, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('9326beac-21b4-42ef-8569-672910466079', 'TAPE_DUNG_CHUNG', 'ES', NULL, 115, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('b69de6da-95a7-4e14-997e-761c94a6dd04', 'TAPE_DUNG_CHUNG', 'J', NULL, 116, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('c8fc6f14-9337-4280-b8dd-ad1da627c05b', 'TAPE_DUNG_CHUNG', 'L', NULL, 117, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('985a96fe-3f55-4666-90ee-24d26d1b6d92', 'TAPE_DUNG_CHUNG', 'M', NULL, 118, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('7d0dad87-fe3f-464b-89b4-15c2e615454f', 'TAPE_DUNG_CHUNG', 'Q', NULL, 119, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('4986dc2a-f248-4fd1-8ef9-969c179b5c28', 'TAPE_DUNG_CHUNG', 'R', NULL, 120, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('18052b26-5751-469c-a976-1893448ae34b', 'TAPE_DUNG_CHUNG', 'S', NULL, 121, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('ea70ab8a-fca6-4268-b0c8-96bb43304c2e', 'TAPE_DUNG_CHUNG', 'T', NULL, 122, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('2482a673-9abc-47e4-82fb-07f8cdf1ec98', 'TAPE_DUNG_CHUNG', 'U', NULL, 123, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('e9619528-5e21-4ac8-92f8-e44866a52089', 'TAPE_DUNG_CHUNG', 'V', NULL, 124, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('bf6e11cd-8205-4e97-99fe-8c70e463f91a', 'TAPE_DUNG_CHUNG', 'X', NULL, 125, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);
INSERT INTO common_category
(id, "type", value, description, sort_order, created_date, created_by, updated_date, updated_by, is_deleted)
VALUES('714ab282-736c-4735-84bc-debda4332fc4', 'TAPE_DUNG_CHUNG', 'Y', NULL, 126, '2024-01-24 16:34:10.697', 'SYSTEM', NULL, NULL, false);