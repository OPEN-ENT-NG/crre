ALTER TABLE IF EXISTS crre.basket_equipment RENAME TO basket_order_item;
ALTER TABLE IF EXISTS crre.basket_order_item
    RENAME COLUMN id_equipment TO id_item;
ALTER SEQUENCE IF EXISTS crre.basket_equipment_id_seq RENAME TO basket_order_item_id_seq;
