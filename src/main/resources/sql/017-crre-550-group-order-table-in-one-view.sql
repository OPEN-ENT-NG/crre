CREATE OR REPLACE VIEW crre.order_universal as
(
SELECT oce.amount            as amount,
       oce.creation_date     as prescriber_validation_date,
       oce.id_campaign       as id_campaign,
       oce.id_structure      as id_structure,
       oce.status            as status,
       oce.equipment_key     as equipment_key,
       oce.cause_status      as cause_status,
       oce.comment           as comment,
       oce.user_id           as prescriber_id,
       oce.id_basket         as id_basket,
       oce.reassort          as reassort,
       ore.owner_id          as validator_id,
       ore.owner_name        as validator_name,
       ore.creation_date     as validator_validation_date,
       ore.modification_date as modification_date,
       ore.id_project        as id_project,

       NULL                  as equipment_name,
       NULL                  as equipment_image,
       NULL                  as equipment_price,
       NULL                  as equipment_grade,
       NULL                  as equipment_editor,
       NULL                  as equipment_diffusor,
       NULL                  as equipment_format,
       NULL                  as equipment_tva5,
       NULL                  as equipment_tva20,
       NULL                  as equipment_priceht,
       NULL                  as offers,
       NULL                  as total_free,

       oce.id                as order_client_id,
       ore.id                as order_region_id

FROM crre.order_client_equipment as oce
         LEFT JOIN crre."order-region-equipment" as ore on oce.id = ore.id_order_client_equipment
         LEFT JOIN crre."order-region-equipment" as "o-r-e" on oce.id = "o-r-e".id_order_client_equipment
UNION ALL
SELECT oceo.amount            as amount,
       oceo.creation_date     as prescriber_validation_date,
       oceo.id_campaign       as id_campaign,
       oceo.id_structure      as id_structure,
       oceo.status            as status,
       oceo.equipment_key     as equipment_key,
       oceo.cause_status      as cause_status,
       oceo.comment           as comment,
       oceo.user_id           as prescriber_id,
       oceo.id_basket         as id_basket,
       oceo.reassort          as reassort,
       oreo.owner_id          as validator_id,
       oreo.owner_name        as validator_name,
       oreo.creation_date     as validator_validation_date,
       oreo.modification_date as modification_date,
       oreo.id_project        as id_project,

       oceo.equipment_name,
       oceo.equipment_image,
       oceo.equipment_price,
       oceo.equipment_grade,
       oceo.equipment_editor,
       oceo.equipment_diffusor,
       oceo.equipment_format,
       oceo.equipment_tva5,
       oceo.equipment_tva20,
       oceo.equipment_priceht,
       oceo.offers,
       oreo.total_free,

       oceo.id                as order_client_id,
       oreo.id                as order_region_id
FROM crre.order_client_equipment_old as oceo
         LEFT JOIN crre."order-region-equipment-old" as oreo on oceo.id = oreo.id_order_client_equipment
    );
