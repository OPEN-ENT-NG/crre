ALTER TABLE crre."order-region-equipment"
    ADD column id_offer_equipment character varying(100);

ALTER TABLE crre."order-region-equipment"
    ALTER column equipment_key DROP NOT NULL;

ALTER TABLE crre."order-region-equipment"
    ALTER column id_campaign DROP NOT NULL;

ALTER TABLE crre."order-region-equipment"
    ALTER column creation_date SET DEFAULT now();
