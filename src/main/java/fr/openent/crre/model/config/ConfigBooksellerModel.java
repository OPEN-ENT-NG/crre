package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.IModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.bookseller.IBooksellerConfigModel;
import fr.openent.crre.service.impl.bookseller.BooksellerServiceEnum;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class ConfigBooksellerModel implements IModel<ConfigBooksellerModel> {
    private BooksellerServiceEnum type;
    private String name;
    private IBooksellerConfigModel param;

    public ConfigBooksellerModel() {
    }

    public ConfigBooksellerModel(JsonObject jsonObject) {
        this.type = BooksellerServiceEnum.getValue(jsonObject.getString(Field.TYPE), null);
        this.name = jsonObject.getString(Field.NAME);
        this.param = this.type.getService().generateModel(jsonObject.getJsonObject(Field.PARAM));
    }

    public BooksellerServiceEnum getType() {
        return type;
    }

    public ConfigBooksellerModel setType(BooksellerServiceEnum type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public ConfigBooksellerModel setName(String name) {
        this.name = name;
        return this;
    }

    public IBooksellerConfigModel getParam() {
        return param;
    }

    public ConfigBooksellerModel setParam(IBooksellerConfigModel param) {
        this.param = param;
        return this;
    }

    public void getStatus() {
        this.type.getService().updateStatus(param);
    }

    public Future<Void> sendOrder(List<OrderUniversalModel> orderList) {
        return this.type.getService().sendOrder(orderList, param);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }
}
