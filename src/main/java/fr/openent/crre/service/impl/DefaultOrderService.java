package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private final Integer PAGE_SIZE = 15;

    public DefaultOrderService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void listOrder(Integer idCampaign, String idStructure, UserInfos user, List<String> ordersId, String startDate,
                          String endDate, boolean oldTable, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String selectOld = oldTable ? ", oe.equipment_image as image, oe.equipment_name as name, s.name as status_name, " +
                "s.id as status_id, oe.equipment_price as price, oe.equipment_format as type, oe.offers as offers " : "";
        String query = "SELECT oe.equipment_key, oe.id as id, oe.comment, oe.amount,to_char(oe.creation_date, 'dd-MM-yyyy') creation_date, " +
                "oe.id_campaign, oe.status, oe.cause_status, oe.id_structure, oe.id_basket, oe.reassort, " +
                "array_to_json(array_agg(DISTINCT order_file.*)) as files, ore.status as region_status " + selectOld +
                "FROM " + Crre.crreSchema + ((oldTable) ? ".order_client_equipment_old oe " : ".order_client_equipment  oe ") +
                "LEFT JOIN " + Crre.crreSchema + ".order_file ON oe.id = order_file.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ((oldTable) ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore ON ore.id_order_client_equipment = oe.id " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign ON oe.id_campaign = campaign.id ";

        if (oldTable) {
            query += "LEFT JOIN  " + Crre.crreSchema + ".status AS s ON s.id = ore.id_status ";
        }

        query += "WHERE oe.creation_date BETWEEN ? AND ? AND oe.id_campaign = ? AND oe.id_structure = ? ";
        values.add(startDate).add(endDate).add(idCampaign).add(idStructure);

        if (user != null) {
            query += "AND user_id = ? ";
            values.add(user.getUserId());
        }
        if (ordersId != null) {
            query += "AND oe.id_basket IN " + Sql.listPrepared(ordersId.toArray());
            for (String id : ordersId) {
                values.add(Integer.parseInt(id));
            }
        }
        query += " GROUP BY ( oe.id, campaign.priority_enabled, ore.status" + ((oldTable) ? ", s.name, s.id) " : ") ") +
                "ORDER BY CASE WHEN campaign.priority_enabled = false " +
                "THEN oe.creation_date END ASC";

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }

    @Override
    public void listOrder(String status, Integer page, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.* , bo.name as basket_name, bo.name_user as user_name, to_json(campaign.* ) campaign,  " +
                "array_to_json(array_agg( distinct structure_group.name)) as structure_groups, " +
                "array_to_json(array_agg(DISTINCT order_file.*)) as files, ore.status as region_status " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo " +
                "ON bo.id = oce.id_basket " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".order_file ON oce.id = order_file.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" ore ON oce.id = ore.id_order_client_equipment ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, user, query, startDate, endDate, values);

        query += "GROUP BY (bo.name, bo.name_user, oce.id, campaign.id, ore.status) " +
                "ORDER BY oce.creation_date DESC ";
        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listOrderAmount(String status, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT sum(oce.amount) as nb_licences " +
                "FROM crre.order_client_equipment oce " +
                "LEFT JOIN crre.campaign c on (oce.id_campaign = c.id) " +
                "LEFT JOIN crre.basket_order bo on (oce.id_basket = bo.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, user, query, startDate, endDate, values);
        query += "AND (c.use_credit = 'consumable_licences' OR c.use_credit = 'licences');";
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listOrderCredit(String status, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.equipment_key, oce.amount " +
                "FROM crre.order_client_equipment oce " +
                "LEFT JOIN crre.campaign c on (oce.id_campaign = c.id) " +
                "LEFT JOIN crre.basket_order bo on (oce.id_basket = bo.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, user, query, startDate, endDate, values);
        query += "AND c.use_credit = 'credits';";
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listUsers(String status, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT(bo.name_user) as user_name, bo.id_user " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment oce ON bo.id = oce.id_basket ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, user, query, null, null, values);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    private String filterWaitingOrder(String status, UserInfos user, String query, String startDate, String endDate, JsonArray values) {
        query += "WHERE ";
        if (startDate != null || endDate != null) {
            query += "oce.creation_date BETWEEN ? AND ? AND ";
            values.add(startDate).add(endDate);
        }
        query += "oce.id_structure IN ( ";
        StringBuilder queryBuilder = new StringBuilder(query);
        for (String idStruct : user.getStructures()) {
            queryBuilder.append("?,");
            values.add(idStruct);
        }
        query = queryBuilder.toString();
        query = query.substring(0, query.length() - 1) + ") ";
        if (!status.contains("ALL")) {
            query += " AND oce.status = ? ";
            values.add(status);
        }
        return query;
    }

    @Override
    public void updateAmount(Integer id, Integer amount, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String queryUpdateBasket = " UPDATE " + Crre.crreSchema + ".basket_order AS bo " +
                " SET amount = bo.amount + (? - oe.amount) " +
                " FROM " + Crre.crreSchema + ".order_client_equipment AS oe " +
                " WHERE (bo.id = oe.id_basket and oe.id = ?);";
        String queryUpdateOe = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET amount = ? " +
                " WHERE id = ?; ";
        values.add(amount).add(id).add(amount).add(id);

        sql.prepared(queryUpdateBasket + queryUpdateOe, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateReassort(Integer id, Boolean reassort, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET reassort = ? " +
                " WHERE id = ?; ";
        values.add(reassort).add(id);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }


    @Override
    public void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET comment = ? " +
                " WHERE id = ?; ";
        values.add(comment).add(id);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }

    @Override
    public void listExport(List<Integer> idsOrders, UserInfos user, String idCampaign, String statut, String startDate, String endDate, boolean oldTable, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT oce.*, bo.name as basket_name " +
                "FROM " + Crre.crreSchema + ((oldTable) ? ".order_client_equipment_old oce " : ".order_client_equipment  oce ") +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oce.id_basket) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        if (statut != null) {
            query = filterWaitingOrder("WAITING", user, query, startDate, endDate, values);
        } else {
            query += "WHERE oce.id_campaign = ? AND oce.user_id = ? AND oce.creation_date BETWEEN ? AND ? ";
            values.add(idCampaign).add(user.getUserId()).add(startDate).add(endDate);
        }
        if (idsOrders.size() > 0) {
            query += " AND oce.id IN ( ";
            for (int id : idsOrders) {
                query += "?,";
                values.add(id);
            }
            query = query.substring(0, query.length() - 1) + ")";
        }
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void rejectOrders(List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET status = 'REJECTED' " +
                " WHERE id in " + Sql.listPrepared(ids.toArray()) + " ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void validateOrders(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET  status = VALID " +
                " WHERE id in " + Sql.listPrepared(ids.toArray()) + " ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void setInProgress(JsonArray ids, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET  " +
                "status = ? " +
                "WHERE id IN " +
                Sql.listPrepared(ids.getList()) +
                " RETURNING id";

        values.add("IN PROGRESS");
        for (int i = 0; i < ids.size(); i++) {
            values.add(ids.getInteger(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }

    public void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount, oe.id as id, tc.name as type_name " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign c ON (c.id = oe.id_campaign) " +
                "LEFT JOIN " + Crre.crreSchema + ".type_campaign tc ON (tc.id = c.id_type) " +
                "WHERE oe.creation_date BETWEEN ? AND ? ";
        values.add(startDate).add(endDate);
        if (id_campaign != null) {
            sqlquery += "AND oe.id_campaign = ? ";
            values.add(id_campaign);
        }

        sqlquery = DefaultBasketService.SQLConditionQueryEquipments(query, equipTab, values, false, sqlquery);

        sqlquery += ") AND oe.status = 'WAITING' AND oe.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        if (filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = (String) filters.getJsonObject(i).fieldNames().toArray()[0];
                JsonArray value = filters.getJsonObject(i).getJsonArray(key);
                if (key.equals("id_user")) {
                    sqlquery += "bo." + key + " IN ( ";
                } else {
                    sqlquery += "oe." + key + " IN ( ";
                }
                for (int k = 0; k < value.size(); k++) {
                    sqlquery += "?,";
                    values.add(value.getString(k));
                }
                sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
                if (!(i == filters.size() - 1)) {
                    sqlquery += " AND ";
                } else {
                    sqlquery += ")";
                }
            }
        }

        sqlquery += " ORDER BY creation_date DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

}

