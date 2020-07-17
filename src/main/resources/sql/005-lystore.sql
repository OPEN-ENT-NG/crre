-- Insert options into equipment
INSERT INTO lystore.equipment (name, price, id_tax, id_contract, status)
SELECT name, price, id_tax, (SELECT id FROM lystore.contract LIMIT 1) as id_contract, 'AVAILABLE' as status
FROM lystore.equipment_option;

-- Alter options table. Add id_option column
ALTER TABLE lystore.equipment_option
ADD COLUMN id_option bigint;

-- Link options to equipment
UPDATE lystore.equipment_option
SET id_option = equipment.id
FROM lystore.equipment
WHERE equipment_option.name = equipment.name
AND equipment_option.price = equipment.price;

-- Alter option table. Set id_option as NOT NULL and add foreign key to equipment
ALTER TABLE lystore.equipment_option
ALTER COLUMN id_option SET NOT NULL;

ALTER TABLE lystore.equipment_option
ADD CONSTRAINT fk_equipment_option_id FOREIGN KEY (id_option)
    REFERENCES lystore.equipment (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE;

-- Alter option table. Delete name, price and id_tax columns
ALTER TABLE lystore.equipment_option
DROP COLUMN name;
ALTER TABLE lystore.equipment_option
DROP COLUMN price;
ALTER TABLE lystore.equipment_option
DROP COLUMN id_tax;

-- Alter table equipment. Add warranty, reference and catalog_enabled columns
ALTER TABLE lystore.equipment
ADD COLUMN reference character varying(255);

ALTER TABLE lystore.equipment
ADD COLUMN warranty bigint;

ALTER TABLE lystore.equipment
ADD COLUMN catalog_enabled boolean NOT NULL DEFAULT true;

-- Alter table equipment, set all warranties to 2 and add NOT NULL constraint
UPDATE lystore.equipment
SET warranty = 1;

ALTER TABLE lystore.equipment
ALTER COLUMN warranty SET NOT NULL;

