WITH values AS (
	SELECT id
	FROM lystore.order_client_equipment
	WHERE equipment_key = 35
	AND action IS NULL
)
UPDATE lystore.order_client_equipment
SET action = (
	SELECT program_action.action
	FROM lystore.program_action
	INNER JOIN lystore.structure_program_action ON (structure_program_action.program_action_id = program_action.id)
	WHERE structure_program_action.contract_type_id = (
		SELECT contract_type.id
		FROM lystore.contract_type
		INNER JOIN lystore.contract ON (contract.id_contract_type = contract_type.id)
		WHERE contract.id = 1
	)
	AND structure_program_action.structure_type = (
		WITH counter as (
			SELECT count(specific_structures.id) as value
			FROM lystore.specific_structures
			INNER JOIN lystore.order_client_equipment ON (order_client_equipment.id_structure = specific_structures.id)
			WHERE order_client_equipment.id = values.id
		)
		SELECT (
			CASE WHEN counter.value = 0
			THEN 'LYC'
			ELSE (
				SELECT specific_structures.type
				FROM lystore.specific_structures
				INNER JOIN lystore.order_client_equipment ON (order_client_equipment.id_structure = specific_structures.id)
				WHERE order_client_equipment.id = values.id
			)
			END
			) as type
		FROM counter
	)
)
FROM values
WHERE order_client_equipment.id = values.id;


UPDATE lystore.order_client_equipment
SET program = values.name
FROM (
	SELECT program.name, order_client_equipment.id
	FROM lystore.program
	INNER JOIN lystore.program_action ON (program.id = program_action.id_program)
	INNER JOIN lystore.order_client_equipment ON (order_client_equipment.action = program_action.action)
) AS values
WHERE order_client_equipment.id = values.id
AND program IS NULL;