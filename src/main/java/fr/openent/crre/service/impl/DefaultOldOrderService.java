package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OldOrderService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static fr.openent.crre.service.impl.DefaultOrderRegionService.filtersSQLCondition;
import static fr.openent.crre.service.impl.DefaultOrderRegionService.selectSQLOrders;

public class DefaultOldOrderService extends SqlCrudService implements OldOrderService {

    private final Integer PAGE_SIZE = 10;

    public DefaultOldOrderService(String table) {
        super(table);
    }

    @Override
    public void getAllOrderRegionByProject(int idProject, boolean filterRejectedSentOrders, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        String query = "SELECT ore.*, ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers, " +
                "to_json(campaign.*) campaign, campaign.name AS campaign_name, p.title AS title, to_json(oce.*) AS order_parent, bo.name AS basket_name " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment-old\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS oce ON ore.id_order_client_equipment = oce.id ";
        DefaultOrderRegionService.jointureAndFilter(idProject, filterRejectedSentOrders, arrayResponseHandler, query);
    }

    public void search(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                       Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure,true);
        if (!query.equals("")) {
            sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
            values.add(query);
            values.add(query);
            values.add(query);
            values.add(query);
        } else {
            sqlquery += "AND (p.title ~* p.title OR ore.owner_name ~* ore.owner_name OR b.name ~* b.name) ";
        }
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    @Override
    public void filterSearch(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                             Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure,true);
        sqlquery += "AND (p.title ~* ? OR ore.owner_name ~* ? OR b.name ~* ? OR ore.equipment_name ~* ?) ";
        values.add(query);
        values.add(query);
        values.add(query);
        values.add(query);
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
    }

    @Override
    public void filter_only(UserInfos user, String startDate, String endDate, String idStructure, JsonArray filters,
                            Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<String, ArrayList> hashMap = new HashMap<>();
        String sqlquery = selectSQLOrders(startDate, endDate, values, idStructure,true);
        filtersSQLCondition(filters, page, arrayResponseHandler, values, hashMap, sqlquery, PAGE_SIZE);
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
                        .add(order.getString("ean"));
                setOrderValuesSQL(params, order);
                params.add(order.getInteger("id_order_client_equipment"))
                        .add(order.getLong("id_project"))
                        .add(order.getBoolean("reassort"));
            }
        }
        query = query.substring(0, query.length()-1);
        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L),
                SqlResult.validUniqueResultHandler(handler));
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
}
