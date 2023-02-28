package fr.openent.crre.model.neo4j;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public class Neo4jUserModel extends UserInfos implements IModel<Neo4jUserModel> {
    private String structureId;

    public Neo4jUserModel() {
    }

    public Neo4jUserModel(JsonObject user) {
        setUserId(user.getString(Field.ID));
    }

    public Neo4jUserModel(String userId) {
        setUserId(userId);
    }

    public String getStructureId() {
        return structureId;
    }

    public Neo4jUserModel setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.getUserId());
    }
}
