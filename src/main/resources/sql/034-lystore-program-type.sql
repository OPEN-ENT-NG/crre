ALTER TABLE Lystore.program
ADD COLUMN program_type VARCHAR (50) NOT NULL DEFAULT '0';

UPDATE Lystore.program
SET program_type = '122008'
WHERE name = 'HP222-008'
;
UPDATE Lystore.program
SET program_type = '122001'
WHERE name = 'HP222-001'
;
UPDATE Lystore.program
SET program_type = '122013'
WHERE name = 'HP224-013'
;
UPDATE Lystore.program
SET program_type = '122030'
WHERE name = 'HP224-030'
;
UPDATE Lystore.program
SET program_type = '128005'
WHERE name = 'HP28-005'
;