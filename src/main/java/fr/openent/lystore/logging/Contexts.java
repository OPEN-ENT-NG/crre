package fr.openent.lystore.logging;

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
    GRADE("GRADE"),
    PROJECT("PROJECT"),
    ORDER ("ORDER"),
    ORDERREGION ("ORDERREGION"),
    OPERATION("OPERATION"),
    INSTRUCTION("INSTRUCTION"),
    EXPORT("EXPORT");

    private final String contextName;

    Contexts (String context) {
        this.contextName = context;
    }
}
