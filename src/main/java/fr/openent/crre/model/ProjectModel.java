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
    private String comment;

    public ProjectModel() {
    }

    public ProjectModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.title = jsonObject.getString(Field.TITLE);
        this.comment = jsonObject.getString(Field.COMMENT);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
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

    public String getComment() {
        return comment;
    }

    public ProjectModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public ProjectModel clone() {
        try {
            return (ProjectModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
