-- create specific structures
CREATE TABLE lystore.specific_structures (
    id character varying(36),
    uai character varying(50) NOT NULL,
    type character varying (3) NOT NULL,
    CONSTRAINT specific_structures_pkey PRIMARY KEY (id),
    CONSTRAINT "specific_structure_type_values" CHECK (type IN ('CMD', 'CMR'))
);

INSERT INTO lystore.specific_structures (id, uai, type) VALUES
('c6f3da5a-4c99-48a6-add9-559fde1a0130', '0783548H', 'CMD'),
('267437cf-954b-470a-9529-6ed638ca0dc6', '0783549J', 'CMD'),
('34d7b711-894f-416f-8180-38bb65989f70','0750704H', 'CMR'),
('8a7fcc7e-1a21-47c6-9f6b-93fc560a7203','0930123D', 'CMR'),
('e1ea2b15-a051-41e0-a032-3ce1220ee244','0920146J', 'CMR'),
('6d3c254c-4609-4ab2-976c-095476533a24','0750703G', 'CMR'),
('75bfcaf4-0f2f-4cfd-a380-19d9824edd62','0750648X', 'CMR'),
('3fb00c41-cdc1-4a97-957d-58a5185f6454','0750700D', 'CMR'),
('14ae8de6-be8f-4630-95f6-2da38eff7162','0750702F', 'CMR'),
('489190b8-7bc1-4730-bcec-fc980e1ed2b1','0920798T', 'CMR'),
('3ca9db74-2935-4181-bec4-5d3f76d468a2','0930121B', 'CMR'),
('e57722e2-fef0-4a28-942d-6f9256a6b21f','0750657G', 'CMR'),
('fdad3421-5512-4158-8ca8-02dd69cb975f','0750656F', 'CMR'),
('320834e4-3973-4e73-a3ce-06e4b8cca1f4','0782562L', 'CMR'),
('006cdc61-98f9-4369-a3ad-649a31634a98','0940117S', 'CMR'),
('bc3f03fc-8f86-402f-b7c3-79784569c514','0940743X', 'CMR'),
('9c788eef-87a9-46f7-a8b0-a1247469bc16','0930118Y', 'CMR'),
('3a3c85b8-9f62-430b-80e8-90c0b44d92eb','0940124Z', 'CMR'),
('8ccd1d64-7aac-4176-be7e-440b44c40761','0930116W', 'CMR'),
('b89b32f3-e9aa-4c2b-a617-77218c8d91e3','0750670W', 'CMR'),
('9b504636-beec-49d6-997e-a2736005be6e','0750663N', 'CMR'),
('58cd419a-bd39-4995-bb1d-03e3e97e2b0f','0782546U', 'CMR'),
('2811fe10-e0b6-4a95-8d3a-04d0b98f3a7f','0750662M', 'CMR'),
('b9db832f-ba23-4d98-8fb5-c83271582589','0750689S', 'CMR'),
('54a0fba4-3e9c-42ad-a59d-ccab07d8eb27','0750652B', 'CMR'),
('e50e20b9-c246-4b87-944c-3a5bd2adf256','0750682J', 'CMR'),
('bc05bd08-e2e2-44e6-9335-26014ac6c6e6','0750690T', 'CMR'),
('fad2c06a-a306-4c4e-a024-ad0097899b6c','0750698B', 'CMR'),
('f54bd00d-8988-4a00-a612-8a744f08eb99','0750654D', 'CMR'),
('0f1780e0-b539-4073-8ddd-4e4e5822c222','0750705J', 'CMR'),
('5c4e2673-655f-4456-af50-ab0383680054','0750683K', 'CMR'),
('4001a6d7-2283-4437-9aa5-735b5e311b0b','0750684L', 'CMR'),
('dd9ba1e0-a277-4791-921b-764e9de8ef8c','0750694X', 'CMR'),
('ccd0e452-217d-4d8c-a2bf-248e160c6b4e','0920142E', 'CMR'),
('50591e16-db7a-48f8-86bd-11abef710e05','0750693W', 'CMR'),
('b33d80c9-09a7-4cc9-9cb5-820dce859d7f','0920145H', 'CMR'),
('7b23c92f-032b-4601-92c6-9618bee023a6','0750675B', 'CMR'),
('c34ec4bf-53f9-483f-a384-dd43fe7c0474','0750715V', 'CMR'),
('febe9291-71da-4d79-8d38-dd4e68c942e0','0750714U', 'CMR'),
('015e0643-afce-4927-93dc-1130ce9b15bf','0750711R', 'CMR'),
('c9eeec0b-3846-4fc7-9620-ec34a62f049d','0932122B', 'CMR'),
('480bb965-9666-4655-9bc1-407a71eef9d9','0750669V', 'CMR'),
('b4a4ef3b-1563-4383-bfc1-610660746560','0750668U', 'CMR'),
('b7358e02-1047-4e10-b910-58c6523456f2','0750699C', 'CMR'),
('4902ffeb-7d8c-4938-b1dc-a99177117547','0920149M', 'CMR'),
('f83c66d4-6916-4491-8ac0-47ba8af38422','0750679F', 'CMR');

