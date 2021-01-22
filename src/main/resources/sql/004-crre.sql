ALTER TABLE crre.basket_equipment
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;

ALTER TABLE crre.order_client_equipment
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;

ALTER TABLE crre.order_region_equipment
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;