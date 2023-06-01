package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import io.vertx.core.json.JsonObject;

public class Credits implements IModel<Credits> {
    private Double addValue;
    private Double newValue;
    private Double refundValue;

    public Credits(JsonObject json) {
        this.addValue = json.getDouble(Field.ADD_VALUE, null);
        this.newValue = json.getDouble(Field.NEW_VALUE, null);
        this.refundValue = json.getDouble(Field.REFUND_VALUE, null);
    }

    public Credits() {
        this.addValue = null;
        this.newValue = null;
        this.refundValue = null;
    }

    public Credits(double addValue, double newValue, double refundValue) {
        this.addValue = addValue;
        this.newValue = newValue;
        this.refundValue = refundValue;
    }

    public Credits(String newValue, String addValue, String refundValue) {
        if (addValue != null && !addValue.isEmpty()) {
            this.addValue = Double.parseDouble(addValue);
        }
        if (newValue != null && !newValue.isEmpty()) {
            this.newValue = Double.parseDouble(newValue);
        }
        if (refundValue != null && !refundValue.isEmpty()) {
            this.refundValue = Double.parseDouble(refundValue);
        }
    }

    public Double getAddValue() {
        return addValue;
    }

    public Credits setAddValue(Double addValue) {
        this.addValue = addValue;
        return this;
    }

    public Double getNewValue() {
        return newValue;
    }

    public Credits setNewValue(Double newValue) {
        this.newValue = newValue;
        return this;
    }

    public Double getRefundValue() {
        return refundValue;
    }

    public Credits setRefundValue(Double refundValue) {
        this.refundValue = refundValue;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.NEW_VALUE, this.newValue)
                .put(Field.ADD_VALUE, this.addValue)
                .put(Field.REFUND_VALUE, this.refundValue);

    }

    public Double getDefaultValue() {
        Double defaultValue = 0.0;
        if(this.getNewValue() != null) {
            defaultValue = this.getNewValue();
        } else if (this.getAddValue() != null) {
            defaultValue = this.getAddValue();
        }
        return defaultValue;
    }

    public Boolean isEmpty() {
        return this.newValue == null && this.addValue == null && this.refundValue == null;
    }


}
