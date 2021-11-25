package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    private final Integer PAGE_SIZE = 10;

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

    public void createProject(String title, Handler<Either<String, JsonObject>> handler) {
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
    public void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT EXISTS(SELECT id FROM " +
                Crre.crreSchema + ".\"order-region-equipment\" " +
                "WHERE equipment_key = ? AND id_structure = ? AND owner_id = 'renew2021-2022' );";
        sql.prepared(query, new JsonArray().add(idEquipment).add(idStructure), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAllOrderRegionByProject(int idProject, boolean filterRejectedSentOrders, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = selectOrderRegion(old);
        query += "WHERE ore.id_project = ? AND ore.equipment_key IS NOT NULL ";
        if (filterRejectedSentOrders) {
            query += "AND ore.status != 'SENT' AND ore.status != 'REJECTED'";
        }
        query = groupOrderRegion(old, query);
        Sql.getInstance().prepared(query, new JsonArray().add(idProject), SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getOrdersRegionById(List<Integer> idsOrder, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = selectOrderRegion(oldTable);
        query += "WHERE ore.id in " + Sql.listPrepared(idsOrder.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : idsOrder) {
            params.add(id);
        }
        query = groupOrderRegion(oldTable, query);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(arrayResponseHandler));
    }

    private static String innerJoin(String query) {
        query += "INNER JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) ";
        return query;
    }

    @Override
    public void getAllProjects(UserInfos user, String startDate, String endDate, Integer page, boolean filterRejectedSentOrders,
                               String idStructure, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder("" +
                "SELECT DISTINCT (p.*), ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + (oldTable ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore ON ore.id_project = p.id " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND ore.equipment_key IS NOT NULL ");
        values.add(startDate);
        values.add(endDate);

        if (filterRejectedSentOrders) {
            query.append(" AND ore.status != 'SENT' AND ore.status != 'REJECTED' ");
        }

        if (!WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString()) &&
                WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString())) {
            query.append(" AND ore.id_structure = ?");
            values.add(idStructure);
        }
        query.append(" ORDER BY ore.creation_date DESC ");
        if (page != null) {
            query.append("OFFSET ? LIMIT ? ");
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query.toString(), values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    static void addValues(String key, String value, HashMap<String, ArrayList> hashMap) {
        ArrayList tempList;
        if (hashMap.containsKey(key)) {
            tempList = hashMap.get(key);
            if (tempList == null)
                tempList = new ArrayList();
            tempList.add(value);
        } else {
            tempList = new ArrayList();
            tempList.add(value);
        }
        hashMap.put(key, tempList);
    }

    public void search(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                       Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = "SELECT DISTINCT (p.*), ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + (old ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ?";
        values.add(startDate);
        values.add(endDate);
        if (!idStructure.equals("null")) {
            sqlquery += " AND ore.id_structure = ?";
            values.add(idStructure);
        }
        //condition with query
        if (!query.equals("")) {
            sqlquery += "AND (lower(p.title) ~* ? OR lower(ore.owner_name) ~* ? OR lower(b.name) ~* ? ";
            values.add(query);
            values.add(query);
            values.add(query);
            if (old) {
                sqlquery += " OR ore.equipment_name ~* ? ";
                values.add(query);
            }
        }
        //condition with equipment
        if (!equipTab.isEmpty() && !old) {
            if (!query.equals("")) {
                sqlquery += " OR ore.equipment_key IN (";
            } else {
                sqlquery += "AND (ore.equipment_key IN (";
            }
            for (int i = 0; i < equipTab.size(); i++) {
                sqlquery += "?,";
                values.add(equipTab.getJsonObject(i).getString("ean"));
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        }
        if (!query.equals("") || (!equipTab.isEmpty() && !old)) {
            sqlquery += ")";
        }

        if (filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = (String) filters.getJsonObject(i).fieldNames().toArray()[0];
                if (key.equals("id_structure")) {
                    JsonArray uai = filters.getJsonObject(i).getJsonArray(key);
                    for (int j = 0; j < uai.size(); j++) {
                        addValues(key, uai.getJsonObject(j).getString("idStructure"), hashMap);
                    }
                } else {
                    String value = filters.getJsonObject(i).getString(key);
                    addValues(key, value, hashMap);
                }
            }
            int count = 0;
            for (Map.Entry mapentry : hashMap.entrySet()) {
                ArrayList list = (ArrayList) mapentry.getValue();
                String keys = mapentry.getKey().toString();
                if (keys.equals("renew")) {
                    if (list.size() == 2) {
                        sqlquery += "ore.owner_id ~* ore.owner_id";
                    } else {
                        if (Boolean.parseBoolean(String.valueOf(list.get(0)))) {
                            sqlquery += "ore.owner_id ~* 'renew'";
                        } else {
                            sqlquery += "ore.owner_id !~* 'renew'";
                        }
                    }
                } else {
                    sqlquery += !(keys.equals("reassort") || keys.equals("status")) ? "b." + keys + " IN (" : "ore." + keys + " IN (";
                    for (int k = 0; k < list.size(); k++) {
                        sqlquery += k + 1 == list.size() ? "?)" : "?, ";
                        values.add(list.get(k).toString());
                    }
                }

                if (!(count == hashMap.entrySet().size() - 1)) {
                    sqlquery += " AND ";
                } else {
                    sqlquery += ")";
                }
                count++;
            }
        }
        sqlquery = sqlquery + " ORDER BY ore.creation_date DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        Sql.getInstance().prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getLastProject(Handler<Either<String, JsonObject>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT p.title, ore.creation_date, p.id " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" AS ore ON ore.id_project = p.id ";
        query = query + " ORDER BY p.id DESC LIMIT 1";
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(arrayResponseHandler));
    }

    @Override
    public void insertOldOrders(JsonArray orderRegions, boolean isRenew, Handler<Either<String, JsonObject>> handler) throws ParseException {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment-old\"" +
                " (" +
                ((isRenew) ? "" : "id,") +
                "amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_order_client_equipment, id_project, reassort, total_free) VALUES ";

        for (int i = 0; i < orderRegions.size(); i++) {
            if (orderRegions.getJsonObject(i).containsKey("id_project")) {
                query += "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
                JsonObject order = orderRegions.getJsonObject(i);
                String creation_date;
                if (isRenew) {
                    query += ") ,";
                    Date date = new SimpleDateFormat("dd/MM/yyyy").parse(order.getString("creation_date"));
                    creation_date = new SimpleDateFormat("yyyy-MM-dd").format(date);
                } else {
                    params.add(order.getLong("id"));
                    query += ", ?) ,";
                    Date date = new SimpleDateFormat("dd-MM-yyyy").parse(order.getString("creation_date"));
                    creation_date = new SimpleDateFormat("yyyy-MM-dd").format(date);
                }

                params.add(order.getInteger("amount"))
                        .add(creation_date)
                        .add(order.getString("owner_name"))
                        .add(order.getString("owner_id"))
                        .add(order.getString("status"))
                        .add(order.getString("equipment_key"));
                setOrderValuesSQL(params, order);
                params.add(order.getInteger("id_order_client_equipment"))
                        .add(order.getLong("id_project"))
                        .add(order.getBoolean("reassort"))
                        .add(order.getInteger("total_free", null));
            }
        }
        query = query.substring(0, query.length() - 1);
        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L), SqlResult.validUniqueResultHandler(handler));
    }

    private String selectOrderRegion(boolean old) {
        String selectOld = old ? ", ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers, s.name as status_name, s.id as status_id " : "";
        String query = "SELECT ore.*, to_json(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, p.title AS title, " +
                "to_json(oce.*) AS order_parent, bo.name AS basket_name " + selectOld +
                "FROM  " + Crre.crreSchema + (old ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore " +
                "LEFT JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oce ON ore.id_order_client_equipment = oce.id ";
        if (old) {
            query += "LEFT JOIN  " + Crre.crreSchema + ".status AS s ON s.id = ore.id_status " +
                    "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                    "LEFT JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id ";
        } else {
            query += "INNER JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                    "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id ";
        }
        query = innerJoin(query);
        return query;
    }

    private String groupOrderRegion(boolean old, String query) {
        query += "GROUP BY ore.id, campaign.name, campaign.use_credit, campaign.*, p.title, oce.id, bo.name, bo.id";
        if (old) {
            query += ", s.name, s.id";
        }
        return query;
    }

    private void setOrderValuesSQL(JsonArray params, JsonObject order) {
        params.add(order.getString("name"))
                .add(order.getString("image", null))
                .add(order.getDouble("unitedPriceTTC"))
                .add(order.getString("grade", null))
                .add(order.getString("editor"))
                .add(order.getString("diffusor"))
                .add(order.getString("type", null))
                .add(order.getInteger("id_campaign", null))
                .add(order.getString("id_structure", ""))
                .add(order.getString("comment"));
    }

    @Override
    public void insertOldClientOrders(JsonArray orderRegions, Handler<Either<String, JsonObject>> handler) throws ParseException {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                " INSERT INTO " + Crre.crreSchema + ".\"order_client_equipment_old\"" +
                " (id, amount, creation_date, user_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_basket, reassort, offers, equipment_tva5, equipment_tva20, equipment_priceht) VALUES ";

        for (int i = 0; i < orderRegions.size(); i++) {
            if (orderRegions.getJsonObject(i).containsKey("id_project")) {
                query += "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),";
                JsonObject order = orderRegions.getJsonObject(i);
                Date date = new SimpleDateFormat("dd-MM-yyyy").parse(order.getString("creation_date"));
                String creation_date = new SimpleDateFormat("yyyy-MM-dd").format(date);
                params.add(order.getLong("id_order_client_equipment"))
                        .add(order.getInteger("amount"))
                        .add(creation_date)
                        .add(order.getString("owner_id"))
                        .add(order.getString("status"))
                        .add(order.getString("equipment_key"));
                setOrderValuesSQL(params, order);
                formatOffers(order);
                params.add(order.getInteger("basket_id"))
                        .add(order.getBoolean("reassort"))
                        .add(order.getJsonArray("offers"))
                        .add(order.getDouble("tva5"))
                        .add(order.getDouble("tva20"))
                        .add(order.getDouble("priceht"));
            }
        }
        query = query.substring(0, query.length() - 1);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void formatOffers(JsonObject order) {
        for (int i = 0; i < order.getJsonArray("offers").size(); i++) {
            JsonObject offer = order.getJsonArray("offers").getJsonObject(i);
            offer.put("id", "F" + order.getLong("id_order_client_equipment") + "_" + i);
        }
    }

    @Override
    public void updateOrders(List<Integer> ids, String status, String justification, final Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " SET  status = ?, cause_status = ?" +
                " WHERE id in " + Sql.listPrepared(ids.toArray()) + " ; ";

        query += "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET  status = ?, cause_status = ? " +
                "WHERE id in ( SELECT ore.id_order_client_equipment FROM " + Crre.crreSchema + ".\"order-region-equipment\" ore " +
                "WHERE id in " + Sql.listPrepared(ids.toArray()) + " ) ; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add(id);
        }
        params.add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add(id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deletedOrders(JsonArray orders, String table, Handler<Either<String, JsonObject>> handlerJsonObject) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + ".\"" + table + "\" as t " +
                "WHERE t.id IN ( ";
        for (int i = 0; i < orders.size(); i++) {
            query += "?,";
            params.add(orders.getLong(i));
        }
        query = query.substring(0, query.length() - 1) + ")";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }

    @Override
    public void getStatusByOrderId(Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "SELECT id FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" WHERE owner_id != 'renew2021-2022'";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(arrayResponseHandler));

    }

    @Override
    public void updateStatus(JsonArray listIdOrders, Handler<Either<String, JsonObject>> handlerJsonObject) {
        String query = "";
        JsonArray params = new JsonArray();
        for (int i = 0; i < listIdOrders.size(); i++) {
            query += "UPDATE " + Crre.crreSchema + ".\"order-region-equipment-old\" SET id_status = ? WHERE id = ?;";
            params.add(listIdOrders.getJsonObject(i).getInteger("status")).add(listIdOrders.getJsonObject(i).getInteger("id"));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }
}
