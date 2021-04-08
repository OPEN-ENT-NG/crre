package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OldBasketService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultOldBasketService extends SqlCrudService implements OldBasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketService.class);

    private final Integer PAGE_SIZE = 15;

    public DefaultOldBasketService(String schema, String table) {
        super(schema, table);
    }

    public void listBasket(Integer idCampaign, String idStructure,  UserInfos user, Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT id, amount, comment , processing_date, id_campaign, id_structure, id_equipment, reassort " +
                "FROM " + Crre.crreSchema + ".basket_equipment basket " +
                "WHERE basket.id_campaign = ? " +
                "AND basket.id_structure = ? " +
                "AND basket.owner_id = ? " +
                "GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure) " +
                "ORDER BY basket.id DESC;";
        values.add(idCampaign).add(idStructure).add(user.getUserId());

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getBasketOrder(Integer idBasketOrder, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id = ?";
        values.add(idBasketOrder);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getBasketsOrders(Integer idCampaign, Handler<Either<String, JsonArray>> handler, UserInfos user) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id_campaign = ? AND id_structure IN (";
        values.add(idCampaign);

        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";
        query += " ORDER BY basket_order.id DESC;";

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getMyBasketOrders(UserInfos user, Integer page, Integer id_campaign, String startDate, String endDate, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT distinct b.* FROM " + Crre.crreSchema + ".basket_order b " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment_old oce on (oce.id_basket = b.id) " +
                "WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? " +
                "ORDER BY b.id DESC ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(id_campaign);
        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
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
        } else {
            sqlquery += "AND (bo.name ~* '' OR bo.name_user ~* '')";
        }
        
        sqlquery += " AND bo.id_structure IN ( ";
        filterBasketSearch(filters, user, arrayResponseHandler, values, sqlquery, page);
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
        filterBasketSearch(filters, user, arrayResponseHandler, values, sqlquery, page);
    }

    public void searchWithAll(String query, JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
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
        sqlquery += " AND bo.id_structure IN ( ";
        filterBasketSearch(filters, user, arrayResponseHandler, values, sqlquery, page);
    }

    private void filterBasketSearch(JsonArray filters, UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler,
                                    JsonArray values, String sqlquery, Integer page) {
        sqlquery = filterSQLTable(filters, user, values, sqlquery);
        sqlquery += " ORDER BY bo.id DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    static String filterSQLTable(JsonArray filters, UserInfos user, JsonArray values, String sqlquery) {
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(0).fieldNames().toString().length() -1);
                String value = filters.getJsonObject(i).getString(key);
                sqlquery += !key.equals("reassort") ? "bo." + key + " = " + "?" : "oe." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        return sqlquery;
    }
    

    private JsonArray getObjects(JsonObject basket) {
        JsonArray params;
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(basket.getInteger("id_order"))
                .add(basket.getInteger("amount"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add("WAITING")
                .add(basket.getString("id_equipment"))
                .add(basket.getString("comment"));
        return params;
    }

}
