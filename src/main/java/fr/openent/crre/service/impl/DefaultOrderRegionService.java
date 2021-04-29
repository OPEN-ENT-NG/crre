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
    public void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT EXISTS(SELECT id FROM " +
                Crre.crreSchema + ".\"order-region-equipment\" " +
                "WHERE equipment_key = ? AND id_structure = ? AND owner_id = 'renew2021-2022' );";
        sql.prepared(query, new JsonArray().add(idEquipment).add(idStructure), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAllOrderRegionByProject(int idProject, boolean filterRejectedSentOrders, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String selectOld = old ? ", ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers " : "";
        String query = "SELECT ore.*, to_json(campaign.*) campaign, campaign.name AS campaign_name, p.title AS title, " +
                "to_json(oce.*) AS order_parent, bo.name AS basket_name " + selectOld +
                "FROM  " + Crre.crreSchema + (old ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore " +
                "LEFT JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oce ON ore.id_order_client_equipment = oce.id ";
        jointureAndFilter(idProject, filterRejectedSentOrders, arrayResponseHandler, query);
    }

    static void jointureAndFilter(int idProject, boolean filterRejectedSentOrders, Handler<Either<String, JsonArray>> arrayResponseHandler, String query) {
        query = innerJoin(query);
        query += "WHERE ore.id_project = ? AND ore.equipment_key IS NOT NULL ";
        if(filterRejectedSentOrders) {
            query += "AND ore.status != 'SENT' AND ore.status != 'REJECTED'";
        }
        Sql.getInstance().prepared(query, new JsonArray().add(idProject), SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getOrdersRegionById(List<Integer> idsOrder, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String selectOld = oldTable ? ", ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers, oce.* " : "";
        String query = "SELECT ore.*, to_json(campaign.*) campaign, campaign.name AS campaign_name, p.title AS title, " +
                "to_json(oce.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id " + selectOld +
                "FROM  " + Crre.crreSchema + (oldTable ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore " +
                "LEFT JOIN " + Crre.crreSchema + (oldTable ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oce ON ore.id_order_client_equipment = oce.id ";
        jointureAndFilter(idsOrder, arrayResponseHandler, query);
    }


    static void jointureAndFilter(List<Integer> idsOrder, Handler<Either<String, JsonArray>> arrayResponseHandler, String query) {
        query = innerJoin(query);
        query += "WHERE ore.id in " + Sql.listPrepared(idsOrder.toArray()) + " ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : idsOrder) {
            params.add( id);
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(arrayResponseHandler));
    }

    private static String innerJoin(String query) {
        query += "INNER JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
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
        prepareSQLConditionOrderRegion(user, startDate, endDate, page, filterRejectedSentOrders, idStructure, values, query, PAGE_SIZE);
        sql.prepared(query.toString(), values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    static void prepareSQLConditionOrderRegion(UserInfos user, String startDate, String endDate, Integer page,
                                               boolean filterRejectedSentOrders, String idStructure, JsonArray values,
                                               StringBuilder query, Integer limitpage) {
        values.add(startDate);
        values.add(endDate);

        if(filterRejectedSentOrders) {
            query.append(" AND ore.status != 'SENT' AND ore.status != 'REJECTED' ");
        }

        if(!WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString()) &&
                WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString())){
            query.append(" AND ore.id_structure = ?");
            values.add(idStructure);
        }
        query.append(" ORDER BY ore.creation_date DESC ");
        if (page != null) {
            query.append("OFFSET ? LIMIT ? ");
            values.add(limitpage * page);
            values.add(limitpage);
        }
    }

    public void search(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                       Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure, old);
        if(!old) {
            if (!query.equals("")) {
                sqlquery += "AND (lower(p.title) ~* ? OR lower(ore.owner_name) ~* ? OR lower(b.name) ~* ? OR oe.equipment_key IN (";
                values.add(query);
                values.add(query);
                values.add(query);
            } else {
                sqlquery += "AND (oe.equipment_key IN (";
            }

            for (int i = 0; i < equipTab.size(); i++) {
                sqlquery += "?,";
                values.add(equipTab.getJsonObject(i).getString("ean"));
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + "))";
        } else {
            if (!query.equals("")) {
                sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
                values.add(query);
                values.add(query);
                values.add(query);
                values.add(query);
            } else {
                sqlquery += "AND (p.title ~* p.title OR ore.owner_name ~* ore.owner_name OR b.name ~* b.name) ";
            }
        }
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    static String selectSQLOrders(String startDate, String endDate, JsonArray values, String idStructure, boolean oldTable) {
        String sqlquery = "SELECT DISTINCT (p.*), ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + (oldTable ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + (oldTable ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ?";
        values.add(startDate);
        values.add(endDate);
        if(!idStructure.equals("null")) {
            sqlquery += " AND ore.id_structure = ?";
            values.add(idStructure);
        }
        return sqlquery;
    }

    @Override
    public void filterSearch(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                             Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure, old);
        if(!old) {
            if(equipTab.getJsonArray(1).isEmpty()){
                sqlquery += "AND (lower(p.title) ~* ? OR lower(ore.owner_name) ~* ? OR lower(b.name) ~* ?) ";
            } else {
                sqlquery += "AND (lower(p.title) ~* ? OR lower(ore.owner_name) ~* ? OR lower(b.name) ~* ? OR ore.equipment_key IN (";
                sqlquery = DefaultOrderService.insertValuesQuery(equipTab, values, sqlquery);
            }
            sqlquery += " AND oe.equipment_key IN (";
            if(equipTab.getJsonArray(0).isEmpty()) {
                sqlquery += "?)";
                values.add("null");
            } else {
                sqlquery = DefaultOrderService.insertEquipmentEAN(equipTab, values, sqlquery);
            }
        } else {
            sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
            values.add(query);
        }

        values.add(query);
        values.add(query);
        values.add(query);
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    @Override
    public void filter_only(UserInfos user, JsonArray equipTab, String startDate, String endDate, String idStructure, JsonArray filters,
                            Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure, old);
        if(!old) {
            sqlquery += "AND oe.equipment_key IN (";

            if(equipTab.isEmpty()) {
                sqlquery += "?)";
                values.add("null");
            } else {
                for (int i = 0; i < equipTab.size(); i++) {
                    sqlquery += "?,";
                    values.add(equipTab.getJsonObject(i).getString("ean"));
                }
                sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
            }
        }
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    static void addValues(String key, String value, HashMap<String, ArrayList> hashMap) {
        ArrayList tempList;
        if (hashMap.containsKey(key)) {
            tempList = hashMap.get(key);
            if(tempList == null)
                tempList = new ArrayList();
            tempList.add(value);
        } else {
            tempList = new ArrayList();
            tempList.add(value);
        }
        hashMap.put(key,tempList);
    }

    @Override
    public void filterSearchWithoutEquip(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                                         Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure, false);
        if (!query.equals("")) {
            sqlquery += "AND (lower(p.title) ~* ? OR lower(ore.owner_name) ~* ? OR lower(b.name) ~* ?)";
            values.add(query);
            values.add(query);
            values.add(query);
        }

        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    static void filtersSQLCondition(JsonArray filters, Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler,
                                     JsonArray values, HashMap<String, ArrayList> hashMap, String sqlquery, Integer pagelimit) {
        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = (String) filters.getJsonObject(i).fieldNames().toArray()[0];
                if(key.equals("id_structure")) {
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
            for(Map.Entry mapentry : hashMap.entrySet()) {
                ArrayList list = (ArrayList) mapentry.getValue();
                String keys = mapentry.getKey().toString();
                if(keys.equals("renew")) {
                    if(list.size() == 2) {
                        sqlquery += "ore.owner_id ~* ore.owner_id";
                    } else {
                        if (Boolean.parseBoolean(String.valueOf(list.get(0)))) {
                            sqlquery += "ore.owner_id ~* 'renew'";
                        } else {
                            sqlquery += "ore.owner_id !~* 'renew'";
                        }
                    }
                } else {
                    sqlquery += !(keys.equals("reassort") || keys.equals("status")) ? "b." + keys + " IN(" : "ore." + keys + " IN(";
                    for(int k = 0; k < list.size(); k++) {
                        sqlquery += k+1 == list.size() ? "?)" : "?, ";
                        values.add(list.get(k).toString());
                    }
                }

                if(!(count == hashMap.entrySet().size() - 1)) {
                    sqlquery += " AND ";
                } else {
                    sqlquery += ")";
                }
                count ++;
            }
        }
        sqlquery = sqlquery + " ORDER BY ore.creation_date DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(pagelimit * page);
            values.add(pagelimit);
        }
        Sql.getInstance().prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT p.title, ore.creation_date " +
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
                " (id, amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_order_client_equipment, id_project, reassort) VALUES ";

        for (int i = 0; i < orderRegions.size(); i++) {
            if(orderRegions.getJsonObject(i).containsKey("id_project")) {
                query += "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),";
                JsonObject order = orderRegions.getJsonObject(i);
                String creation_date;
                if(isRenew) {
                    creation_date = order.getString("creation_date");
                } else {
                    Date date = new SimpleDateFormat("dd-MM-yyyy").parse(order.getString("creation_date"));
                    creation_date = new SimpleDateFormat("yyyy-MM-dd").format(date);
                }

                params.add(order.getLong("id"))
                        .add(order.getInteger("amount"))
                        .add(creation_date)
                        .add(order.getString("owner_name"))
                        .add(order.getString("owner_id"))
                        .add(order.getString("status"))
                        .add(order.getString("equipment_key"));
                setOrderValuesSQL(params, order);
                params.add(order.getInteger("id_order_client_equipment"))
                        .add(order.getLong("id_project"))
                        .add(order.getBoolean("reassort"));
            }
        }
        query = query.substring(0, query.length()-1);
        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L), SqlResult.validUniqueResultHandler(handler));
    }

    private void setOrderValuesSQL(JsonArray params, JsonObject order) {
        params.add(order.getString("name"))
                .add(order.getString("image"))
                .add(order.getDouble("unitedPriceTTC"))
                .add(order.getString("grade"))
                .add(order.getString("editor"))
                .add(order.getString("diffusor"))
                .add(order.getString("type"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
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
            if(orderRegions.getJsonObject(i).containsKey("id_project")) {
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
                params.add(order.getInteger("basket_id"))
                        .add(order.getBoolean("reassort"))
                        .add(order.getJsonArray("offers"))
                        .add(order.getDouble("tva5"))
                        .add(order.getDouble("tva20"))
                        .add(order.getDouble("priceht"));
            }
        }
        query = query.substring(0, query.length()-1);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
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

    @Override
    public void deletedOrderClient(JsonArray ordersClient, Handler<Either<String, JsonObject>> handlerJsonObject) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "WHERE oce.id IN ( ";
        for (int i = 0; i < ordersClient.size(); i++) {
            query += "?,";
            params.add(ordersClient.getLong(i));
        }
        query = query.substring(0, query.length() - 1) + ")";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }

    @Override
    public void deleteOrderRegion(JsonArray ordersRegion, Handler<Either<String, JsonObject>> handlerJsonObject) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + ".\"order-region-equipment\" ore " +
                "WHERE ore.id IN ( ";
        for (int i = 0; i < ordersRegion.size(); i++) {
            query += "?,";
            params.add(ordersRegion.getLong(i));
        }
        query = query.substring(0, query.length() - 1) + ")";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }

}
