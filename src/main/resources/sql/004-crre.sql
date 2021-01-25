ALTER TABLE crre.basket_equipment
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;

ALTER TABLE crre.order_client_equipment
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;

ALTER TABLE crre."order-region-equipment"
    ADD COLUMN reassort boolean NOT NULL DEFAULT false;