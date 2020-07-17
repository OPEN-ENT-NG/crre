ALTER TABLE lystore.order_client_options
ADD COLUMN id_type bigint NOT NULL DEFAULT 1;

ALTER TABLE lystore.order_client_equipment
ADD COLUMN id_type bigint NOT NULL DEFAULT 1;

ALTER TABLE lystore.basket_equipment
ADD COLUMN id_type bigint NOT NULL DEFAULT 1;



UPDATE lystore.order_client_options
SET    id_type = subquery.id_type
FROM   (SELECT id_type,
               NAME,
               price
        FROM   lystore.equipment) AS subquery
WHERE  subquery.price = order_client_options.price
       AND order_client_options.NAME = subquery.NAME ;


       UPDATE lystore.order_client_equipment
SET    id_type = subquery.id_type
FROM   (SELECT id_type,
               NAME,
               price
        FROM   lystore.equipment) AS subquery
WHERE  subquery.price = order_client_equipment.price
       AND order_client_equipment.NAME = subquery.NAME ;


       UPDATE lystore.basket_equipment
SET    id_type = subquery.id_type
FROM   (SELECT id_type,
               id
        FROM   lystore.equipment) AS subquery
WHERE basket_equipment.id_equipment = subquery.id ;