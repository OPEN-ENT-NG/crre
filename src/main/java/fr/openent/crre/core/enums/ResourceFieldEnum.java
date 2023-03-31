package fr.openent.crre.core.enums;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemFilterField;

public enum ResourceFieldEnum {
    DISCIPLINES(Field.DISCIPLINES),
    NIVEAUX(Field.NIVEAUX),
    CLASSES(Field.CLASSES),
    TECHNOS(Field.TECHNOS),
    EDITEUR(Field.EDITEUR),
    DISTRIBUTEUR(Field.DISTRIBUTEUR),
    PUBLIC_CIBLE(ItemFilterField.TARGET),
    DOCS_TYPE(Field._INDEX);

    private final String fieldName;

    ResourceFieldEnum(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return fieldName;
    }
}

