package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.core.enums.database.sql.OrderClientEquipmentTableField;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.OrderClientEquipmentModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.service.OrderService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private final Integer PAGE_SIZE = 15;
    private static final Logger log = LoggerFactory.getLogger(DefaultOrderService.class);


    public DefaultOrderService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public Future<List<OrderUniversalModel>> listOrder(String searchingText, List<Integer> campaignIdList, List<String> structureIdList, List<String> userIdList,
                                                       List<String> basketIdList, List<Integer> orderIdList, String startDate, String endDate, List<OrderStatus> orderStatusList) {
        Promise<List<OrderUniversalModel>> promise = Promise.promise();
        JsonArray values = new JsonArray();
        String query = "SELECT " + getOrderUniversalSelector() + ", to_jsonb(basket.*) basket, to_jsonb(campaign.*) campaign, to_jsonb(project.*) project" +
                " FROM crre.order_universal as o_u" +
                " LEFT JOIN crre.basket_order basket on o_u.id_basket = basket.id" +
                " LEFT JOIN crre.project project on o_u.id_project = project.id" +
                " LEFT JOIN crre.campaign campaign on campaign.id = o_u.id_campaign";

        query += " WHERE (prescriber_validation_date BETWEEN ? AND ?) ";
        values.add(startDate).add(endDate);

        if(searchingText != null) {
            query += "AND (lower(basket.name) ~* ? OR lower(basket.name_user) ~* ?) ";
            values.add(searchingText).add(searchingText);
        }

        if (campaignIdList != null && !campaignIdList.isEmpty()) {
            query += "AND campaign.id IN " + Sql.listPrepared(campaignIdList) + " ";
            values.addAll(new JsonArray(campaignIdList));
        }

        if (structureIdList != null && !structureIdList.isEmpty()) {
            query += "AND o_u.id_structure IN " + Sql.listPrepared(structureIdList) + " ";
            values.addAll(new JsonArray(structureIdList));
        }

        if (basketIdList != null && !basketIdList.isEmpty()) {
            query += "AND basket.id IN " + Sql.listPrepared(basketIdList) + " ";
            values.addAll(new JsonArray(basketIdList));
        }

        if (orderIdList != null && !orderIdList.isEmpty()) {
            query += "AND o_u.order_client_id IN " + Sql.listPrepared(orderIdList) + " OR " +
                    "o_u.order_region_id IN " + Sql.listPrepared(orderIdList) + " ";
            values.addAll(new JsonArray(orderIdList))
                    .addAll(new JsonArray(orderIdList));
        }

        if (userIdList != null && !userIdList.isEmpty()) {
            query += "AND o_u.prescriber_id IN " + Sql.listPrepared(userIdList) + " ";
            values.addAll(new JsonArray(userIdList));
        }

        if (orderStatusList != null && !orderStatusList.isEmpty()) {
            query += "AND o_u.status IN " + Sql.listPrepared(orderStatusList) + " ";
            values.addAll(new JsonArray(orderStatusList.stream().map(Enum::name).collect(Collectors.toList())));
        }

        query += "ORDER BY o_u.prescriber_validation_date ASC";

        String errorMessage = String.format("[CRRE@%s::listOrder] Fail to list order", this.getClass().getSimpleName());
        sql.prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, OrderUniversalModel.class, errorMessage)));

        return promise.future();
    }

    private String getOrderUniversalSelector() {
        return "o_u.amount as amount, o_u.prescriber_validation_date as prescriber_validation_date, o_u.id_campaign as id_campaign, o_u.id_structure as id_structure," +
                " o_u.status as status, o_u.equipment_key as equipment_key, o_u.cause_status as cause_status, o_u.comment as comment," +
                " o_u.prescriber_id as prescriber_id, o_u.id_basket as id_basket, o_u.reassort as reassort, o_u.validator_id as validator_id," +
                " o_u.validator_name as validator_name, o_u.validator_validation_date as validator_validation_date, o_u.modification_date as modification_date," +
                " o_u.id_project as id_project," +
                " o_u.equipment_name, o_u.equipment_image, o_u.equipment_price, o_u.equipment_grade," +
                " o_u.equipment_editor, o_u.equipment_diffusor, o_u.equipment_format, o_u.equipment_tva5, o_u.equipment_tva20," +
                " o_u.equipment_priceht, o_u.offers, o_u.total_free, o_u.order_client_id, o_u.order_region_id";
    }

    @Override
    public void listOrder(String status, String idStructure, Integer page, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.*, bo.name as basket_name, bo.name_user as user_name, to_json(campaign.* ) campaign, ore.status as region_status " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo " +
                "ON bo.id = oce.id_basket " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" ore ON oce.id = ore.id_order_client_equipment ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, idStructure, query, startDate, endDate, values);

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
    public void listOrderAmount(String status, String idStructure, UserInfos user, String startDate, String endDate, Boolean consumable,
                                Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT DISTINCT sum(oce.amount) as nb_licences " +
                "FROM crre.order_client_equipment oce " +
                "LEFT JOIN crre.campaign c on (oce.id_campaign = c.id) " +
                "LEFT JOIN crre.basket_order bo on (oce.id_basket = bo.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        query = filterWaitingOrder(status, idStructure, query, startDate, endDate, values);
        query += "AND c.use_credit = '" + (consumable ? "consumable_" : "") + "licences';";
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getTotalAmountOrder(String status, String idStructure, UserInfos user, String startDate, String endDate, JsonArray filters,
                                    Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT DISTINCT oce.id, oce.amount " +
                "FROM crre.order_client_equipment oce " +
                "LEFT JOIN crre.campaign c on (oce.id_campaign = c.id) " +
                "LEFT JOIN crre.basket_order bo on (oce.id_basket = bo.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        query = filterWaitingOrder(status, idStructure, query, startDate, endDate, values);
        if (filters != null && filters.size() > 0) {
            query += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = (String) filters.getJsonObject(i).fieldNames().toArray()[0];
                JsonArray value = filters.getJsonObject(i).getJsonArray(key);
                if (key.equals("id_user")) {
                    query += "oce.user_id IN ( ";
                } else {
                    query += "oce." + key + " IN ( ";
                }
                for (int k = 0; k < value.size(); k++) {
                    query += "?,";
                    values.add(value.getString(k));
                }
                query = query.substring(0, query.length() - 1) + ")";
                if (!(i == filters.size() - 1)) {
                    query += " AND ";
                } else {
                    query += ")";
                }
            }
        }
        query += "AND (c.use_credit = 'credits' OR c.use_credit = 'consumable_credits');";
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listOrderCredit(String status, String idStructure, UserInfos user, String startDate, String endDate, JsonArray filters, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT DISTINCT oce.equipment_key, oce.amount, c.use_credit, oce.id " +
                "FROM crre.order_client_equipment oce " +
                "LEFT JOIN crre.campaign c on (oce.id_campaign = c.id) " +
                "LEFT JOIN crre.basket_order bo on (oce.id_basket = bo.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) ";
        query = filterWaitingOrder(status, idStructure, query, startDate, endDate, values);
        query += "AND (c.use_credit = 'credits' OR c.use_credit = 'consumable_credits');";
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listUsers(String status, String idStructure, UserInfos user, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT(bo.name_user) as user_name, bo.id_user " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment oce ON bo.id = oce.id_basket ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        query = filterWaitingOrder(status, idStructure, query, null, null, values);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    private String filterWaitingOrder(String status, String idStructure, String query, String startDate, String endDate, JsonArray values) {
        query += "WHERE ";
        if (startDate != null || endDate != null) {
            query += "oce.creation_date BETWEEN ? AND ? AND ";
            values.add(startDate).add(endDate);
        }
        query += "oce.id_structure = ? ";
        values.add(idStructure);
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
    public Future<List<OrderClientEquipmentModel>> updateStatus(List<Integer> orderClientEquipmentIdList, OrderStatus orderStatus) {
        Promise<List<OrderClientEquipmentModel>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment SET status = ? WHERE id IN " +
                Sql.listPrepared(orderClientEquipmentIdList) + " RETURNING *";
        values.add(orderStatus.toString());
        values.addAll(new JsonArray(orderClientEquipmentIdList));

        String errorMessage = String.format("[CRRE@%s::updateStatus] Fail to update status", this.getClass().getSimpleName());
        sql.prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, OrderClientEquipmentModel.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> search(String query, Map<String, List<String>> filters, String idStructure, List<String> equipementIdList,
                                    Integer idCampaign, String startDate, String endDate, Integer page) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray values = new JsonArray();
        final StringBuilder sqlquery = new StringBuilder("SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount, " +
                "oe.id as id, tc.name as type_name, to_json(c.* ) campaign " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign c ON (c.id = oe.id_campaign) " +
                "LEFT JOIN " + Crre.crreSchema + ".type_campaign tc ON (tc.id = c.id_type) " +
                "WHERE oe.creation_date BETWEEN ? AND ? ");
        values.add(startDate).add(endDate);
        if (idCampaign != null) {
            sqlquery.append("AND oe.id_campaign = ? ");
            values.add(idCampaign);
        }

        sqlquery.append(DefaultBasketOrderService.SQLConditionQueryEquipments(query, equipementIdList, values, false, ""));

        sqlquery.append(") AND oe.status = 'WAITING' AND oe.id_structure = ? ");
        values.add(idStructure);

        if (filters != null && filters.size() > 0) {
            String filtersString = filters.entrySet().stream()
                    .map(filter -> {
                        String filterString;
                        if (Field.ID_USER.equals(filter.getKey())) {
                            filterString = "bo.";
                            // if the key is in OrderClientEquipmentTableField
                        } else if (Arrays.stream(OrderClientEquipmentTableField.values())
                                .anyMatch(orderClientEquipmentTableField -> orderClientEquipmentTableField.name().equalsIgnoreCase(filter.getKey()))) {
                            filterString = "oe.";
                        } else {
                            return null;
                        }
                        filterString += filter.getKey() + " IN " + Sql.listPrepared(filter.getValue());
                        values.addAll(new JsonArray(filter.getValue()));

                        return filterString;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" AND "));
            if (!filtersString.isEmpty()) {
                sqlquery.append(" AND ( ").append(filtersString).append(")");
            }
        }

        sqlquery.append(" ORDER BY creation_date DESC ");
        if (page != null) {
            sqlquery.append("OFFSET ? LIMIT ? ");
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery.toString(), values, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public Future<List<OrderClientEquipmentModel>> getOrderClientEquipmentList(List<Integer> orderClientEquipmentIdList) {
        Promise<List<OrderClientEquipmentModel>> promise = Promise.promise();

        String selectQuery = "SELECT * FROM " + Crre.crreSchema + ".order_client_equipment WHERE id IN " + Sql.listPrepared(orderClientEquipmentIdList) + ";";
        String errorMessage = String.format("[CRRE@%s::getOrderClientEquipmentList] Fail to get basket order", this.getClass().getSimpleName());
        Sql.getInstance().prepared(selectQuery, new JsonArray(orderClientEquipmentIdList),
                SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, OrderClientEquipmentModel.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<OrderClientEquipmentModel>> getOrderClientEquipmentListFromBasketId(List<Integer> basketIdList) {
        Promise<List<OrderClientEquipmentModel>> promise = Promise.promise();

        String selectQuery = "SELECT row_to_json(o_c_e.*) " +
                "FROM " + Crre.crreSchema + ".basket_order AS bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS o_c_e ON (bo.id = o_c_e.id_basket) " +
                "WHERE o_c_e.id_basket IN " + Sql.listPrepared(basketIdList) + " " +
                "UNION ALL " +
                "SELECT row_to_json(o_c_e_o.*) " +
                "FROM " + Crre.crreSchema + ".basket_order AS bo " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS o_c_e_o ON (bo.id = o_c_e_o.id_basket) " +
                "WHERE o_c_e_o.id_basket IN " + Sql.listPrepared(basketIdList) + ";";
        JsonArray params = new JsonArray(new ArrayList<>(basketIdList));
        params.addAll(new JsonArray(basketIdList));
        Sql.getInstance().prepared(selectQuery, params,
                SqlResult.validResultHandler(stringJsonArrayEither -> {
                    if (stringJsonArrayEither.isRight()) {
                        promise.complete(stringJsonArrayEither.right().getValue().stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .map(orderJson -> IModelHelper.toModel(new JsonObject(orderJson.getString(Field.ROW_TO_JSON)), OrderClientEquipmentModel.class))
                                .collect(Collectors.toList()));
                    } else {
                        String errorMessage = String.format("[CRRE@%s::getOrderClientEquipmentListFromBasketId] Fail to get basket order", this.getClass().getSimpleName());
                        log.error(errorMessage);
                        promise.fail(stringJsonArrayEither.left().getValue());
                    }
                }));
        return promise.future();
    }
}

