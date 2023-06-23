package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
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
import org.entcore.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultBasketOrderService implements BasketOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketOrderService.class);

    private final Integer PAGE_SIZE = 15;

    public DefaultBasketOrderService() {
    }

    @Override
    public Future<List<BasketOrder>> getMyBasketOrders(String userId, Integer page, Integer idCampaign, String idStructure,
                                                       String startDate, String endDate, List<OrderStatus> statusList) {
        Promise<List<BasketOrder>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        StringBuilder query = new StringBuilder().append("SELECT distinct b.* FROM ").append(Crre.crreSchema).append(".basket_order b ")
                .append("INNER JOIN ").append(Crre.crreSchema).append(".order_universal as o_u on (o_u.id_basket = b.id) ")
                .append("WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? ");
        values.add(startDate).add(endDate).add(userId).add(idCampaign);
        if (idStructure != null) {
            query.append("AND b.id_structure = ? ");
            values.add(idStructure);
        }

        if (!statusList.isEmpty()) {
            query.append("AND o_u.status IN ").append(Sql.listPrepared(statusList)).append(" ");
            values.addAll(new JsonArray(statusList.stream().map(Enum::name).collect(Collectors.toList())));
        }

        query.append("ORDER BY b.id DESC ");

        if (page != null) {
            query.append("OFFSET ? LIMIT ? ");
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        String errorMessage = String.format("[CRRE@%s::getMyBasketOrders] Fail to get user basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrder>> search(String query, UserInfos user, List<String> equipementIdList, int idCampaign, String idStructure,
                                            String startDate, String endDate, Integer page, List<OrderStatus> statusList) {
        Promise<List<BasketOrder>> promise = Promise.promise();
        if (user.getStructures().isEmpty() && idStructure == null) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }

        JsonArray values = new JsonArray();
        StringBuilder sqlquery = new StringBuilder()
                .append("SELECT distinct bo.* FROM ")
                .append(Crre.crreSchema)
                .append(".basket_order bo ")
                .append("INNER JOIN ")
                .append(Crre.crreSchema)
                .append(".order_universal as o_u ON (bo.id = o_u.id_basket) ")
                .append("WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ? ");

        values.add(startDate).add(endDate).add(user.getUserId()).add(idCampaign);

        sqlquery.append(sqlConditionQueryEquipments(query, equipementIdList, values));

        if (idStructure != null) {
            sqlquery.append(" AND bo.id_structure = ? ");
            values.add(idStructure);
        } else {
            sqlquery.append(" AND bo.id_structure IN ").append(Sql.listPrepared(user.getStructures()));
            values.addAll(new JsonArray(user.getStructures()));
        }

        if (!statusList.isEmpty()) {
            sqlquery.append(" AND o_u.status IN ").append(Sql.listPrepared(statusList));
            values.addAll(new JsonArray(statusList.stream().map(Enum::name).collect(Collectors.toList())));
        }

        sqlquery.append(" ORDER BY bo.id DESC ");
        if (page != null) {
            sqlquery.append("OFFSET ? LIMIT ? ");
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        String errorMessage = String.format("[CRRE@%s::search] Fail to search basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(String.valueOf(sqlquery), values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class), errorMessage));

        return promise.future();
    }

    static String SQLConditionQueryEquipments(String query, List<String> equipementIdList, JsonArray values, Boolean old, String sqlQuery) {
        if (!old) {
            if (!query.equals("")) {
                sqlQuery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? ";
                values.add(query);
                values.add(query);
            }
            if (!equipementIdList.isEmpty()) {
                if (!query.equals("")) {
                    sqlQuery += "OR ";
                } else {
                    sqlQuery += "AND (";
                }
                sqlQuery += "oe.equipment_key IN (";

                for (String equipId : equipementIdList) {
                    sqlQuery += "?,";
                    values.add(equipId);
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

    public String sqlConditionQueryEquipments(String query, List<String> equipementIdList, JsonArray values) {
        // No filter
        if (StringUtils.isEmpty(query) && equipementIdList.isEmpty()) {
            return "";
        }

        // Query filter
        if (!StringUtils.isEmpty(query)) {

            String oldCondition = " o_u.status IN ('" + Arrays.stream(OrderStatus.values())
                    .filter(OrderStatus::isHistoricStatus)
                    .map(Enum::name)
                    .collect(Collectors.joining("', '")) + "')";
            String notOldCondition = " o_u.status IN ('" + Arrays.stream(OrderStatus.values())
                    .filter(orderStatus -> !orderStatus.isHistoricStatus())
                    .map(Enum::name)
                    .collect(Collectors.joining("', '")) + "')";

            values.add(query)
                    .add(query)
                    .add(query);
            String equipmentCondition;
            if (equipementIdList.isEmpty()) {
                equipmentCondition = "TRUE";
            } else {
                equipmentCondition = "o_u.equipment_key IN " + Sql.listPrepared(equipementIdList);
                values.addAll(new JsonArray(new ArrayList<>(equipementIdList)));
            }
            return "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR ((" + oldCondition + " AND o_u.equipment_name ~* ?) OR (" + notOldCondition + " AND " + equipmentCondition + ")))";
        }

        // Equipment filter
        values.addAll(new JsonArray(new ArrayList<>(equipementIdList)));
        return "AND o_u.equipment_key IN " + Sql.listPrepared(equipementIdList);
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
        if (orderRegionIdList.isEmpty()) {
            return Future.succeededFuture(new ArrayList<>());
        }

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
