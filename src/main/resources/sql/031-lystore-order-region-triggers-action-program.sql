ALTER TABLE lystore."order-region-equipment"
ADD COLUMN program character varying,
ADD COLUMN action character varying;

CREATE OR REPLACE FUNCTION lystore.get_action_region(orderId in bigint) RETURNS character varying AS
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
		INNER JOIN lystore."order-region-equipment" ON ("order-region-equipment".equipment_key = equipment.id)
		WHERE "order-region-equipment".id = orderId
	)
	AND structure_program_action.structure_type = (
		WITH counter as (
			SELECT count(specific_structures.id) as value
			FROM lystore.specific_structures
			INNER JOIN lystore."order-region-equipment" ON ("order-region-equipment".id_structure = specific_structures.id)
			WHERE "order-region-equipment".id = orderId
		)
		SELECT (
			CASE WHEN counter.value = 0
			THEN 'LYC'
			ELSE (
				SELECT specific_structures.type
				FROM lystore.specific_structures
				INNER JOIN lystore."order-region-equipment" ON ("order-region-equipment".id_structure = specific_structures.id)
				WHERE "order-region-equipment".id = orderId
			)
			END
			) as type
		FROM counter
	) INTO actionId;

	RETURN actionId;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION lystore.fill_program_action_region() RETURNS TRIGGER AS $order_region_equipment$
DECLARE
	actionValue character varying;
	programValue character varying;
BEGIN
	SELECT lystore.get_action_region(NEW.id) INTO actionValue;

	SELECT name
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	WHERE program_action.action = actionValue
	INTO programValue;

    RAISE NOTICE '%', NEW.id;
    RAISE NOTICE '%', programValue;
    RAISE NOTICE '%', actionValue;
    UPDATE lystore."order-region-equipment"
    SET program = programValue,
        action = actionValue
    WHERE id = NEW.id;

    RETURN NEW;
END;
$order_region_equipment$ LANGUAGE plpgsql;

CREATE TRIGGER fill_program_action_region AFTER INSERT ON lystore."order-region-equipment"
FOR EACH ROW EXECUTE PROCEDURE lystore.fill_program_action_region();


UPDATE lystore."order-region-equipment"
SET action = (
    SELECT lystore.get_action_region("order-region-equipment".id)
);

UPDATE lystore."order-region-equipment"
SET program = values.name
FROM (
	SELECT program.name, "order-region-equipment".id
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	INNER JOIN lystore."order-region-equipment" ON ("order-region-equipment".action = program_action.action)
) AS values
WHERE "order-region-equipment".id = values.id;