ALTER TABLE crre.logs ALTER date SET DEFAULT now();

TRUNCATE TABLE crre.type_campaign;
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure)
VALUES (1, 'Type numérique', 'licences', false, 'Catalogue numérique', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}]'),
       (2, 'Type papier', 'licences', false, 'Catalogue papier', false, '[{"libelle":"Papier"}, {"libelle":"Mixtes"}]'),
       (3, 'Type Pro numérique', 'licences', false, 'Catalogue numérique Pro', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements professionnels"}, {"libelle":"Mixtes"}]'),
       (4, 'Type LGT papier', 'licences', false, 'Catalogue papier LGT', false, '[{"libelle":"Papier"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}]'),
       (5, 'Type LGT numérique', 'licences', false, 'Catalogue numérique LGT', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}]'),
       (6, 'Type consommable pro numérique et papier', 'consumable_licences', false, 'Catalogue consommable Pro', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}, {"libelle": "Établissements professionnels"}]');

TRUNCATE TABLE crre.structure_group CASCADE;
INSERT INTO crre.structure_group VALUES (1, 'Établissements professionnels papier', 'Regroupement des lycées professionnels et papier');
INSERT INTO crre.structure_group VALUES (2, 'Établissements généraux papier', 'Regroupement des lycées généraux et papier');
INSERT INTO crre.structure_group VALUES (3, 'Établissements polyvalents papier', 'Regroupement des lycées polyvalent et papier');
INSERT INTO crre.structure_group VALUES (4, 'Établissements professionnels numériques', 'Regroupement des lycées professionnels numérique');
INSERT INTO crre.structure_group VALUES (5, 'Établissements généraux numériques', 'Regroupement des lycées généraux et numérique');
INSERT INTO crre.structure_group VALUES (6, 'Établissements polyvalents numériques', 'Regroupement des lycées polyvalent et numérique');
INSERT INTO crre.structure_group VALUES (7, 'Établissements professionnels mixtes', 'Regroupement des lycées professionnels mixte');
INSERT INTO crre.structure_group VALUES (8, 'Établissements généraux mixtes', 'Regroupement des lycées généraux mixte');
INSERT INTO crre.structure_group VALUES (9, 'Établissements polyvalents mixtes', 'Regroupement des lycées polyvalent mixte');
