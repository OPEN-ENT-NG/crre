
CREATE TABLE lystore.export (
  filename character varying(255) NOT NULL,
  created timestamp without time zone NOT NULL DEFAULT now(),
  fileid CHARACTER  VARYING (255) NOT NULL,
  ownerid CHARACTER  VARYING (255) NOT NULL
);
