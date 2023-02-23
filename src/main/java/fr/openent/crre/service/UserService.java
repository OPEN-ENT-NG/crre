package fr.openent.crre.service;

import fr.openent.crre.model.neo4j.Neo4jUserModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * Return all user structures based on user id
     * @param userId User id
     * @param handler Function handler returning data
     */
    void getStructures(String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Return all user having {@link fr.openent.crre.security.WorkflowActions#VALIDATOR_RIGHT} but not {@link fr.openent.crre.security.WorkflowActions#ADMINISTRATOR_RIGHT}
     *
     * @param structureIdList structure id list filter
     */
    Future<Map<Neo4jUserModel, String>> getValidatorUser(List<String> structureIdList);
}
