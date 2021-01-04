CREATE TABLE crre.project (
    id integer NOT NULL,
    title character varying(50)
);
ALTER TABLE crre.project OWNER TO "web-education";

CREATE SEQUENCE crre.project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE crre.project_id_seq OWNER TO "web-education";
ALTER SEQUENCE crre.project_id_seq OWNED BY crre.project.id;
ALTER TABLE ONLY crre.project ALTER COLUMN id SET DEFAULT nextval('crre.project_id_seq'::regclass);
ALTER TABLE ONLY crre.project ADD CONSTRAINT project_pkey PRIMARY KEY (id);
