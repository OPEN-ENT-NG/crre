package fr.openent.lystore.logging;

public enum Actions {
    CREATE ("CREATE"),
    UPDATE ("UPDATE"),
    DELETE ("DELETE"),
    IMPORT ("IMPORT");

    private final String actionName;

    Actions (String action) {
        this.actionName = action;
    }
}
