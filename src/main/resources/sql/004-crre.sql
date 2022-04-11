ALTER TABLE crre.logs ALTER date SET DEFAULT now();

TRUNCATE TABLE crre.type_campaign;
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure)
VALUES (1, 'Type numérique', 'licences', false, 'Catalogue numérique', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}]'),
       (2, 'Type papier', 'licences', false, 'Catalogue papier', false, '[{"libelle":"Papier"}, {"libelle":"Mixtes"}]'),
       (3, 'Type Pro numérique', 'licences', false, 'Catalogue numérique Pro', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements professionnels"}, {"libelle":"Mixtes"}]'),
       (4, 'Type LGT papier', 'licences', false, 'Catalogue papier LGT', false, '[{"libelle":"Papier"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}]'),
       (5, 'Type LGT numérique', 'licences', false, 'Catalogue numérique LGT', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}]'),
       (6, 'Type consommable pro numérique et papier', 'consumable_licences', false, 'Catalogue consommable Pro', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}, {"libelle": "Établissements professionnels"}]');