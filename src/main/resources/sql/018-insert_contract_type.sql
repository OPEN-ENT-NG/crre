
INSERT INTO lystore.contract_type ( code, name)
SELECT  '236', 'Subventions'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.contract_type WHERE code='236'
);

INSERT INTO lystore.contract_type ( code, name)
SELECT '21828', 'Véhicules'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.contract_type WHERE code='21828'
);

INSERT INTO lystore.contract_type ( code, name)
SELECT  '21831', 'Informatique'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.contract_type WHERE code='21831'
);

INSERT INTO lystore.contract_type ( code, name)
SELECT  '21841', 'Mobilier'
WHERE NOT EXISTS (
	SELECT 1 FROM  lystore.contract_type WHERE code='21841'
);
