package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OldBasketService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import static fr.openent.crre.service.impl.DefaultBasketService.filterBasketSearch;

public class DefaultOldBasketService extends SqlCrudService implements OldBasketService {

    private final Integer PAGE_SIZE = 15;

    public DefaultOldBasketService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void search(String query, JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment_old oe ON (bo.id = oe.id_basket) " +
                "WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ? ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(id_campaign);
        if (!query.equals("")) {
            sqlquery += "AND (bo.name ~* ? OR bo.name_user ~* ? OR oe.equipment_name ~* ?)";
            values.add(query);
            values.add(query);
            values.add(query);
        }
        sqlquery += " AND bo.id_structure IN ( ";
        sqlquery = filterBasketSearch(filters, user, values, sqlquery, page, PAGE_SIZE);
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }
    
    public void filter(JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".order_client_equipment_old oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE bo.created BETWEEN ? AND ? AND bo.id_campaign = ? AND bo.id_user = ? ";
        values.add(startDate)
                .add(endDate)
                .add(id_campaign)
                .add(user.getUserId());
        sqlquery = filterBasketSearch(filters, user, values, sqlquery, page, PAGE_SIZE);
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }
}
