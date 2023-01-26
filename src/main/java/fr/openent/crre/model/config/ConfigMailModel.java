package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

public class ConfigMailModel implements IModel<ConfigModel> {
    private final String address;

    public ConfigMailModel(JsonObject jsonObject) {
        this.address = jsonObject.getString(Field.ADDRESS);
    }

    public String getAddress() {
        return address;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject().put(Field.ADDRESS, this.address);
    }
}
