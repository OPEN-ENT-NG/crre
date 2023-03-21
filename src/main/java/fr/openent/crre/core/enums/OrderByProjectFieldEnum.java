package fr.openent.crre.core.enums;

import java.util.Arrays;

public enum OrderByProjectFieldEnum {
    UAI,
    STRUCTURE_NAME,
    DATE,
    QUANTITY,
    NAME,
    ID;

    public static OrderByProjectFieldEnum getValue(String value, OrderByProjectFieldEnum defaultOrderByProjectFieldEnum) {
        return Arrays.stream(OrderByProjectFieldEnum.values())
                .filter(orderByProjectFieldEnum -> orderByProjectFieldEnum.name().equals(value))
                .findFirst()
                .orElse(defaultOrderByProjectFieldEnum);
    }
}
