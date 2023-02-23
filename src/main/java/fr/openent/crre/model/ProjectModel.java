package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class ProjectModel implements IModel<ProjectModel> {
    private Integer id;
    private String title;

    private String structureId;

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
}
