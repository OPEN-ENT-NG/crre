package fr.openent.crre.model;

import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;
import fr.openent.crre.core.constants.Field;


public class WorkflowNeo4jModel implements IModel<WorkflowNeo4jModel> {
    private String displayName;
    private String name;

    public WorkflowNeo4jModel() {
    }

    public WorkflowNeo4jModel(JsonObject jsonObject) {
        this.displayName = jsonObject.getString(Field.DISPLAYNAME);
        this.name = jsonObject.getString(Field.NAME);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public String getDisplayName() {
        return displayName;
    }

    public WorkflowNeo4jModel setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getName() {
        return name;
    }

    public WorkflowNeo4jModel setName(String name) {
        this.name = name;
        return this;
    }
}
