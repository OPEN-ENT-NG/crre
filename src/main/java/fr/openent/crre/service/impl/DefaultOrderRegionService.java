package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);
    }

    @Override
    public void createOrdersRegion(JsonObject order, UserInfos user, Number id_project, Handler<Either<String, JsonObject>> handler) {
        String queryOrderRegionEquipment = "" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment\" ";
            queryOrderRegionEquipment += " ( amount, creation_date,  owner_name, owner_id," +
                    " status, equipment_key, id_campaign, id_structure," +
                    " comment, id_order_client_equipment, id_project, reassort) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(order.getString("user_name"))
                .add(order.getString("user_id"))
                .add("IN PROGRESS")
                .add(order.getString("equipment_key"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"))
                .add(id_project)
                .add(order.getBoolean("reassort"));
        Sql.getInstance().prepared(queryOrderRegionEquipment, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void createProject( String title, Handler<Either<String, JsonObject>> handler) {
        JsonArray params;

        String queryProjectEquipment = "" +
                    "INSERT INTO " + Crre.crreSchema + ".project " +
                "( title ) VALUES " +
                "( ? )  RETURNING id ;";
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(title);

        Sql.getInstance().prepared(queryProjectEquipment, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "DELETE FROM " +
                Crre.crreSchema + ".\"order-region-equipment\" " +
                "WHERE id = ? " +
                "RETURNING id";
        sql.prepared(query, new JsonArray().add(idOrderRegion), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOneOrderRegion(int idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(tt.*) AS title, " +
                "       to_json(oce.*) AS order_parent " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.status = 'IN PROGRESS' AND ore.id = ? " +
                "GROUP BY ( prj.id, " +
                "          ore.id, " +
                "          campaign.id, " +
                "          tt.id, " +
                "          oce.id )";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAllOrderRegion(Handler<Either<String, JsonArray>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       to_json(campaign.*) campaign, " +
                "       p.title AS title, " +
                "       to_json(oce.*) AS order_parent, " +
                "       bo.name AS basket_name " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.status = 'IN PROGRESS'  ";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getAllOrderRegionByProject(int idProject, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "" +
                "SELECT ore.*, " +
                "       to_json(campaign.*) campaign, " +
                "       campaign.name AS campaign_name, " +
                "       p.title AS title, " +
                "       to_json(oce.*) AS order_parent, " +
                "       bo.name AS basket_name " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.id_project = ? ";
        Sql.getInstance().prepared(query, new JsonArray().add(idProject), SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getOrdersRegionById(List<Integer> idsOrder, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "" +
                "SELECT ore.*, " +
                "       to_json(campaign.*) campaign, " +
                "       campaign.name AS campaign_name, " +
                "       p.title AS title, " +
                "       to_json(oce.*) AS order_parent, " +
                "       bo.name AS basket_name " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.id in " + Sql.listPrepared(idsOrder.toArray()) + " ; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : idsOrder) {
            params.add( id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getAllProjects(UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder("" +
                "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id ");
        if(!WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString()) &&
                WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString())){
            query.append("WHERE ore.id_structure IN ( ");
            for (String idStruct : user.getStructures()) {
                query.append("?,");
                values.add(idStruct);
            }
            query = new StringBuilder(query.substring(0, query.length() - 1) + ")");
        }
        query.append(" ORDER BY p.id DESC ");
        sql.prepared(query.toString(), values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void search(String query, UserInfos user, JsonArray equipTab, Handler<Either<String, JsonArray>> arrayResponseHandler) {
            JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
            String sqlquery = "SELECT DISTINCT (p.*) " +
                    "FROM  " + Crre.crreSchema + ".project p " +
                    "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                    "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oe ON oe.id = ore.id_order_client_equipment " +
                    "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                    "WHERE (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_key IN ( ";

            values.add(query);
            values.add(query);
            values.add(query);

            for (int i = 0; i < equipTab.size(); i++) {
                sqlquery += "?,";
                values.add(equipTab.getJsonObject(i).getString("ean"));
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

            sqlquery += ") AND ore.id_structure IN ( ";
            for (String idStruct : user.getStructures()) {
                sqlquery += "?,";
                values.add(idStruct);
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
            sqlquery = sqlquery + " ORDER BY p.id DESC ";
            sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
        }

    @Override
    public void searchWithoutEquip(String query, UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? ";

        values.add(query);
        values.add(query);
        values.add(query);

        sqlquery += ") AND ore.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery = sqlquery + " ORDER BY p.id DESC";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void searchName(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearchName(word, handler);
    }

    public void filter(UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND ore.id_structure IN ( ";
        values.add(startDate);
        values.add(endDate);
        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";
        query = query + " ORDER BY p.id DESC ";
        sql.prepared(query, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void filterSearch(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, JsonArray filters, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ? ";

        values.add(startDate);
        values.add(endDate);
        if (query != "") {
            sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_key IN (";
            values.add(query);
            values.add(query);
            values.add(query);
        } else {
            sqlquery += "AND (p.title ~* p.title OR ore.owner_name ~* ore.owner_name OR b.name ~* b.name OR ore.equipment_key IN (";
        }


        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getString("ean"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += ") AND ore.id_structure IN ( ";
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
                sqlquery += !key.equals("reassort") ? "b." + key + " = " + "?" : "ore." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery = sqlquery + " ORDER BY p.id DESC ";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void filterSearchWithoutEquip(UserInfos user, String query, String startDate, String endDate, JsonArray filters, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? ";

        values.add(startDate);
        values.add(endDate);
        values.add(query);
        values.add(query);
        values.add(query);

        sqlquery += ") AND ore.id_structure IN ( ";
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
                sqlquery += !key.equals("reassort") ? "b." + key + " = " + "?" : "ore." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery = sqlquery + " ORDER BY p.id DESC ";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT p.title " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "WHERE ore.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";
        query = query + " ORDER BY p.id DESC LIMIT 1";
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(arrayResponseHandler));
    }

    @Override
    public void  updateOrders(List<Integer> ids, String status, String justification, final Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " SET  status = ?, cause_status = ?" +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ; ";

        query += "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET  status = ?, cause_status = ? " +
                "WHERE id in ( SELECT ore.id_order_client_equipment FROM " + Crre.crreSchema + ".\"order-region-equipment\" ore " +
                "WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ) ; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add( id);
        }
        params.add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add( id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
