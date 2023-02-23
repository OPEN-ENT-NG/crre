package fr.openent.crre.core.enums;

import java.util.Arrays;

public enum CreditTypeEnum {
    LICENCES("licences"),
    CONSUMABLE_LICENCES("consumable_licences"),
    CREDITS("credits"),
    CONSUMABLE_CREDITS("consumable_credits"),
    NONE("none");

    private final String name;

    CreditTypeEnum(String name) {
        this.name = name;
    }

    public static CreditTypeEnum getValue(String value) {
        return getValue(value, null);
    }

    public static CreditTypeEnum getValue(String value, CreditTypeEnum defaultValue) {
        return Arrays.stream(CreditTypeEnum.values())
                .filter(creditTypeEnum -> creditTypeEnum.name.equals(value))
                .findFirst()
                .orElse(defaultValue);
    }
}
