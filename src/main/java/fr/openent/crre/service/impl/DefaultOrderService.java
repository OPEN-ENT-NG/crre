package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.filter_waiting;
import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private final Integer PAGE_SIZE = 15;

    public DefaultOrderService(String schema, String table){
        super(schema,table);
    }

    @Override
    public void listOrder(Integer idCampaign, String idStructure, UserInfos user, List<String> ordersId, String startDate,
                          String endDate,  Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.equipment_key, oe.id as id, oe.comment, oe.amount,to_char(oe.creation_date, 'dd-MM-yyyy') creation_date, " +
                "oe.id_campaign, oe.status, oe.cause_status, oe.id_structure, oe.id_basket, oe.reassort, " +
                "array_to_json(array_agg(DISTINCT order_file.*)) as files, ore.status as region_status " +
                "FROM "+ Crre.crreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN " + Crre.crreSchema + ".order_file ON oe.id = order_file.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" ore ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign ON oe.id_campaign = campaign.id " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND oe.id_campaign = ? AND oe.id_structure = ? ";
        values.add(startDate).add(endDate).add(idCampaign).add(idStructure);

        if(user != null){
            query += "AND user_id = ? ";
            values.add(user.getUserId());
        }
        if(ordersId != null){
            query += "AND oe.id_basket IN " + Sql.listPrepared(ordersId.toArray());
            for (String id : ordersId) {
                values.add(Integer.parseInt(id));
            }
        }
        query +=" GROUP BY ( oe.id, campaign.priority_enabled, ore.status) " +
                "ORDER BY CASE WHEN campaign.priority_enabled = false " +
                "THEN oe.creation_date END ASC";

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }

    @Override
    public  void listOrder(String status, Integer page, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> handler){
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
        sql.prepared(query, values , SqlResult.validResultHandler(handler));
    }

    @Override
    public  void listUsers(String status, UserInfos user,  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT DISTINCT(bo.name_user) as user_name, bo.id_user "+
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment oce ON bo.id = oce.id_basket ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, user, query, null, null, values);
        sql.prepared(query, values , SqlResult.validResultHandler(handler));
    }

    private String filterWaitingOrder(String status, UserInfos user, String query, String startDate, String endDate, JsonArray values) {
        query += "WHERE ";
        if(startDate != null || endDate != null) {
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
        String query = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET amount = ? " +
                " WHERE id = ?; ";
        values.add(amount).add(id);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
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
    public void getFile(Integer orderId, String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Crre.crreSchema + ".order_file WHERE id = ? AND id_order_client_equipment = ?";
        JsonArray params = new JsonArray()
                .add(fileId)
                .add(orderId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue().getJsonObject(0)));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }


    @Override
    public void listExport(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT oe.amount, oe.creation_date, oe.status, oe.equipment_key, oe.comment, bo.name as basket_name " +
                "FROM "+ Crre.crreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN "+ Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.id IN ( ";

        for (int id : idsOrders) {
            query += "?,";
            values.add(id);
        }
        query = query.substring(0, query.length() - 1) + ")";
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void rejectOrders(List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET status = 'REJECTED' " +
                " WHERE id in " + Sql.listPrepared(ids.toArray()) +" ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void validateOrders(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET  status = VALID " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT oce.id, oce.id_structure as idStructure, campaign.name as namecampaign, oce.amount as qty," +
                " oce.creation_date as date" +
                "FROM "+ Crre.crreSchema +".order_client_equipment  oce "+
                "INNER JOIN "+ Crre.crreSchema +".campaign ON campaign.id = oce.id_campaign "+
                "WHERE oce.id in "+ Sql.listPrepared(idsOrders.toArray()) +
                " GROUP BY oce.id, idStructure, qty,date, oce.id_campaign, namecampaign ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for(Integer id : idsOrders){
            params.add(id);
        }

        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    @Override
    public void updateStatusOrder(Integer idOrder, JsonObject orderStatus, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET status = ? " +
                "WHERE id = " +
                idOrder +
                " RETURNING id";

        values.add(orderStatus.getString("status"));
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT oec.* , array_to_json ( array_agg ( campaign.*)) as campaign " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oec " +
                "INNER JOIN " + Crre.crreSchema + ".campaign on oec.id_campaign =  campaign.id " +
                "where oec.id = ? " +
                "group by oec.id";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
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

    @Override
    public void searchWithoutEquip(String query, JsonArray filters, UserInfos user, Integer id_campaign, String startDate, String endDate, Integer page,
                                   Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.id as id " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND ";
        values.add(startDate).add(endDate);
        if(id_campaign != null){
            sqlquery += "oe.id_campaign = ? AND ";
            values.add(id_campaign);
        }
        sqlquery += "oe.status = 'WAITING' AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ?) AND oe.id_structure IN (";

        values.add(query);
        values.add(query);
        orderPaginationSQL(filters, user, page, arrayResponseHandler, values, sqlquery);
    }

    public void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount, oe.id as id " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND oe.status = 'WAITING' ";
        values.add(startDate).add(endDate);

        if(id_campaign != null){
            sqlquery += "AND oe.id_campaign = ? ";
            values.add(id_campaign);
        }

        if (!query.equals("")) {
            sqlquery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_key IN (";
            values.add(query);
            values.add(query);
        } else {
            sqlquery += "AND (oe.equipment_key IN (";
        }

        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getString("ean"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery += ") AND oe.id_structure IN ( ";
        orderPaginationSQL(filters, user, page, arrayResponseHandler, values, sqlquery);
    }

    public void searchWithAll(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, String startDate, String endDate, Integer page,
                              Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount, oe.id as id " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND oe.status = 'WAITING' ";
        values.add(startDate).add(endDate);
        if(id_campaign != null){
            sqlquery += "AND oe.id_campaign = ? ";
            values.add(id_campaign);
        }
        sqlquery = queryFilterSQL(query, equipTab, values, sqlquery);

        sqlquery += " AND oe.id_structure IN ( ";
        orderPaginationSQL(filters, user, page, arrayResponseHandler, values, sqlquery);
    }

    static String queryFilterSQL(String query, JsonArray equipTab, JsonArray values, String sqlquery) {
        values.add(query);
        values.add(query);
        if(equipTab.getJsonArray(1).isEmpty()){
            sqlquery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ?) ";
        } else {
            sqlquery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_key IN (";
            sqlquery = insertValuesQuery(equipTab, values, sqlquery);
        }

        sqlquery += " AND oe.equipment_key IN (";
        sqlquery = insertEquipmentEAN(equipTab, values, sqlquery);
        return sqlquery;
    }

    static String insertEquipmentEAN(JsonArray equipTab, JsonArray values, String sqlquery) {
        for (int i = 0; i < equipTab.getJsonArray(0).size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonArray(0).getJsonObject(i).getString("ean"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        return sqlquery;
    }

    static String insertValuesQuery(JsonArray equipTab, JsonArray values, String sqlquery) {
        for (int i = 0; i < equipTab.getJsonArray(1).size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonArray(1).getJsonObject(i).getString("ean"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery += ")";
        return sqlquery;
    }

    public void filter(JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount, oe.id as id " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND oe.status = 'WAITING' ";
        values.add(startDate).add(endDate);
        if(id_campaign != null){
            sqlquery += "AND oe.id_campaign = ? ";
            values.add(id_campaign);
        }
        sqlquery = filterSQLTable(equipTab, values, sqlquery);
        orderPaginationSQL(filters, user, page, arrayResponseHandler, values, sqlquery);
    }

    private void orderPaginationSQL(JsonArray filters, UserInfos user, Integer page,
                                    Handler<Either<String, JsonArray>> arrayResponseHandler, JsonArray values, String sqlquery) {
        sqlquery = DefaultBasketService.filterSQLTable(filters, user, values, sqlquery);
        sqlquery += " ORDER BY creation_date DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    static String filterSQLTable(JsonArray equipTab, JsonArray values, String sqlquery) {
        sqlquery += " AND oe.equipment_key IN (";
        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getString("ean"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += " AND oe.id_structure IN ( ";
        return sqlquery;
    }

    public void searchName(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearchName(word, handler);
    }

    @Override
    public void filterGrade(List<String> filter, String query, Handler<Either<String, JsonArray>> handler) {
        if(StringUtils.isEmpty(query)) {
            filter_waiting(filter, null, handler);
        } else {
            filter_waiting(filter, query, handler);
        }
    }
}

