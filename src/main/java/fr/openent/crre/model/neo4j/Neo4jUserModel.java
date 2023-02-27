package fr.openent.crre.model.neo4j;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public class Neo4jUserModel extends UserInfos {
    private String structureId;

    public Neo4jUserModel() {
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
}
