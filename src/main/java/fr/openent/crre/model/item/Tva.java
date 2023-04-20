package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

public class Tva implements IModel<Tva> {
    private Float percent;
    private Float rate;

    public Tva(JsonObject json) {
        this.percent = json.getFloat(ItemField.PERCENT);
        this.rate = json.getFloat(ItemField.RATE);
    }

    // Getters and setters

    public Float getPercent() {
        return percent;
    }

    public Tva setPercent(Float percent) {
        this.percent = percent;
        return this;
    }

    public Float getRate() {
        return rate;
    }

    public Tva setRate(Float rate) {
        this.rate = rate;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(ItemField.PERCENT, this.percent)
                .put(ItemField.RATE, this.rate);
    }
}
