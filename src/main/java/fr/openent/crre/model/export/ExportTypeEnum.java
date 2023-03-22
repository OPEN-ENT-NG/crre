package fr.openent.crre.model.export;

import java.util.Arrays;

public enum ExportTypeEnum {
    ORDER_REGION,
    NULL;

    public static ExportTypeEnum getValue(String value, ExportTypeEnum defaultValue) {
        return Arrays.stream(ExportTypeEnum.values())
                .filter(exportTypeEnum -> exportTypeEnum.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(defaultValue);
    }
}
