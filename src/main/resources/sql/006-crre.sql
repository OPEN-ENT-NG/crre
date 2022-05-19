TRUNCATE TABLE crre.type_campaign;
INSERT INTO crre.type_campaign (id, name, credit, reassort, catalog, automatic_close, structure)
VALUES (1, 'Type numérique', 'credits', false, 'Catalogue numérique', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}]'),
       (2, 'Type papier', 'credits', false, 'Catalogue papier', false, '[{"libelle":"Papier"}, {"libelle":"Mixtes"}]'),
       (3, 'Type Pro numérique', 'credits', false, 'Catalogue numérique Pro', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements professionnels"}, {"libelle":"Mixtes"}, {"libelle":"Établissements polyvalents"}]'),
       (4, 'Type LGT papier', 'credits', false, 'Catalogue papier LGT', false, '[{"libelle":"Papier"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}, {"libelle":"Établissements polyvalents"}]'),
       (5, 'Type LGT numérique', 'credits', false, 'Catalogue numérique LGT', false, '[{"libelle":"Numériques"}, {"libelle": "Établissements généraux"}, {"libelle":"Mixtes"}, {"libelle":"Établissements polyvalents"}]'),
       (6, 'Type consommable pro numérique et papier', 'credits', false, 'Catalogue consommable Pro', false, '[{"libelle":"Numériques"}, {"libelle":"Mixtes"}, {"libelle": "Établissements professionnels"}, {"libelle":"Établissements polyvalents"}]');