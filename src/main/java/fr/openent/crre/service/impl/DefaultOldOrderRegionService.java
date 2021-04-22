package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.OldOrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;

public class DefaultOldOrderRegionService extends SqlCrudService implements OldOrderRegionService {

    private final Integer PAGE_SIZE = 10;

    public DefaultOldOrderRegionService(String table) {
        super(table);
    }

    @Override
    public void getOneOrderRegion(int idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(tt.*) AS title, " +
                "       to_json(oce.*) AS order_parent " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS oce ON ore.id_order_client_equipment = oce.id " +
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
    public void getAllOrderRegionByProject(int idProject, boolean filterRejectedSentOrders, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "" +
                "SELECT ore.*, ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers, " +
                "       to_json(campaign.*) campaign, " +
                "       campaign.name AS campaign_name, " +
                "       p.title AS title, " +
                "       to_json(oce.*) AS order_parent, " +
                "       bo.name AS basket_name " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON bo.id = oce.id_basket " +
                "LEFT JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "LEFT JOIN  " + Crre.crreSchema + ".project AS p ON p.id = ore.id_project " +
                "LEFT JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "LEFT JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.id_project = ? AND ore.equipment_key IS NOT NULL ";
        if(filterRejectedSentOrders) {
            query += "AND ore.status != 'SENT' AND ore.status != 'REJECTED'";
        }
        Sql.getInstance().prepared(query, new JsonArray().add(idProject), SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getOrdersRegionById(List<Integer> idsOrder, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "" +
                "SELECT ore.*, oce.*, " +
                "       to_json(campaign.*) campaign, " +
                "       campaign.name AS campaign_name, " +
                "       p.title AS title, " +
                "       to_json(oce.*) AS order_parent, " +
                "       bo.name AS basket_name, " +
                "       bo.id AS basket_id " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS oce ON ore.id_order_client_equipment = oce.id " +
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
    public void getAllProjects(UserInfos user, String startDate, String endDate, Integer page, boolean filterRejectedSentOrders, String idStructure, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder("" +
                "SELECT DISTINCT (p.*), ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore ON ore.id_project = p.id " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND ore.equipment_key IS NOT NULL ");
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
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query.toString(), values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void searchName(String word, Handler<Either<String, JsonArray>> handler) {
        if(!(word.equals(""))) {
            plainTextSearchName(word, handler);
        } else {
            handler.handle(new Either.Right<>(new JsonArray()));
        }

    }

    public void search(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                       Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure);

        if (!query.equals("")) {
            sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
            values.add(query);
            values.add(query);
            values.add(query);
            values.add(query);
        } else {
            sqlquery += "AND (p.title ~* p.title OR ore.owner_name ~* ore.owner_name OR b.name ~* b.name) ";
        }

        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery);
    }

    private String selectSQLOrders(String startDate, String endDate, JsonArray values, String idStructure) {
        String sqlquery = "SELECT DISTINCT (p.*), ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore ON ore.id_project = p.id " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS oe ON oe.id = ore.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON b.id = oe.id_basket " +
                "WHERE ore.creation_date BETWEEN ? AND ? AND ore.id_structure = ? ";
        values.add(startDate);
        values.add(endDate);
        values.add(idStructure);
        return sqlquery;
    }

    @Override
    public void filterSearch(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                             Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure);
        sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
        values.add(query);
        values.add(query);
        values.add(query);
        values.add(query);
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery);
    }

    @Override
    public void filter_only(UserInfos user, String startDate, String endDate, String idStructure, JsonArray filters,
                            Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure);
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery);
    }

    private void addValues(String key, String value, HashMap<String, ArrayList> hashMap) {
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

    private void filtersSQLCondition(JsonArray filters, Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler,
                                     JsonArray values, HashMap<String, ArrayList> hashMap, String sqlquery) {
        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(i).fieldNames().toString().length() -1);
                if(key.equals("id_structure")) {
                    JsonArray uai = filters.getJsonObject(i).getJsonArray(key);
                    for (int j = 0; j < uai.size(); j++) {
                        addValues(key, uai.getJsonObject(j).getString("uai"), hashMap);
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
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT p.title, ore.creation_date " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore ON ore.id_project = p.id ";
        query = query + " ORDER BY p.id DESC LIMIT 1";
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(arrayResponseHandler));
    }

    @Override
    public void  updateOrders(List<Integer> ids, String status, String justification, final Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Crre.crreSchema + ".\"order-region-equipment-old\" " +
                " SET  status = ?, cause_status = ?" +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ; ";

        query += "UPDATE " + Crre.crreSchema + ".order_client_equipment_old " +
                "SET  status = ?, cause_status = ? " +
                "WHERE id in ( SELECT ore.id_order_client_equipment FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" ore " +
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
