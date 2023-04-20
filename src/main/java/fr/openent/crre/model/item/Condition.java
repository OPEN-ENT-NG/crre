package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

public class Condition implements IModel<Condition> {
    private Integer freeCondition;
    private Integer freeNumber;

    public Condition(JsonObject json) {
        this.freeCondition = json.getInteger(ItemField.FREE_CONDITION);
        this.freeNumber = json.getInteger(ItemField.FREE);
    }

    public Integer getfreeCondition() {
        return freeCondition;
    }

    public Condition setfreeCondition(Integer freeCondition) {
        this.freeCondition = freeCondition;
        return this;
    }

    public Integer getfreeNumber() {
        return freeNumber;
    }

    public Condition setfreeNumber(Integer freeNumber) {
        this.freeNumber = freeNumber;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(ItemField.FREE_CONDITION, this.freeCondition)
                .put(ItemField.FREE, this.freeNumber);
    }
}