-- update contract types
ALTER TABLE lystore.contract_type
ADD COLUMN description text;

UPDATE lystore.contract_type
SET description = 'Avances versées aux EPLE sur immobilisations régionales'
WHERE code = '236';

UPDATE lystore.contract_type
SET description = 'Autres matériels de transport'
WHERE code = '21828';

UPDATE lystore.contract_type
SET description = 'Matériel informatique scolaire'
WHERE code = '21831';

UPDATE lystore.contract_type
SET description = 'Matériel de bureau et mobilier scolaires'
WHERE code = '21841';

INSERT INTO lystore.contract_type (code, name, description) VALUES
('2031', 'Etudes', 'Frais d’études'),
('611', 'Prestations (FONCTIONNEMENT)', 'Contrats de prestations de services'),
('6156', 'Maintenance (FONCTIONNEMENT)', 'Contrats de maintenance des biens immobiliers et mobiliers');

-- Update program table
ALTER TABLE lystore.program
ADD COLUMN section character varying(50),
ADD COLUMN chapter bigint,
ADD COLUMN functional_code bigint,
ADD COLUMN label text;


INSERT INTO lystore.program ( name, section, chapter, functional_code, label)
Select  'HP222-008', 'Investissement', 902, 222, 'Équipement des lycées publics'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.program WHERE name='HP222-008'
);


INSERT INTO lystore.program ( name, section, chapter, functional_code, label)
SELECT  'HP224-013', 'Investissement', 902, 224, 'Équipement des cités mixtes départementales'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.program WHERE name='HP224-013'
);

UPDATE lystore.program
SET section = 'Investissement',
    chapter = 902,
    functional_code = 222,
    label = 'Équipement des lycées publics'
WHERE name = 'HP222-008';

UPDATE lystore.program
SET section = 'Investissement',
    chapter = 902,
    functional_code = 224,
    label = 'Équipement des cités mixtes départementales'
WHERE name = 'HP224-013';

INSERT INTO lystore.program (name, section, chapter, functional_code, label) VALUES
('HP222-001', 'Investissement', 902, 222, 'Études générales lycées publics'),
('HP224-030', 'Investissement', 902, 224, 'Équipement des cités mixtes régionales'),
('HP28-005', 'Fonctionnement', 932, 28, 'Schéma des formations');

-- Create program action table
CREATE TABLE lystore.program_action(
    id bigserial,
    id_program bigint NOT NULL,
    action character varying(50) NOT NULL,
    description text,
    CONSTRAINT program_action_pkey PRIMARY KEY (id),
    CONSTRAINT fk_program_id FOREIGN KEY (id_program) REFERENCES lystore.program (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

INSERT INTO lystore.program_action (id_program, action, description) VALUES
((SELECT id FROM lystore.program WHERE name = 'HP222-008'), '12200801', 'Matériel pédagogique'),
((SELECT id FROM lystore.program WHERE name = 'HP222-008'), '12200802', 'Transports'),
((SELECT id FROM lystore.program WHERE name = 'HP222-008'), '12200803', 'Développement des TICE et des ENT'),
((SELECT id FROM lystore.program WHERE name = 'HP224-013'), '12201301', 'Equipement des cités mixtes départementales'),
((SELECT id FROM lystore.program WHERE name = 'HP222-001'), '12200101', 'Etudes générales lycées publics'),
((SELECT id FROM lystore.program WHERE name = 'HP224-030'), '12203001', 'Matériel pédagogique'),
((SELECT id FROM lystore.program WHERE name = 'HP224-030'), '12203002', 'Transports'),
((SELECT id FROM lystore.program WHERE name = 'HP224-030'), '12203003', 'Etudes liées aux cités mixtes régionales'),
((SELECT id FROM lystore.program WHERE name = 'HP224-030'), '12203004', 'Développement des TICE et des ENT'),
((SELECT id FROM lystore.program WHERE name = 'HP28-005'), '12800501', 'Réussite des élèves'),
((SELECT id FROM lystore.program WHERE name = 'HP28-005'), '12800503', 'Logiciels et matériels didactiques'),
((SELECT id FROM lystore.program WHERE name = 'HP28-005'), '12800504', 'Développement des TICE et des ENT');


CREATE TABLE lystore.structure_program_action(
    id bigserial,
    program_action_id bigint NOT NULL,
    structure_type character varying(50) NOT NULL,
    contract_type_id bigint NOT NULL,
    CONSTRAINT structure_program_action_pkey PRIMARY KEY (id),
    CONSTRAINT fk_program_action_id FOREIGN KEY (program_action_id) REFERENCES lystore.program_action (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "structure_program_action_type_values" CHECK (structure_type IN ('CMD', 'CMR', 'LYC')),
    CONSTRAINT fk_contract_type_id FOREIGN KEY(contract_type_id) REFERENCES lystore.contract_type (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

INSERT INTO lystore.structure_program_action (program_action_id, structure_type, contract_type_id) VALUES
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12200801'
    AND program.name = 'HP222-008'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '236')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12201301'
    AND program.name = 'HP224-013'),
    'CMD',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '236')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12203001'
    AND program.name = 'HP224-030'),
    'CMR',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '236')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12800503'
    AND program.name = 'HP28-005'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '611')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12200101'
    AND program.name = 'HP222-001'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '2031')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program.name = 'HP224-013'),
    'CMD',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '2031')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12203003'
    AND program.name = 'HP224-030'),
    'CMR',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '2031')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12800504'
    AND program.name = 'HP28-005'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '6156')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12200802'
    AND program.name = 'HP222-008'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21828')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program.name = 'HP224-013'),
    'CMD',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21828')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12203002'
    AND program.name = 'HP224-030'),
    'CMR',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21828')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12200803'
    AND program.name = 'HP222-008'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21831')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program.name = 'HP224-013'),
    'CMD',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21831')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12203004'
    AND program.name = 'HP224-030'),
    'CMR',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21831')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12200801'
    AND program.name = 'HP222-008'),
    'LYC',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21841')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12201301'
    AND program.name = 'HP224-013'),
    'CMD',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21841')
),
(
    (SELECT program_action.id
    FROM lystore.program_action
    INNER JOIN lystore.program ON (program_action.id_program = program.id)
    WHERE program_action.action = '12203001'
    AND program.name = 'HP224-030'),
    'CMR',
    (SELECT id
    FROM lystore.contract_type
    WHERE code = '21841')
);

