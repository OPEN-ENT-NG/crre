package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.WorkflowNeo4jModel;
import fr.openent.crre.service.WorkflowService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultWorkflowService implements WorkflowService {
    private static final Logger log = LoggerFactory.getLogger(DefaultWorkflowService.class);

    @Override
    public Future<Map<String, List<WorkflowNeo4jModel>>> getWorkflowListFromStructureScope(String userId, List<String> structureIdList) {
        Promise<Map<String, List<WorkflowNeo4jModel>>> promise = Promise.promise();

        String query = "MATCH (u:User)-->(g:Group)-->(r:Role)-[:AUTHORIZE]->(w:WorkflowAction), (g)-[:DEPENDS]->(s:Structure)" +
                " WHERE s.id IN {structureIdList} AND u.id = {userId}" +
                " RETURN distinct s.id, w";
        JsonObject params = new JsonObject()
                .put("structureIdList", new JsonArray(structureIdList))
                .put("userId", userId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue().stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .collect(Collectors.groupingBy(jsonObject -> jsonObject.getString("s.id")))
                        .entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                stringListEntry -> stringListEntry.getValue().stream()
                                        .map(jsonObject -> new WorkflowNeo4jModel(jsonObject.getJsonObject("w", new JsonObject())
                                                .getJsonObject(Field.DATA)))
                                        .collect(Collectors.toList())
                        )));
            } else {
                log.error(String.format("[CRRE@%s::getValidatorUser] Fail to get workflow list from structure scope %s",
                        this.getClass().getSimpleName(), event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        }));
        return promise.future();
    }
}
