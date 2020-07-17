CREATE TABLE lystore.basket_file (
  id character varying (36) NOT NULL,
  id_basket_equipment bigint NOT NULL,
  filename character varying (255) NOT NULL,
  CONSTRAINT basket_file_pkey PRIMARY KEY (id, id_basket_equipment),
  CONSTRAINT fk_basket_equipment_id FOREIGN KEY (id_basket_equipment)
  REFERENCES lystore.basket_equipment (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE lystore.order_file (
  id character varying (36) NOT NULL,
  id_order_client_equipment bigint NOT NULL,
  filename character varying (255) NOT NULL,
  CONSTRAINT order_file_pkey PRIMARY KEY (id, id_order_client_equipment),
  CONSTRAINT fk_order_client_equipment_id FOREIGN KEY (id_order_client_equipment)
  REFERENCES lystore.order_client_equipment (id) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE CASCADE
);
ALTER TABLE lystore.BASKET_EQUIPMENT
  ADD COLUMN price_proposal numeric;

ALTER TABLE lystore.ORDER_CLIENT_EQUIPMENT
  ADD COLUMN price_proposal numeric;
