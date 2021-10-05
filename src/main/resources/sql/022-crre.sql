CREATE TABLE crre.status
(
    id bigint NOT NULL,
    name character varying,
    PRIMARY KEY (id)
);

INSERT INTO crre.status(id, name) VALUES (0, 'En cours de commande');
INSERT INTO crre.status(id, name) VALUES (1, 'Disponible');
INSERT INTO crre.status(id, name) VALUES (2, 'À paraître');
INSERT INTO crre.status(id, name) VALUES (3, 'En cours de réimpression');
INSERT INTO crre.status(id, name) VALUES (4, 'Non disponible provisoirement');
INSERT INTO crre.status(id, name) VALUES (6, 'Epuisé');
INSERT INTO crre.status(id, name) VALUES (7, 'Manque sans date');
INSERT INTO crre.status(id, name) VALUES (9, 'Non disponible à long terme');
INSERT INTO crre.status(id, name) VALUES (10, 'En cours de livraison');
INSERT INTO crre.status(id, name) VALUES (14, 'Réclamation en cours auprès de l''éditeur');
INSERT INTO crre.status(id, name) VALUES (15, 'Annulé par le client');
INSERT INTO crre.status(id, name) VALUES (20, 'Annulé par la LDE (pas de date de dispo.)');
INSERT INTO crre.status(id, name) VALUES (35, 'Relance éditeur en cours');
INSERT INTO crre.status(id, name) VALUES (55, 'En cours de commande à l''éditeur');
INSERT INTO crre.status(id, name) VALUES (57, 'En cours de commande à l''éditeur');
INSERT INTO crre.status(id, name) VALUES (58, 'Licence offerte à l''achat des licences élèves');
INSERT INTO crre.status(id, name) VALUES (59, 'Les CGV doivent être validées');

ALTER TABLE crre."order-region-equipment-old"
    ADD COLUMN id_status bigint;
ALTER TABLE crre."order-region-equipment-old"
    ADD FOREIGN KEY (id_status) REFERENCES crre.status(id);

