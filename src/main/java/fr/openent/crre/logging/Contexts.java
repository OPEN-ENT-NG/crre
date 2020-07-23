package fr.openent.crre.logging;

public enum Contexts {
    AGENT ("AGENT"),
    SUPPLIER  ("SUPPLIER"),
    CONTRACT ("CONTRACT"),
    TAG ("TAG"),
    EQUIPMENT ("EQUIPMENT"),
    CAMPAIGN ("CAMPAIGN"),
    STRUCTUREGROUP("STRUCTUREGROUP"),
    PURSE ("PURSE"),
    BASKET("BASKET"),
    TITLE("TITLE"),
    ORDER ("ORDER"),
    ORDERREGION ("ORDERREGION"),
    EXPORT("EXPORT");

    private final String contextName;

    Contexts (String context) {
        this.contextName = context;
    }
}
