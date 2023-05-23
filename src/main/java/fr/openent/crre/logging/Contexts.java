package fr.openent.crre.logging;

import fr.openent.crre.core.enums.OrderStatus;

public enum Contexts {
    EQUIPMENT ("EQUIPMENT"),
    CAMPAIGN ("CAMPAIGN"),
    STRUCTUREGROUP("STRUCTUREGROUP"),
    PURSE ("PURSE"),
    BASKET("BASKET"),
    ORDER ("ORDER"),
    ORDERREGION ("ORDERREGION"),
    BASKET_ITEM ("BASKET_ITEM"),
    EXPORT("EXPORT");

    Contexts (String context) {
    }
}
