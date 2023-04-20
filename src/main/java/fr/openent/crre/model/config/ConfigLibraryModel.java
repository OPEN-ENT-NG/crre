package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.IModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.library.ILibraryConfigModel;
import fr.openent.crre.service.impl.library.LibraryServiceEnum;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class ConfigLibraryModel implements IModel<ConfigLibraryModel> {
    private LibraryServiceEnum type;
    private String name;
    private ILibraryConfigModel param;

    public ConfigLibraryModel() {
    }

    public ConfigLibraryModel(JsonObject jsonObject) {
        this.type = LibraryServiceEnum.getValue(jsonObject.getString(Field.TYPE), null);
        this.name = jsonObject.getString(Field.NAME);
        this.param = this.type.getService().generateModel(jsonObject.getJsonObject(Field.PARAM));
    }

    public LibraryServiceEnum getType() {
        return type;
    }

    public ConfigLibraryModel setType(LibraryServiceEnum type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public ConfigLibraryModel setName(String name) {
        this.name = name;
        return this;
    }

    public ILibraryConfigModel getParam() {
        return param;
    }

    public ConfigLibraryModel setParam(ILibraryConfigModel param) {
        this.param = param;
        return this;
    }

    public void getStatus() {
        this.type.getService().getStatus(param);
    }

    public void sendOrder(List<OrderUniversalModel> orderList) {
        this.type.getService().sendOrder(orderList, param);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }
}
