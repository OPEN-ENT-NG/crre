package fr.openent.crre.security;

import fr.openent.crre.Crre;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class AccessOrderCommentRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String id,query;
        if(binding.getServiceMethod().equals("fr.openent.crre.controllers.BasketController|updateComment")){
            id = request.getParam("idBasket");
           query = "SELECT count(id) FROM " + Crre.crreSchema + ".basket_order_item WHERE id = ? AND id_structure IN " + Sql.listPrepared(userInfos.getStructures());
        }else {
            id=request.getParam("idOrder");
           query = "SELECT count(id) FROM " + Crre.crreSchema + ".order_client_equipment WHERE id = ? AND id_structure IN " + Sql.listPrepared(userInfos.getStructures());
        }
        if(id!=null) {
            JsonArray params = new JsonArray();
            params.add(id);
            for (String structure : userInfos.getStructures()) {
                params.add(structure);
            }
            rightAccess(request, userInfos, handler, query, params);
        }else{
            request.response().setStatusCode(400).end();
        }
    }
    private void rightAccess(HttpServerRequest request, UserInfos userInfos, Handler<Boolean> handler, String query, JsonArray params) {
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(stringJsonArrayEither -> {
            if (stringJsonArrayEither.isRight()) {
                request.resume();
                JsonArray result = stringJsonArrayEither.right().getValue();
                handler.handle(result.size() == 1 && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.PRESCRIPTOR_RIGHT.toString()));
            } else {
                request.response().setStatusCode(500).end();
            }
        }));
    }
}
