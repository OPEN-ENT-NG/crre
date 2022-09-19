package fr.openent.crre.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    public static final String STATUS = "status";
    public static final String OK = "ok";
    public static final String ERROR = "error";

    public static final String rejected = "rejected";
    public static final String resubmit = "resubmit";
}