package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.BasketOrder;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.BasketOrderService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

public class DefaultBasketOrderService implements BasketOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketOrderService.class);

    private final Integer PAGE_SIZE = 15;

    public DefaultBasketOrderService() {
    }

    @Override
    public Future<List<BasketOrder>> getMyBasketOrders(String userId, Integer page, Integer idCampaign, String idStructure,
                                                       String startDate, String endDate, boolean oldTable) {
        Promise<List<BasketOrder>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "SELECT distinct b.* FROM " + Crre.crreSchema + ".basket_order b " +
                "INNER JOIN " + Crre.crreSchema + "." + (oldTable ? "order_client_equipment_old" : "order_client_equipment") + " oce on (oce.id_basket = b.id) " +
                "WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? ";
        values.add(startDate).add(endDate).add(userId).add(idCampaign);
        if (idStructure != null) {
            query += "AND b.id_structure = ? ";
            values.add(idStructure);
        }

        query += "ORDER BY b.id DESC ";

        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        String errorMessage = String.format("[CRRE@%s::getMyBasketOrders] Fail to get user basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrder>> search(String query, UserInfos user, JsonArray equipTab, int idCampaign, String idStructure,
                                            String startDate, String endDate, Integer page, Boolean old) {
        Promise<List<BasketOrder>> promise = Promise.promise();
        if (user.getStructures().isEmpty() && idStructure == null) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        JsonArray values = new JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oe ON (bo.id = oe.id_basket) " +
                "WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ? ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(idCampaign);

        sqlquery = SQLConditionQueryEquipments(query, equipTab, values, old, sqlquery);

        if (idStructure != null) {
            sqlquery += ") AND bo.id_structure = ? ";
            values.add(idStructure);
        } else {
            sqlquery += ") AND bo.id_structure IN ( ";
            for (String idStruct : user.getStructures()) {
                sqlquery += "?,";
                values.add(idStruct);
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        }


        sqlquery += " ORDER BY bo.id DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        String errorMessage = String.format("[CRRE@%s::search] Fail to search basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(sqlquery, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class), errorMessage));

        return promise.future();
    }

    static String SQLConditionQueryEquipments(String query, JsonArray equipTab, JsonArray values, Boolean old, String sqlQuery) {
        if (!old) {
            if (!query.equals("")) {
                sqlQuery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? ";
                values.add(query);
                values.add(query);
            }
            if (!equipTab.isEmpty()) {
                if (!query.equals("")) {
                    sqlQuery += "OR ";
                } else {
                    sqlQuery += "AND (";
                }
                sqlQuery += "oe.equipment_key IN (";
                for (int i = 0; i < equipTab.size(); i++) {
                    sqlQuery += "?,";
                    values.add(equipTab.getJsonObject(i).getString("ean"));
                }
                sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 1) + ")";
            }
        } else {
            sqlQuery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_name ~* ?";
            values.add(query);
            values.add(query);
            values.add(query);
        }
        return sqlQuery;
    }

    @Override
    public TransactionElement getTransactionInsertBasketName(UserInfos user, String idStructure, Integer idCampaign, String basketName, double total, int amount) {

        String insertBasketNameQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_order(" +
                        "name, id_structure, id_campaign, name_user, id_user, total, amount, created)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) returning id;";
        JsonArray params = new JsonArray()
                .add(basketName)
                .add(idStructure)
                .add(idCampaign)
                .add(user.getUsername())
                .add(user.getUserId())
                .add(total)
                .add(amount);


        return new TransactionElement(insertBasketNameQuery, params);
    }

    @Override
    public Future<List<BasketOrder>> getBasketOrderList(List<Integer> basketIdList) {
        Promise<List<BasketOrder>> promise = Promise.promise();

        String selectQuery = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id IN " + Sql.listPrepared(basketIdList) + ";";
        String errorMessage = String.format("[CRRE@%s::getBasketOrderList] Fail to get basket order", this.getClass().getSimpleName());
        Sql.getInstance().prepared(selectQuery, new JsonArray(basketIdList),
                SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrder>> getBasketOrderListByOrderRegion(List<Integer> orderRegionIdList) {
        Promise<List<BasketOrder>> promise = Promise.promise();
        String selectQuery = "SELECT DISTINCT(bo.*) " +
                "FROM " + Crre.crreSchema + ".basket_order AS bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS o_c_e ON (bo.id = o_c_e.id_basket) " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS o_c_e_o ON (bo.id = o_c_e_o.id_basket) " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS o_r_e ON (o_c_e.id = o_r_e.id_order_client_equipment) " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" o_r_e_o ON (o_c_e_o.id = o_r_e_o.id_order_client_equipment) " +
                "WHERE o_r_e.id IN " + Sql.listPrepared(orderRegionIdList) +
                "OR " + "o_r_e_o.id IN " + Sql.listPrepared(orderRegionIdList) + ";";
        JsonArray params = new JsonArray(new ArrayList<>(orderRegionIdList));
        params.addAll(new JsonArray(orderRegionIdList));
        String errorMessage = String.format("[CRRE@%s::getBasketOrderListByOrderRegion] Fail to get basket order", this.getClass().getSimpleName());
        Sql.getInstance().prepared(selectQuery, params,
                SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class, errorMessage)));

        return promise.future();
    }
}
