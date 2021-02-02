DROP TABLE crre.rel_equipment_collection;
DROP TABLE crre.collection;

DROP TABLE crre.contract_type CASCADE;
DROP TABLE crre.distributor CASCADE;
DROP TABLE crre.agent CASCADE;
DROP TABLE crre.equipment_option CASCADE;
DROP TABLE crre.supplier CASCADE;
DROP TABLE crre.basket_file;
DROP TABLE crre.basket_option;
DROP TABLE crre.order_client_options;

ALTER TABLE crre."order-region-equipment"
DROP COLUMN name,
DROP COLUMN summary,
DROP COLUMN description,
DROP COLUMN price,
DROP COLUMN image,
DROP COLUMN technical_spec,
DROP COLUMN id_contract,
DROP COLUMN number_validation,
DROP COLUMN id_order,
DROP COLUMN rank;

ALTER TABLE crre.order_client_equipment
DROP COLUMN name,
DROP COLUMN summary,
DROP COLUMN description,
DROP COLUMN price,
DROP COLUMN price_proposal,
DROP COLUMN tax_amount,
DROP COLUMN image,
DROP COLUMN technical_spec,
DROP COLUMN id_contract,
DROP COLUMN number_validation,
DROP COLUMN id_order,
DROP COLUMN id_project,
DROP COLUMN override_region,
DROP COLUMN id_type,
DROP COLUMN rank;