ALTER TABLE lystore.order_client_equipment
ADD COLUMN program character varying,
ADD COLUMN action character varying;

CREATE OR REPLACE FUNCTION lystore.getAction(orderId in bigint) RETURNS bigint AS
$BODY$
DECLARE
	actionId bigint;
BEGIN
	SELECT program_action.action
	FROM lystore.program_action
	INNER JOIN lystore.structure_program_action ON (structure_program_action.program_action_id = program_action.id)
	WHERE structure_program_action.contract_type_id = (
		SELECT contract_type.id
		FROM lystore.contract_type
		INNER JOIN lystore.contract ON (contract.id_contract_type = contract_type.id)
		INNER JOIN lystore.equipment ON (equipment.id_contract = contract.id)
		INNER JOIN lystore.order_client_equipment ON (order_client_equipment.equipment_key = equipment.id)
		WHERE order_client_equipment.id = orderId
	)
	AND structure_program_action.structure_type = (
		WITH counter as (
			SELECT count(specific_structures.id) as value
			FROM lystore.specific_structures
			INNER JOIN lystore.order_client_equipment ON (order_client_equipment.id_structure = specific_structures.id)
			WHERE order_client_equipment.id = orderId
		)
		SELECT (
			CASE WHEN counter.value = 0
			THEN 'LYC'
			ELSE (
				SELECT specific_structures.type
				FROM lystore.specific_structures
				INNER JOIN lystore.order_client_equipment ON (order_client_equipment.id_structure = specific_structures.id)
				WHERE order_client_equipment.id = orderId
			)
			END
			) as type
		FROM counter
	) INTO actionId;

	RETURN actionId;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION lystore.fill_program_action() RETURNS TRIGGER AS $order_client_equipment$
DECLARE
	actionValue bigint;
	programValue character varying;
BEGIN
	SELECT lystore.getAction(NEW.id) INTO actionValue;

	SELECT name
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	WHERE program_action.action = actionValue
	INTO programValue;

    NEW.program := programValue;
    NEW.action := actionValue;
    RETURN NEW;
END;
$order_client_equipment$ LANGUAGE plpgsql;

CREATE TRIGGER fill_program_action BEFORE INSERT ON lystore.order_client_equipment
FOR EACH ROW EXECUTE PROCEDURE lystore.fill_program_action();


UPDATE lystore.order_client_equipment
SET action = (
    SELECT lystore.getAction(order_client_equipment.id)
);

UPDATE lystore.order_client_equipment
SET program = values.name
FROM (
	SELECT program.name, order_client_equipment.id
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	INNER JOIN lystore.order_client_equipment ON (order_client_equipment.action = program_action.action)
) AS values
WHERE order_client_equipment.id = values.id;