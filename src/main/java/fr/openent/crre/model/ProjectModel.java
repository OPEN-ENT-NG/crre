package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectModel implements IModel<ProjectModel>, Cloneable {
    private Integer id;
    private String title;
    private String structureId;

    private List<OrderRegionEquipmentModel> orderRegionEquipmentList;

    public ProjectModel() {
    }

    public ProjectModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.title = jsonObject.getString(Field.TITLE);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, false, true);
    }

    public Integer getId() {
        return id;
    }

    public ProjectModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ProjectModel setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public ProjectModel setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public List<OrderRegionEquipmentModel> getOrderRegionEquipmentList() {
        return orderRegionEquipmentList;
    }

    public ProjectModel setOrderRegionEquipmentList(List<OrderRegionEquipmentModel> orderRegionEquipmentList) {
        this.orderRegionEquipmentList = orderRegionEquipmentList;
        return this;
    }

    @Override
    public ProjectModel clone() {
        try {
            ProjectModel clone = (ProjectModel) super.clone();
            if (this.orderRegionEquipmentList != null) {
                clone.setOrderRegionEquipmentList(this.orderRegionEquipmentList.stream()
                        .map(OrderRegionEquipmentModel::clone).collect(Collectors.toList()));
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
