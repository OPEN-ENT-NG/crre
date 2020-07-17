ALTER TABLE lystore.export
ADD COLUMN instruction_name  CHAR VARYING (255) NOT NULL DEFAULT 'OLD INSTRUCTION',
ADD COLUMN instruction_id  bigint not null DEFAULT -1
