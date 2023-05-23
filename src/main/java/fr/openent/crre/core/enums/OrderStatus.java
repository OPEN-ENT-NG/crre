package fr.openent.crre.core.enums;

import java.util.Arrays;

public enum OrderStatus {
    WAITING("WAITING", false),
    REJECTED("REJECTED", false),
    IN_PROGRESS("IN_PROGRESS", false),
    VALID("VALID", false),
    SENT("SENT", true),
    RESUBMIT("RESUBMIT", false),
    WAITING_FOR_ACCEPTANCE("WAITING_FOR_ACCEPTANCE", false),
    DONE("DONE", true),
    ARCHIVED("ARCHIVED", true),
    ;

    private final String value;
    private final boolean isHistoricStatus;

    OrderStatus(String value, boolean isHistoricStatus) {
        this.value = value;
        this.isHistoricStatus = isHistoricStatus;
    }

    public static OrderStatus getValue(String value) {
        return Arrays.stream(OrderStatus.values())
                .filter(orderClientEquipmentType -> orderClientEquipmentType.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return this.value;
    }

    public boolean isHistoricStatus() {
        return isHistoricStatus;
    }
}
