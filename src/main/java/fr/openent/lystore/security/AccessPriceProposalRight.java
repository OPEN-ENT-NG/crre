package fr.openent.lystore.security;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class AccessPriceProposalRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String query,id;
        query = "SELECT count(basket_equipment.id) FROM "+ Lystore.lystoreSchema + ".basket_equipment "
                + " INNER JOIN " + Lystore.lystoreSchema + ".equipment ON equipment.id = basket_equipment.id_equipment "
                + " INNER JOIN " + Lystore.lystoreSchema + ".contract ON contract.id = equipment.id_contract "
                + " WHERE equipment.price_editable IS TRUE AND basket_equipment.id = ? ";
        id = request.getParam("idBasket");


        if (id != null) {
            JsonArray params = new JsonArray().add(id);
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
