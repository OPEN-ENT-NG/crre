package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.UserService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultUserService implements UserService {
    @Override
    public void getStructures(String userId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (a:Action {displayName:'lystore.access'})<-[AUTHORIZE]-(:Role)" +
                "<-[AUTHORIZED]-(g:Group:ManualGroup)-[:DEPENDS]->(s:Structure), " +
                "(u:User {id:{userId}})-[:IN]->(g) return s.id as id, s.name as name, s.UAI as UAI";

        JsonObject params = new JsonObject()
                .put("userId", userId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
