CREATE  SEQUENCE lystore.seq_validation_number
  INCREMENT 1
  MINVALUE 0
  MAXVALUE 99999999999999
  START 19700101001
  CACHE 1;

CREATE OR REPLACE FUNCTION lystore.get_validation_number()
  RETURNS VARCHAR AS $$
DECLARE
nextSeqVal VARCHAR ;
valeurinitiale VARCHAR := '0001';
BEGIN
	select  pg_catalog.nextval('lystore.seq_validation_number'::regclass) into nextSeqVal;
	if (left(nextSeqVal ,8 ) != replace (  CURRENT_DATE || '', '-' , ''))
	then
		select replace (  CURRENT_DATE || '', '-' , '') || valeurinitiale into nextSeqVal;
		PERFORM pg_catalog.setval('lystore.seq_validation_number'::regclass, nextSeqVal::BIGINT, true);
	end if;
	return nextSeqVal;
 END;
 $$ LANGUAGE plpgsql VOLATILE;

ALTER TABLE lystore.order_client_equipment
	ADD COLUMN file character varying (250);

ALTER TABLE lystore.order_client_equipment RENAME COLUMN "number" TO "number_validation";