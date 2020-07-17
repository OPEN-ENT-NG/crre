package fr.openent.lystore.security;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class PostBasketFileRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        Integer basketId = Integer.parseInt(request.getParam("id"));

        String checkQuery = "SELECT count(contract.id) " +
                "FROM " + Lystore.lystoreSchema + ".basket_equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON (basket_equipment.id_equipment = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (equipment.id_contract = contract.id) " +
                "WHERE basket_equipment.id = ? " +
                "AND contract.file = true";

        Sql.getInstance().prepared(checkQuery, new JsonArray().add(basketId), SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray res = event.right().getValue();
                handler.handle(res.getJsonObject(0).getInteger("count") == 1 && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.ACCESS_RIGHT.toString()));
            } else {
                handler.handle(false);
            }
        }));

    }
}
