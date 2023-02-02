package fr.openent.crre.core.constants;

public class Field {

    public static final String MAIL = "mail";
    public static final String EQUIPEMENT = "equipment";
    public static final String CONTENT = "content";
    public static final String NB_ETAB = "nbEtab";
    public static final String ADDRESS = "address";
    public static final String CSVFILE = "csvFile";
    public static final String ORDER_CLIENT_EQUIPMENT = "order_client_equipment";
    public static final String ID_STRUCTURE = "id_structure";
    public static final String DESCRIPTION = "description";
    public static final String STRUCTURES = "structures";
    public static final String _ID = "_id";
    public static final String METADATA = "metadata";
    public static final String FILENAME = "filename";
    public static final String MESSAGE = "message";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ELASTICSEARCHCONFIG = "elasticsearchConfig";
    public static final String API_DASH_KEY = "api-key";
    public static final String EMAILCONFIG = "emailConfig";

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    public static final String STATUS = "status";
    public static final String ID = "id";
    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String rejected = "rejected";
    public static final String resubmit = "resubmit";
    public static final String NAME = "name";
    public static final String TITLE = "title";
}