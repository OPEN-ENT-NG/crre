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

public class AccessPriceProposalRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String query,id;
        query = "SELECT count(basket_equipment.id) FROM "+ Crre.crreSchema + ".basket_equipment "
                + " INNER JOIN " + Crre.crreSchema + ".equipment ON equipment.id = basket_equipment.id_equipment "
                + " INNER JOIN " + Crre.crreSchema + ".contract ON contract.id = equipment.id_contract "
                + " WHERE equipment.price_editable IS TRUE AND basket_equipment.id = ? ";
        id = request.getParam("idBasket");
        if (id != null) {
            JsonArray params = new JsonArray().add(id);
            rightAccess(request, userInfos, handler, query, params);
        } else {
            request.response().setStatusCode(400).end();
        }
    }
}
