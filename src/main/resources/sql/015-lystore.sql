ALTER TABLE lystore.project
DROP COLUMN name;

CREATE TABLE lystore.rel_title_campaign_structure (
  id_title bigint NOT NULL,
  id_campaign bigint NOT NULL,
  id_structure character varying (36),
  CONSTRAINT rel_title_campaign_structure_pkey PRIMARY KEY (id_title, id_campaign, id_structure),
  CONSTRAINT fk_title_id FOREIGN KEY (id_title) REFERENCES lystore.title (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_campaign_id FOREIGN KEY (id_campaign) REFERENCES lystore.campaign (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);