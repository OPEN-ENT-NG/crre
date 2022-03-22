TRUNCATE TABLE crre.type_campaign;
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure) VALUES (1, 'Type numérique', 'licences', false, 'Catalogue numérique', false, '[{"libelle":"Numérique"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure) VALUES (2, 'Type papier', 'licences', false, 'Catalogue papier', false, '[{"libelle":"Papier"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (3, 'Type consommables papier', 'consumable_licences', false, 'Catalogue papier consommable', false, '[{"libelle":"Papier"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (4, 'Type consommables numérique', 'consumable_licences', false, 'Catalogue numérique consommable', false, '[{"libelle":"Numérique"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (5, 'Type Pro papier', 'licences', false, 'Catalogue papier Pro', false, '[{"libelle":"Papier"},  {"libelle": "Établissements professionnels"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (6, 'Type Pro numérique', 'licences', false, 'Catalogue numérique Pro', false, '[{"libelle":"Numérique"}, {"libelle": "Établissements professionnels"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (7, 'Type LGT papier', 'licences', false, 'Catalogue papier LGT', false, '[{"libelle":"Papier"},  {"libelle": "Établissements généraux"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (8, 'Type LGT numérique', 'licences', false, 'Catalogue numérique LGT', false, '[{"libelle":"Numérique"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixte"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (9, 'Type Pro consommables papier', 'consumable_licences', false, 'Catalogue consommable Pro', false, '[{"libelle":"Papier"}, {"libelle":"Mixte"}, {"libelle": "Établissements professionnels"}]');
INSERT INTO crre.type_campaign(	id, name, credit, reassort, catalog, automatic_close, structure) VALUES (10, 'Type Pro consommables numérique', 'consumable_licences', false, 'Catalogue consommable Pro', false, '[{"libelle":"Numérique"}, {"libelle":"Mixte"}, {"libelle": "Établissements professionnels"}]');

INSERT INTO crre.status(id, name) VALUES (70, 'Livré');
INSERT INTO crre.status(id, name) VALUES (71, 'Mis à disposition par l''éditeur');
INSERT INTO crre.status(id, name) VALUES (72, 'Livré');
INSERT INTO crre.status(id, name) VALUES (52, 'En cours de vérification de l''offre');
INSERT INTO crre.status(id, name) VALUES (1000, 'Statut non reconnu');

ALTER TABLE crre.quote ALTER creation_date SET DEFAULT now();

CREATE OR REPLACE FUNCTION crre.update_old_end_date() RETURNS TRIGGER AS $$
BEGIN
    UPDATE crre.campaign
    SET accessible = false
    WHERE end_date <  NOW() AND automatic_close = true AND accessible = true;
    RETURN NEW ;
END;
$$ LANGUAGE 'plpgsql';

UPDATE crre.structure_group SET name = 'Numériques' WHERE name = 'Numérique';
UPDATE crre.structure_group SET name = 'Mixtes' WHERE name = 'Mixte';