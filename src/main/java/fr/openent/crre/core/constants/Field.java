package fr.openent.crre.core.constants;

public class Field {

    public static final String MAIL = "mail";
    public static final String EQUIPEMENT = "equipment";

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