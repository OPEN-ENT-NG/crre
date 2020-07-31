package fr.openent.crre.security;

import fr.openent.crre.Crre;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.crre.security.AccesProjectPriority.rightAccess;

public class AccesProjectRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String id, query;
        query = "SELECT count(project.id) " +
                "FROM " + Crre.crreSchema + ".project " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment oce  ON project.id = oce.id_project " +
                "WHERE project.id = ? " +
                "AND id_structure IN " + Sql.listPrepared(userInfos.getStructures());

        id = request.getParam("id");
        if (id != null) {
            JsonArray params = new JsonArray();
            params.add(id);
            for (String structure : userInfos.getStructures()) {
                params.add(structure);
            }
            rightAccess(request, userInfos, handler, query, params);
        } else {
            request.response().setStatusCode(400).end();
        }
    }
}
