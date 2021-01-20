package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderRegionService;
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

import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderRegionService.class);
    @Override
    public void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "INSERT INTO  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "(price, " +
                "amount, " +
                "creation_date, " +
                "owner_name, " +
                "owner_id, " +
                "equipment_key, " +
                "name, " +
                "comment, " +
                "id_order_client_equipment, " +
                "rank, ";
        query += "status, " +
                "id_campaign, " +
                "id_structure, " +
                "summary, " +
                "description, " +
                "image, " +
                "technical_spec, " +
                "id_contract, " +
                "cause_status, " +
                "number_validation, " +
                "id_order, " +
                "id_project) ";

        query += "SELECT " +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ,";
        query += order.getInteger("rank") != -1 ? "?, " : "NULL, ";
        query += " 'IN PROGRESS', " +
                "       id_campaign, " +
                "       id_structure, " +
                "       summary, " +
                "       description, " +
                "       image, " +
                "       technical_spec, " +
                "       ? as id_contract, " +
                "       cause_status, " +
                "       number_validation, " +
                "       id_order, " +
                "       id_project " +
                "FROM  " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE id = ? " +
                "RETURNING id;";

        params.add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getInteger("equipment_key"))
                .add(order.getString("name"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        params.add(order.getInteger("id_contract"));
        params.add(order.getInteger("id_order_client_equipment"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateOrderRegion(JsonObject order, int idOrder, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "UPDATE " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " SET " +
                "price = ?, " +
                "amount = ?, " +
                "modification_date = ? , " +
                "owner_name = ? , " +
                "owner_id = ?, " +
                "name = ?, " +
                "equipment_key = ?, " +
                "cause_status = 'IN PROGRESS', ";

        query += order.getInteger("rank") != -1 ? "rank=?," : "rank = NULL, ";
        query += order.containsKey("id_contract") ? "id_contract = ?, " : "";
        query += "comment = ? " +
                "WHERE id = ?" +
                "RETURNING id;";

        params.add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getInteger("equipment_key"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_contract")) {
            params.add(order.getInteger("id_contract"));
        }
        params.add(order.getString("comment"))
                .add(idOrder);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createOrdersRegion(JsonObject order, UserInfos user, Number id_project, Handler<Either<String, JsonObject>> handler) {
        JsonArray params;
        final double cons = 100.0;
        String updatePurseQuery = "UPDATE  " + Crre.crreSchema + ".purse " +
                "SET amount = amount - ?  " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? ;";

        String queryOrderRegionEquipment = "" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment\" ";

        if (order.getInteger("rank") != -1) {
            queryOrderRegionEquipment += " ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment, id_order_client_equipment,  id_project, rank) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        } else {
            queryOrderRegionEquipment += " ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment, id_order_client_equipment, id_project) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        }

        params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round((order.getDouble("price") * cons)/cons))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(order.getString("user_name"))
                .add(order.getString("user_id"))
                .add(order.getString("name"))
                .add(order.getString("summary"))
                .add(order.getString("description"))
                .add(order.getString("image"))
                .add(order.getJsonArray("technical_specs"))
                .add("IN PROGRESS")
                .add(order.getInteger("id_contract"))
                .add(order.getInteger("equipment_key"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"))
                .add(id_project);
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        Sql.getInstance().prepared(updatePurseQuery + queryOrderRegionEquipment, params, SqlResult.validUniqueResultHandler(handler));
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
                "       ore.price AS price_single_ttc, " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(tt.*) AS title, " +
                "       to_json(oce.*) AS order_parent " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN  " + Crre.crreSchema + ".contract ON ore.id_contract = contract.id " +
                "INNER JOIN  " + Crre.crreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.status = 'IN PROGRESS' AND ore.id = ? " +
                "GROUP BY ( prj.id, " +
                "          ore.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          campaign.id, " +
                "          tt.id, " +
                "          oce.id )";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAllOrderRegion(Handler<Either<String, JsonArray>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       ore.price AS price_single_ttc, " +
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
                "       ore.price AS price_single_ttc, " +
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
                "WHERE ore.status = 'IN PROGRESS' and ore.id_project = ? ";
        Sql.getInstance().prepared(query, new JsonArray().add(idProject), SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getAllProjects(UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "WHERE ore.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";
        query = query + " ORDER BY p.id DESC ";
        sql.prepared(query, values, SqlResult.validResultHandler(arrayResponseHandler));
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
                values.add(equipTab.getJsonObject(i).getInteger("id"));
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
    public void filterSearch(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT DISTINCT (p.*) " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_key IN ( ";

        values.add(startDate);
        values.add(endDate);
        values.add(query);
        values.add(query);
        values.add(query);

        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getInteger("id"));
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
    public void filterSearchWithoutEquip(UserInfos user, String query, String startDate, String endDate, Handler<Either<String, JsonArray>> arrayResponseHandler) {
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
        sqlquery = sqlquery + " ORDER BY p.id DESC ";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }


}
