ALTER TABLE crre.rel_group_campaign
DROP CONSTRAINT fk_campaign_id; -- Suppression de la contrainte existante

ALTER TABLE crre.rel_group_campaign
    ADD CONSTRAINT fk_campaign_id
        FOREIGN KEY (id_campaign)
            REFERENCES crre.campaign (id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE; -- Ajout de la contrainte ON DELETE CASCADE

ALTER TABLE crre."order-region-equipment"
DROP CONSTRAINT fk_campaign_id; -- Suppression de la contrainte existante

ALTER TABLE crre."order-region-equipment"
    ADD CONSTRAINT fk_campaign_id
        FOREIGN KEY (id_campaign)
            REFERENCES crre.campaign (id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE; -- Ajout de la contrainte ON DELETE CASCADE

ALTER TABLE crre.order_client_equipment
DROP CONSTRAINT fk_campaign_id; -- Suppression de la contrainte existante

ALTER TABLE crre.order_client_equipment
    ADD CONSTRAINT fk_campaign_id
        FOREIGN KEY (id_campaign)
            REFERENCES crre.campaign (id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE; -- Ajout de la contrainte ON DELETE CASCADE

ALTER TABLE crre.basket_order
DROP CONSTRAINT fk_campaign_id; -- Suppression de la contrainte existante

ALTER TABLE crre.basket_order
    ADD CONSTRAINT fk_campaign_id
        FOREIGN KEY (id_campaign)
            REFERENCES crre.campaign (id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE; -- Ajout de la contrainte ON DELETE CASCADE

ALTER TABLE crre.basket_order_item
DROP CONSTRAINT fk_campaign_id; -- Suppression de la contrainte existante

ALTER TABLE crre.basket_order_item
    ADD CONSTRAINT fk_campaign_id
        FOREIGN KEY (id_campaign)
            REFERENCES crre.campaign (id)
            ON UPDATE NO ACTION
            ON DELETE CASCADE; -- Ajout de la contrainte ON DELETE CASCADE


