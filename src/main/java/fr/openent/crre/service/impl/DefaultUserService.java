package fr.openent.crre.service.impl;

import fr.openent.crre.service.UserService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultUserService implements UserService {
    @Override
    public void getStructures(String userId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (a:Action {displayName:'crre.access'})<-[AUTHORIZE]-(:Role)" +
                "<-[AUTHORIZED]-(g:Group)-[:DEPENDS]->(s:Structure), " +
                "(u:User {id:{userId}})-[:IN]->(g) return DISTINCT s.id as id, s.name as name, s.UAI as UAI, s.type";
        JsonObject params = new JsonObject()
                .put("userId", userId);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}