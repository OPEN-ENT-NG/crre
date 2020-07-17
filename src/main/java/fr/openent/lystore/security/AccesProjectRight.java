package fr.openent.lystore.security;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class AccesProjectRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String id, query;
        query = "SELECT count(project.id) " +
                "FROM lystore.project " +
                "INNER JOIN lystore.order_client_equipment oce  ON project.id = oce.id_project " +
                "WHERE project.id = ? " +
                "AND id_structure IN " + Sql.listPrepared(userInfos.getStructures());

        id = request.getParam("id");
        if (id != null) {
            JsonArray params = new JsonArray();
            params.add(id);
            for (String structure : userInfos.getStructures()) {
                params.add(structure);
            }
            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        request.resume();
                        JsonArray result = event.right().getValue();
                        handler.handle(result.size() == 1 && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.ACCESS_RIGHT.toString()));
                    } else {
                        request.response().setStatusCode(500).end();
                    }
                }
            }));
        } else {
            request.response().setStatusCode(400).end();
        }
    }
}
