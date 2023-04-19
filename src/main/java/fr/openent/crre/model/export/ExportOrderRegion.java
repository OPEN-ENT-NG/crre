package fr.openent.crre.model.export;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.FilterModel;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * @deprecated Use {@link FilterModel}
 */
@Deprecated
public class ExportOrderRegion implements IModel<ExportOrderRegion> {
    private List<Integer> idsOrders;
    private List<String> idsEquipments;
    private List<String> idsStructures;
    private String idUser;

    public ExportOrderRegion() {
    }

    public ExportOrderRegion(JsonObject jsonObject) {
        this.idsOrders = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.IDSORDERS), Integer.class);
        this.idsEquipments = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.IDSEQUIPMENTS), String.class);
        this.idsStructures = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.IDSSTRUCTURES), String.class);
        this.idUser = jsonObject.getString(Field.IDUSER);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }

    public JsonObject toEventBusJson() {
        return new JsonObject().put(Field.ACTION, ExportTypeEnum.ORDER_REGION.toString())
                .put(Field.PARAMS, toJson());
    }

    public List<Integer> getIdsOrders() {
        return idsOrders;
    }

    public ExportOrderRegion setIdsOrders(List<Integer> idsOrders) {
        this.idsOrders = idsOrders;
        return this;
    }

    public List<String> getIdsEquipments() {
        return idsEquipments;
    }

    public ExportOrderRegion setIdsEquipments(List<String> idsEquipments) {
        this.idsEquipments = idsEquipments;
        return this;
    }

    public List<String> getIdsStructures() {
        return idsStructures;
    }

    public ExportOrderRegion setIdsStructures(List<String> idsStructures) {
        this.idsStructures = idsStructures;
        return this;
    }

    public String getIdUser() {
        return idUser;
    }

    public ExportOrderRegion setIdUser(String idUser) {
        this.idUser = idUser;
        return this;
    }
}
