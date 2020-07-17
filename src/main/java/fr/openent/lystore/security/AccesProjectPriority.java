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

public class AccesProjectPriority implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        Integer idProject = Integer.parseInt(request.params().get("idProject"));
        Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
        String idStructure =request.getParam("structureId");
        if(idProject == null || idCampaign == null || idStructure == null){
            request.response().setStatusCode(400).end();
            return;
        }
        String query = "SELECT count(DISTINCT project.id) " +
                "FROM lystore.project " +
                "INNER JOIN lystore.order_client_equipment ON (project.id = order_client_equipment.id_project) " +
                "WHERE order_client_equipment.id_campaign = ? " +
                        "AND order_client_equipment.id_structure = ? " +
                        "AND project.id = ?;";
        JsonArray params = new JsonArray().add(idCampaign).add(idStructure).add(idProject);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {

            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    request.resume();
                    JsonArray result = stringJsonArrayEither.right().getValue();
                    handler.handle(result.size() == 1 && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.ACCESS_RIGHT.toString()));
                } else {
                    request.response().setStatusCode(500).end();
                }
            }
        }));

    }
}
