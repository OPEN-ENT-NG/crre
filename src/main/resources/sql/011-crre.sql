DROP TABLE crre.equipment CASCADE;
DROP TABLE crre.contract CASCADE;
DROP TABLE crre.rel_equipment_grade;
DROP TABLE crre.rel_equipment_subject;
DROP TABLE crre.tax;

ALTER TABLE crre.basket_equipment
ALTER COLUMN id_equipment TYPE VARCHAR;

ALTER TABLE crre."order-region-equipment"
ALTER COLUMN equipment_key TYPE VARCHAR;

ALTER TABLE crre.order_client_equipment
ALTER COLUMN equipment_key TYPE VARCHAR;