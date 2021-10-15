ALTER TABLE crre.licences
    ADD COLUMN consumable_amount bigint,
    ADD COLUMN consumable_initial_amount bigint;

CREATE TABLE crre.consumable_formation (
     id bigint NOT NULL,
     label character varying NOT NULL
);

INSERT INTO crre.consumable_formation VALUES (1, 'BAC PRO 3 ANS : 2NDE PRO (OU 1ERE ANNEE)');
INSERT INTO crre.consumable_formation VALUES (2, 'BAC PRO 3 ANS : 1ERE PRO (OU 2EME ANNEE)');
INSERT INTO crre.consumable_formation VALUES (3, 'BAC PRO 3 ANS : TERM PRO (OU 3EME ANNEE)');
INSERT INTO crre.consumable_formation VALUES (4, 'CAP EN 2 ANS : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (5, 'CAP EN 2 ANS : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (6, 'CAP EN 1 AN');
INSERT INTO crre.consumable_formation VALUES (7, 'CAP EN 3 ANS : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (8, 'CAP EN 3 ANS : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (9, 'CAP EN 3 ANS : 3EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (10, 'BMA EN 2 ANS : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (11, 'BMA EN 2 ANS : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (12, 'BREVET PROFESSIONNEL : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (13, 'BREVET PROFESSIONNEL : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (14, 'BREVET DE TECH.SUP.EN 3 ANS : 1ERE ANN.');
INSERT INTO crre.consumable_formation VALUES (15, 'BREVET DE TECH.SUP.EN 3 ANS : 2EME ANN.');
INSERT INTO crre.consumable_formation VALUES (16, 'BREVET DE TECH.SUP.EN 3 ANS : 3EME ANN.');
INSERT INTO crre.consumable_formation VALUES (17, 'BREVET DE TECH.SUP.EN 2 ANS : 1ERE ANN.');
INSERT INTO crre.consumable_formation VALUES (18, 'BREVET DE TECH.SUP.EN 2 ANS : 2EME ANN.');
INSERT INTO crre.consumable_formation VALUES (19, 'BREVET DE TECHNICIEN SUPERIEUR EN 1 AN');
INSERT INTO crre.consumable_formation VALUES (20, 'FCIL & FC NON DIPLOMANTE POST NIVEAU-3');
INSERT INTO crre.consumable_formation VALUES (21, 'FCIL & FC NON DIPLOMANTE POST NIVEAU-4');
INSERT INTO crre.consumable_formation VALUES (22, 'FCIL & FC NON DIPLOMANTE POST NIVEAU-5');
INSERT INTO crre.consumable_formation VALUES (23, 'MENTION COMPLEMENTAIRE');
INSERT INTO crre.consumable_formation VALUES (24, 'PREPA.DIVERSE PRE-BAC : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (25, 'PREPA.DIVERSE PRE-BAC : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (26, 'PREPA.DIVERSE POST-BAC : 1ERE ANNEE');
INSERT INTO crre.consumable_formation VALUES (27, 'PREPA.DIVERSE POST-BAC : 2EME ANNEE');
INSERT INTO crre.consumable_formation VALUES (28, 'PREPA.DIVERSE POST-BAC : 3EME ANNEE');