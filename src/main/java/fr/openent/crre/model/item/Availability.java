package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

public class Availability implements IModel<Availability> {

    private String value;
    private String availabilityDate;
    private boolean orderable;
    private String comment;

    public Availability(JsonObject json) {
        this.value = json.getString(ItemField.VALUE);
        this.availabilityDate = json.getString(ItemField.DATE_AVAILABILITY);
        this.orderable = json.getBoolean(ItemField.ORDERABLE);
        this.comment = json.getString(ItemField.COMMENT);
    }

    // Getters and setters

    public String getValue() {
        return value;
    }

    public Availability setValue(String value) {
        this.value = value;
        return this;
    }

    public String getAvailabilityDate() {
        return availabilityDate;
    }

    public Availability setAvailabilityDate(String availabilityDate) {
        this.availabilityDate = availabilityDate;
        return this;
    }

    public boolean isOrderable() {
        return orderable;
    }

    public Availability setOrderable(boolean orderable) {
        this.orderable = orderable;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public Availability setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(ItemField.VALUE, this.value)
                .put(ItemField.DATE_AVAILABILITY, this.availabilityDate)
                .put(ItemField.ORDERABLE, this.orderable)
                .put(ItemField.COMMENT, this.comment);
    }
}
