package fr.openent.crre.core.enums;

import java.util.Arrays;

public enum OrderStatus {
    WAITING("WAITING"),
    REJECTED("REJECTED"),
    IN_PROGRESS("IN_PROGRESS"),
    VALID("VALID"),
    SENT("SENT"),
    RESUBMIT("RESUBMIT"),
    WAITING_FOR_ACCEPTANCE("WAITING_FOR_ACCEPTANCE"),
    DONE("DONE"),
    ;

    private final String value;

    OrderStatus(String value) {
        this.value = value;
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
}
