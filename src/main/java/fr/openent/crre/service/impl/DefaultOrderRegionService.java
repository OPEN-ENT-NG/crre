package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.DateHelper;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.*;
import fr.openent.crre.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.utils.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.openent.crre.controllers.OrderController.exportPriceComment;
import static fr.openent.crre.controllers.OrderController.exportStudents;
import static fr.openent.crre.core.constants.Field.UTF8_BOM;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static java.lang.Math.min;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    private final Integer PAGE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(DefaultOrderRegionService.class);


    public DefaultOrderRegionService(String table) {
        super(table);
    }

    @Override
    public TransactionElement getTransactionCreateOrdersRegion(JsonObject order, Number idProject) {
        String queryOrderRegionEquipment = "INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " (amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, id_campaign, id_structure," +
                " comment, id_order_client_equipment, id_project, reassort) " +
                "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING * ;";

        JsonArray params = new JsonArray()
                .add(order.getInteger(Field.AMOUNT))
                .add(order.getString(Field.CREATION_DATE))
                .add(order.getString(Field.USER_NAME))
                .add(order.getString(Field.USER_ID))
                .add(Field.IN_PROGRESS)
                .add(order.getString(Field.EQUIPMENT_KEY))
                .add(order.getInteger(Field.ID_CAMPAIGN))
                .add(order.getString(Field.ID_STRUCTURE))
                .add(order.getString(Field.COMMENT))
                .add(order.getLong(Field.ID_ORDER_CLIENT_EQUIPMENT))
                .add(idProject)
                .add(order.getBoolean(Field.REASSORT));

        return new TransactionElement(queryOrderRegionEquipment, params);
    }

    @Override
    public Future<List<Integer>> getAllIdsStatus() {
        Promise<List<Integer>> promise = Promise.promise();
        String query = "SELECT id FROM " + Crre.crreSchema + ".status;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                List<Integer> idsStatus = event.right().getValue()
                        .stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .collect(Collectors.toList());
                promise.complete(idsStatus);
            } else {
                String errorMessage = String.format("[CRRE@%s::getAllIdsStatus] Fail to get status ids. %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errorMessage);
                promise.fail(event.left().getValue());
            }
        }));
        return promise.future();
    }


    @Override
    public void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT EXISTS(SELECT id FROM " +
                Crre.crreSchema + ".\"order-region-equipment\" " +
                "WHERE equipment_key = ? AND id_structure = ? AND owner_id = 'renew2021-2022' );";
        sql.prepared(query, new JsonArray().add(idEquipment).add(idStructure), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonArray> getAllOrderRegionByProject(List<Integer> idsProject, FilterModel filters, FilterItemModel filtersItem, List<String> itemSearchedIdsList, List<String> itemFilteredIdsList) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray values = new JsonArray();

        if (!filters.getStatus().isEmpty()) {
            JsonArray statusArray = new JsonArray(filters.getStatus()
                    .stream()
                    .map(OrderStatus::toString)
                    .collect(Collectors.toList()));
            values.addAll(statusArray);
        }


        String select = "SELECT to_jsonb(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, p.title AS title, " +
                "to_jsonb(o_c_e.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, " +
                "o_r_e.id, o_r_e.id_structure, o_r_e.amount, o_r_e.creation_date, o_r_e.modification_date, o_r_e.owner_name, o_r_e.owner_id, o_r_e.status, " +
                "o_r_e.equipment_key, o_r_e.cause_status, o_r_e.comment, o_r_e.id_project, o_r_e.id_order_client_equipment, " +
                "o_r_e.reassort, NULL as total_free, null as image, null as name, null as price, null as offers, null as editeur, null as distributeur, null as _index, null as status_name, " +
                "-1 as status_id, false as old " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project " + (filters.getStatus().size() > 0 ? "AND o_r_e.status IN " + Sql.listPrepared(filters.getStatus()) + " " : "") +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON (bo.id = o_c_e.id_basket) " +
                "LEFT JOIN  " + Crre.crreSchema + ".campaign ON (o_r_e.id_campaign = campaign.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".students AS st ON (o_r_e.id_structure = st.id_structure) " +
                "LEFT JOIN " + Crre.crreSchema + ".structure AS struct ON (o_r_e.id_structure = struct.id_structure) " +
                "WHERE o_r_e.id_project IN " + Sql.listPrepared(idsProject) + " AND o_r_e.equipment_key IS NOT NULL ";
        values.addAll(new JsonArray(idsProject));
        // Condition de recherche de texte
        if (filters.getSearchingText() != null) {
            select += "AND (lower(o_r_e.owner_name) ~* ? OR lower(bo.name) ~* ? OR lower(struct.uai) ~* ? OR lower(struct.name) ~* ? " +
                    "OR lower(struct.city) ~* ? OR lower(struct.region) ~* ? OR lower(struct.public) ~* ? OR lower(struct.catalog) ~* ? OR lower(p.title) ~* ?";
            values.add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText());
            if (!itemSearchedIdsList.isEmpty()) {
                select += " OR o_r_e.equipment_key IN " + Sql.listPrepared(itemSearchedIdsList);
                values.addAll(new JsonArray(itemSearchedIdsList));
            }
            select += ") ";
        }

        // Condition de filtrage d'équipements
        if (filtersItem.hasFilters() && !itemFilteredIdsList.isEmpty()) {
            select += "AND (o_r_e.equipment_key IN " + Sql.listPrepared(itemFilteredIdsList) + ")";
            values.addAll(new JsonArray(itemFilteredIdsList));
        }


        // Condition de filtrage de structures
        if (!filters.getIdsStructure().isEmpty()) {
            select += " AND (o_r_e.id_structure IN " + Sql.listPrepared(filters.getIdsStructure()) + ")";
            values.addAll(new JsonArray(filters.getIdsStructure()));
        }

        // Condition de filtrage des campagnes
        if (!filters.getIdsCampaign().isEmpty()) {
            select += " AND (o_r_e.id_campaign IN " + Sql.listPrepared(filters.getIdsCampaign()) + ")";
            values.addAll(new JsonArray(filters.getIdsCampaign()));
        }

        select += " GROUP BY o_r_e.id, campaign.name, campaign.use_credit, campaign.*, p.title, o_c_e.id, bo.name, bo.id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name, status_id";

        if (!filters.getStatus().isEmpty()) {
            JsonArray statusArray = new JsonArray(filters.getStatus()
                    .stream()
                    .map(OrderStatus::toString)
                    .collect(Collectors.toList()));
            values.addAll(statusArray);
        }

        String selectOld = "SELECT to_jsonb(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, p.title AS title, " +
                "to_jsonb(o_c_e_o.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2 , " +
                "o_r_e_o.id, o_r_e_o.id_structure, o_r_e_o.amount,o_r_e_o.creation_date, o_r_e_o.modification_date, o_r_e_o.owner_name, o_r_e_o.owner_id, o_r_e_o.status, " +
                "o_r_e_o.equipment_key, o_r_e_o.cause_status, o_r_e_o.comment, o_r_e_o.id_project, o_r_e_o.id_order_client_equipment, o_r_e_o.reassort, o_r_e_o.total_free, " +
                "o_r_e_o.equipment_image as image, o_r_e_o.equipment_name as name, o_r_e_o.equipment_price as price, to_jsonb(o_c_e_o.offers) as offers, o_r_e_o.equipment_editor as editeur, " +
                "o_r_e_o.equipment_diffusor as distributeur, o_r_e_o.equipment_format as _index, s.name as status_name, " +
                "s.id as status_id, true as old " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project " + (filters.getStatus().size() > 0 ? "AND o_r_e_o.status IN " + Sql.listPrepared(filters.getStatus()) + " " : "") +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS bo ON (bo.id = o_c_e_o.id_basket) " +
                "LEFT JOIN  " + Crre.crreSchema + ".campaign ON (o_r_e_o.id_campaign = campaign.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".students AS st ON (o_r_e_o.id_structure = st.id_structure) " +
                "LEFT JOIN  " + Crre.crreSchema + ".status AS s ON s.id = o_r_e_o.id_status " +
                "LEFT JOIN " + Crre.crreSchema + ".structure AS struct ON (o_r_e_o.id_structure = struct.id_structure) " +
                "WHERE o_r_e_o.id_project IN " + Sql.listPrepared(idsProject) + " ";

        values.addAll(new JsonArray(idsProject));

        // Condition de filtrage d'équipements
        if (filtersItem.hasFilters()) {
            if (!filtersItem.getEditors().isEmpty() && !filtersItem.getDistributors().isEmpty()) {
                selectOld += " AND ((o_r_e_o.equipment_editor IN " + Sql.listPrepared(filtersItem.getEditors()) + " " +
                        "AND o_r_e_o.equipment_diffusor IN " + Sql.listPrepared(filtersItem.getDistributors()) + ")";
                values.addAll(new JsonArray(filtersItem.getEditors())).addAll(new JsonArray(filtersItem.getDistributors()));
            } else if (!filtersItem.getEditors().isEmpty() && filtersItem.getDistributors().isEmpty()) {
                selectOld += " AND (o_r_e_o.equipment_editor IN " + Sql.listPrepared(filtersItem.getEditors()) + ")";
                values.addAll(new JsonArray(filtersItem.getEditors()));
            } else if (filtersItem.getEditors().isEmpty() && !filtersItem.getDistributors().isEmpty()) {
                selectOld += " AND (o_r_e_o.equipment_diffusor IN " + Sql.listPrepared(filtersItem.getDistributors()) + ")";
                values.addAll(new JsonArray(filtersItem.getDistributors()));
            }
        }

        // Condition de recherche de texte
        if (filters.getSearchingText() != null) {
            selectOld += "AND (lower(o_r_e_o.owner_name) ~* ? OR lower(bo.name) ~* ? OR lower(struct.uai) ~* ? OR lower(struct.name) ~* ? " +
                    "OR lower(struct.city) ~* ? OR lower(struct.region) ~* ? OR lower(struct.public) ~* ? OR lower(struct.catalog) ~* ? OR lower(p.title) ~* ? OR o_r_e_o.equipment_name ~* ?)";
            values.add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText());
        }

        // Condition de filtrage de structures
        if (!filters.getIdsStructure().isEmpty()) {
            selectOld += " AND (o_r_e_o.id_structure IN " + Sql.listPrepared(filters.getIdsStructure()) + ")";
            values.addAll(new JsonArray(filters.getIdsStructure()));
        }

        // Condition de filtrage des campagnes
        if (!filters.getIdsCampaign().isEmpty()) {
            selectOld += " AND (o_r_e_o.id_campaign IN " + Sql.listPrepared(filters.getIdsCampaign()) + ")";
            values.addAll(new JsonArray(filters.getIdsCampaign()));
        }

        selectOld += " GROUP BY o_r_e_o.id, campaign.name, campaign.use_credit, campaign.*, p.title, o_c_e_o.id, bo.name, bo.id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name, status_id";

        String query = String.format("%s UNION %s;", select, selectOld);
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public Future<List<OrderRegionEquipmentModel>> getOrdersRegionById(List<Integer> orderRegionEquipmentIdList) {
        Promise<List<OrderRegionEquipmentModel>> promise = Promise.promise();

        String query = "SELECT * FROM " + Crre.crreSchema + ".\"order-region-equipment\"" +
                "WHERE id IN " + Sql.listPrepared(orderRegionEquipmentIdList);

        JsonArray params = new JsonArray(orderRegionEquipmentIdList);

        String errorMessage = String.format("[CRRE@%s::getOrdersRegionById] Fail to get orders region by id", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, OrderRegionEquipmentModel.class, errorMessage)));
        return promise.future();
    }

    @Override
    public Future<List<OrderRegionEquipmentModel>> getOrdersRegionByStatus(OrderStatus status) {
        Promise<List<OrderRegionEquipmentModel>> promise = Promise.promise();

        String query = "SELECT * FROM " + Crre.crreSchema + ".\"order-region-equipment\"" +
                "WHERE status = ?;";

        String errorMessage = String.format("[CRRE@%s::getOrdersRegionByStatus] Fail to get orders region by status", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, new JsonArray().add(status), SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, OrderRegionEquipmentModel.class, errorMessage)));
        return promise.future();
    }

    @Override
    public Future<List<OrderRegionComplex>> getOrdersRegionById(List<Integer> idsOrder, Boolean oldTable) {
        Promise<List<OrderRegionComplex>> promise = Promise.promise();

        String query;
        JsonArray params = new JsonArray();
        if (oldTable == null) {
            query = getQueryOrdersRegionById(false, idsOrder, params) + " UNION " + getQueryOrdersRegionById(true, idsOrder, params);
        } else {
            query = getQueryOrdersRegionById(oldTable, idsOrder, params);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                log.error(String.format("[CRRE@%s::getOrdersRegionById] Fail to get get orders region by id %s", this.getClass().getSimpleName(), event.left().getValue()));
                promise.fail(event.left().getValue());
            } else {
                List<OrderRegionComplex> orderRegionComplexList = event.right().getValue().stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(jsonObject -> new OrderRegionComplex(new JsonObject(jsonObject.getString(Field.RESULT))))
                        .collect(Collectors.toList());
                promise.complete(orderRegionComplexList);
            }
        }));
        return promise.future();
    }

    private String getQueryOrdersRegionById(boolean oldTable, List<Integer> idsOrder, JsonArray params) {
        params.addAll(new JsonArray(new ArrayList<>(idsOrder)));
        return "SELECT to_jsonb(selectTable" + ((oldTable) ? "_old" : "") + ".*) AS result\n" +
                "FROM (SELECT To_json(ore.*) order_region,\n" +
                "             To_json(campaign.*) campaign,\n" +
                "             To_json(p.*) project,\n" +
                "             To_json(oce.*) order_client,\n" +
                "             To_json(bo.*) basket_order,\n" +
                "             To_json(st.*) students\n" +
                "      FROM   crre.\"order-region-equipment" + ((oldTable) ? "-old" : "") + "\" AS ore\n" +
                "                 LEFT JOIN crre.order_client_equipment" + ((oldTable) ? "_old" : "") + " AS oce\n" +
                "                           ON ore.id_order_client_equipment = oce.id\n" +
                "                 LEFT JOIN crre.students st\n" +
                "                           ON ( oce.id_structure = st.id_structure )\n" +
                "                 INNER JOIN crre.basket_order AS bo\n" +
                "                            ON bo.id = oce.id_basket\n" +
                "                 INNER JOIN crre.campaign\n" +
                "                            ON ore.id_campaign = campaign.id\n" +
                "                 INNER JOIN crre.project AS p\n" +
                "                            ON p.id = ore.id_project\n" +
                "                 INNER JOIN crre.rel_group_structure\n" +
                "                            ON ( ore.id_structure = rel_group_structure.id_structure )\n" +
                "      WHERE  ore.id IN " + Sql.listPrepared(idsOrder) + "\n" +
                "      GROUP  BY ore.id,\n" +
                "                campaign.NAME,\n" +
                "                campaign.use_credit,\n" +
                "                campaign.*,\n" +
                "                p.*,\n" +
                "                oce.id,\n" +
                "                bo.NAME,\n" +
                "                bo.id,\n" +
                "                st.*) as selectTable" + ((oldTable) ? "_old" : "");
    }

    /**
     * @deprecated Use {@link #getOrdersRegionById(List, Boolean)}
     */
    @Deprecated
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
    public Future<JsonArray> search(FilterModel filters, FilterItemModel
            filtersItem, List<String> itemSearchedIdsList, List<String> itemFilteredIdsList) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray values = new JsonArray();
        String sqlquery = "SELECT DISTINCT p.*, COALESCE (o_r_e_o.creation_date, o_r_e.creation_date) as creationDate, count(o_r_e.*) + count(o_r_e_o.*) AS nbOrders " +
                //used only for order
                ", MAX(s.name) as structure_name, MAX(s.uai) as uai " +
                "FROM  " + Crre.crreSchema + ".project p " +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project " + (filters.getStatus().size() > 0 ? "AND o_r_e_o.status IN " + Sql.listPrepared(filters.getStatus()) + " " : "") +
                "LEFT JOIN " + Crre.crreSchema + ".\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project " + (filters.getStatus().size() > 0 ? "AND o_r_e.status IN " + Sql.listPrepared(filters.getStatus()) + " " : "") +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment_old AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order AS b ON (b.id = o_c_e.id_basket OR b.id = o_c_e_o.id_basket) " +
                "LEFT JOIN " + Crre.crreSchema + ".structure AS s ON (o_r_e.id_structure = s.id_structure OR o_r_e_o.id_structure = s.id_structure) " +
                "WHERE ((o_r_e.creation_date BETWEEN ? AND ? AND o_r_e.equipment_key IS NOT NULL) OR " +
                "(o_r_e_o.creation_date BETWEEN ? AND ?)) ";
        // Condition de filtrage de status
        if (!filters.getStatus().isEmpty()) {
            JsonArray statusArray = new JsonArray(filters.getStatus()
                    .stream()
                    .map(OrderStatus::toString)
                    .collect(Collectors.toList()));
            values.addAll(statusArray).addAll(statusArray);
        }
        values.add(filters.getStartDate()).add(filters.getEndDate()).add(filters.getStartDate()).add(filters.getEndDate());

        // Condition de recherche de texte
        if (filters.getSearchingText() != null) {
            sqlquery += "AND (lower(s.uai) ~* ? OR lower(s.name) ~* ? OR lower(s.city) ~* ? OR lower(s.region) ~* ? OR " +
                    "lower(s.public) ~* ? OR lower(s.catalog) ~* ? OR " +
                    "lower(p.title) ~* ? OR lower(o_r_e.owner_name) ~* ? OR lower(o_r_e_o.owner_name) ~* ? OR lower(b.name) ~* ? OR o_r_e_o.equipment_name ~* ? ";
            values.add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText()).add(filters.getSearchingText())
                    .add(filters.getSearchingText());
            if (!itemSearchedIdsList.isEmpty()) {
                sqlquery += " OR o_r_e.equipment_key IN " + Sql.listPrepared(itemSearchedIdsList);
                values.addAll(new JsonArray(itemSearchedIdsList));
            }
            sqlquery += ") ";
        }

        // Condition de filtrage d'équipements
        if (filtersItem.hasFilters()) {
            if (!itemFilteredIdsList.isEmpty()) {
                sqlquery += "AND (o_r_e.equipment_key IN " + Sql.listPrepared(itemFilteredIdsList) + " " +
                        "OR o_r_e_o.id IS NOT NULL) ";
                values.addAll(new JsonArray(itemFilteredIdsList));
            } else {
                sqlquery += "AND (? ";
                values.add(false);
            }
            if (!filtersItem.getEditors().isEmpty() && !filtersItem.getDistributors().isEmpty() && !filtersItem.getCatalogs().isEmpty()) {
                sqlquery += " AND ((o_r_e_o.equipment_editor IN " + Sql.listPrepared(filtersItem.getEditors()) + " " +
                        "AND o_r_e_o.equipment_format IN " + Sql.listPrepared(filtersItem.getCatalogs()) + " " +
                        "AND o_r_e_o.equipment_diffusor IN " + Sql.listPrepared(filtersItem.getDistributors()) + ") " +
                        "OR o_r_e.id IS NOT NULL) ";
                values.addAll(new JsonArray(filtersItem.getEditors())).addAll(new JsonArray(filtersItem.getCatalogs())).addAll(new JsonArray(filtersItem.getDistributors()));
            } else if (!filtersItem.getEditors().isEmpty() && filtersItem.getDistributors().isEmpty()) {
                sqlquery += " AND (o_r_e_o.equipment_editor IN " + Sql.listPrepared(filtersItem.getEditors()) +
                        " OR o_r_e.id IS NOT NULL) ";
                values.addAll(new JsonArray(filtersItem.getEditors()));
            } else if (filtersItem.getEditors().isEmpty() && !filtersItem.getDistributors().isEmpty()) {
                sqlquery += " AND (o_r_e_o.equipment_diffusor IN " + Sql.listPrepared(filtersItem.getDistributors()) +
                        " OR o_r_e.id IS NOT NULL) ";
                values.addAll(new JsonArray(filtersItem.getDistributors()));
            } else if (filtersItem.getCatalogs().isEmpty() && !filtersItem.getDistributors().isEmpty()) {
                sqlquery += " AND (o_r_e_o.equipment_diffusor IN " + Sql.listPrepared(filtersItem.getDistributors()) +
                        " OR o_r_e.id IS NOT NULL) ";
                values.addAll(new JsonArray(filtersItem.getDistributors()));
            }
        }


        // Condition de filtrage de structures
        if (!filters.getIdsStructure().isEmpty()) {
            sqlquery += " AND (o_r_e.id_structure IN " + Sql.listPrepared(filters.getIdsStructure());
            sqlquery += " OR o_r_e_o.id_structure IN " + Sql.listPrepared(filters.getIdsStructure()) + " )";
            values.addAll(new JsonArray(filters.getIdsStructure())).addAll(new JsonArray(filters.getIdsStructure()));
        }

        // Condition de filtrage des campagnes
        if (!filters.getIdsCampaign().isEmpty()) {
            sqlquery += " AND (o_r_e.id_campaign IN " + Sql.listPrepared(filters.getIdsCampaign());
            sqlquery += " OR o_r_e_o.id_campaign IN " + Sql.listPrepared(filters.getIdsCampaign()) + " )";
            values.addAll(new JsonArray(filters.getIdsCampaign())).addAll(new JsonArray(filters.getIdsCampaign()));
        }

        // Condition de filtrage sur les commandes renouvellables
        if (filters.getRenew() != null) {
            if (filters.getRenew()) {
                sqlquery += " AND o_r_e_o.owner_id ~* 'renew' ";
            } else {
                sqlquery += " AND o_r_e_o.owner_id !~* 'renew' ";
            }
        }


        sqlquery = sqlquery + " GROUP BY p.id, creationDate";

        if (filters.getOrderBy() != null) {
            switch (filters.getOrderBy()) {
                case ID:
                    sqlquery += " ORDER BY id ";
                    break;
                case DATE:
                    sqlquery += " ORDER BY creationDate ";
                    break;
                case QUANTITY:
                    sqlquery += " ORDER BY nbOrders ";
                    break;
                case UAI:
                    sqlquery += " ORDER BY uai ";
                    break;
                case STRUCTURE_NAME:
                    sqlquery += " ORDER BY structure_name ";
                    break;
                case NAME:
                    sqlquery += " ORDER BY p.title ";
                    break;
            }
            sqlquery += (filters.getOrderDesc()) ? "DESC " : "ASC ";
        }

        if (filters.getPage() != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * filters.getPage());
            values.add(PAGE_SIZE);
        }

        Sql.getInstance().prepared(sqlquery, values, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));
        return promise.future();
    }

    @Override
    public List<TransactionElement> insertOldOrders(List<OrderRegionBeautifyModel> orderRegionBeautify, boolean isRenew) {
        return ListUtils.partition(orderRegionBeautify, 500).stream()
                .map(orderRegionsList -> this.insertOrderList(orderRegionsList, isRenew))
                .collect(Collectors.toList());
    }

    private TransactionElement insertOrderList(List<OrderRegionBeautifyModel> orderRegionsList, boolean isRenew) {
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment-old\"" +
                " (" +
                ((isRenew) ? "" : "id,") +
                "amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_order_client_equipment, id_project, reassort, id_status, total_free) VALUES ");

        for (OrderRegionBeautifyModel order : orderRegionsList) {
            if (order.getOrderRegion().getIdProject() != null) {
                query.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
                if (isRenew) {
                    query.append(") ,");
                } else {
                    params.add(order.getOrderRegion().getId());
                    query.append(", ?) ,");
                }

                params.add(order.getOrderRegion().getAmount())
                        .add(order.getOrderRegion().getCreationDate())
                        .add(order.getOrderRegion().getOwnerName())
                        .add(order.getOrderRegion().getOwnerId())
                        .add((!isRenew) ? "SENT" : order.getOrderRegion().getStatus())
                        .add(order.getOrderRegion().getEquipmentKey());
                setOrderValuesSQL(params, order);
                params.add(order.getOrderRegion().getIdOrderClientEquipment())
                        .add(order.getOrderRegion().getIdProject())
                        .add(order.getOrderRegion().getReassort())
                        .addNull() //Order region n'est pas encore envoyé a lde on ne peut pas avoir d'id status
                        .add(order.getTotalFree());
            }
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        return new TransactionElement(query.toString(), params);
    }

    @Deprecated
    @Override
    public List<TransactionElement> insertOldOrders(JsonArray orderRegions, boolean isRenew) {
        List<JsonObject> allOrderRegionsList = orderRegions.stream().map(JsonObject.class::cast).collect(Collectors.toList());

        return ListUtils.partition(allOrderRegionsList, 500).stream()
                .map(orderRegionsList -> this.insertOldOrderList(orderRegionsList, isRenew))
                .collect(Collectors.toList());
    }

    @Deprecated
    private TransactionElement insertOldOrderList(List<JsonObject> orderRegionsList, boolean isRenew) {
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment-old\"" +
                " (" +
                ((isRenew) ? "" : "id,") +
                "amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_order_client_equipment, id_project, reassort, id_status, total_free) VALUES ");

        for (JsonObject order : orderRegionsList) {
            if (order.containsKey("id_project")) {
                query.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
                String creation_date;
                if (isRenew) {
                    query.append(") ,");
                    creation_date = DateHelper.convertStringDateToOtherFormat(order.getString("creation_date"), DateHelper.DAY_FORMAT, DateHelper.SQL_FORMAT);
                } else {
                    params.add(order.getLong(Field.ID));
                    query.append(", ?) ,");
                    creation_date = DateHelper.convertStringDateToOtherFormat(order.getString("creation_date"), DateHelper.DAY_FORMAT_DASH, DateHelper.SQL_FORMAT);
                }

                params.add(order.getInteger("amount"))
                        .add(creation_date)
                        .add(order.getString("owner_name"))
                        .add(order.getString("owner_id"))
                        .add((!isRenew) ? "SENT" : order.getString(Field.STATUS))
                        .add(order.getString("equipment_key"));
                setOrderValuesSQL(params, order);
                params.add(order.getInteger("id_order_client_equipment"))
                        .add(order.getLong("id_project"))
                        .add(order.getBoolean("reassort"))
                        .add(order.getInteger("id_status"))
                        .add(order.getInteger("total_free", null));
            }
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        return new TransactionElement(query.toString(), params);
    }

    private String selectOrderRegion(boolean old) {
        String selectOld = old ? ", ore.equipment_image as image, ore.equipment_name as name, ore.equipment_price as price, oce.offers as offers, s.name as status_name, s.id as status_id " : "";
        String query = "SELECT ore.*, to_json(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, p.title AS title, " +
                "to_json(oce.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2 " + selectOld +
                "FROM  " + Crre.crreSchema + (old ? ".\"order-region-equipment-old\"" : ".\"order-region-equipment\"") + " AS ore " +
                "LEFT JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".students st on (oce.id_structure = st.id_structure) ";
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
        query += "GROUP BY ore.id, campaign.name, campaign.use_credit, campaign.*, p.title, oce.id, bo.name, bo.id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, " +
                "st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2";
        if (old) {
            query += ", s.name, s.id";
        }
        return query;
    }

    private void setOrderValuesSQL(JsonArray params, OrderRegionBeautifyModel orderRegionBeautify) {
        params.add(orderRegionBeautify.getName())
                .add(orderRegionBeautify.getImage())
                .add(orderRegionBeautify.getUnitedPriceTTC())
                .add(orderRegionBeautify.getGrade())
                .add(orderRegionBeautify.getEditor())
                .add(orderRegionBeautify.getDiffusor())
                .add(orderRegionBeautify.getType())
                .add(orderRegionBeautify.getOrderRegion().getIdCampaign())
                .add(orderRegionBeautify.getOrderRegion().getIdStructure())
                .add(orderRegionBeautify.getOrderRegion().getComment());
    }

    @Deprecated
    private void setOrderValuesSQL(JsonArray params, JsonObject order) {
        params.add(order.getString(Field.NAME))
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
    public List<TransactionElement> insertOldClientOrders(List<OrderRegionBeautifyModel> orderRegionBeautifyList) {
        return ListUtils.partition(orderRegionBeautifyList, 500).stream()
                .map(this::insertOldClientOrderList)
                .collect(Collectors.toList());
    }

    private TransactionElement insertOldClientOrderList(List<OrderRegionBeautifyModel> orderRegionBeautifyList) {
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("" +
                " INSERT INTO " + Crre.crreSchema + ".\"order_client_equipment_old\"" +
                " (id, amount, creation_date, user_id," +
                " status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure," +
                " comment, id_basket, reassort, offers, equipment_tva5, equipment_tva20, equipment_priceht) VALUES ");
        for (OrderRegionBeautifyModel order : orderRegionBeautifyList) {
            if (order.getOrderRegion().getIdProject() != null) {
                query.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),");
                params.add(order.getOrderRegion().getIdOrderClientEquipment())
                        .add(order.getOrderRegion().getAmount())
                        .add(order.getBasketOrder().getCreated())
                        .add(order.getOrderRegion().getOwnerId())
                        .add("SENT")
                        .add(order.getOrderRegion().getEquipmentKey());
                setOrderValuesSQL(params, order);

                JsonArray offers = new JsonArray(order.getOffers().stream().map(OrderRegionBeautifyModel::toJsonOffer).collect(Collectors.toList()));
                params.add(order.getBasketOrder().getId())
                        .add(order.getOrderRegion().getReassort())
                        .add(offers)
                        .add(order.getTva5())
                        .add(order.getTva20())
                        .add(order.getPriceht());
            }
        }
        query = new StringBuilder(query.substring(0, query.length() - 1));
        return new TransactionElement(query.toString(), params);
    }

    private static void launchSQL(JsonArray params, StringBuilder query, Handler<Either<String, JsonObject>> handler) {
        Sql.getInstance().prepared(query.toString(), params, new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L),
                SqlResult.validUniqueResultHandler(handler));
    }

    private void formatOffers(JsonObject order) {
        for (int i = 0; i < order.getJsonArray("offers").size(); i++) {
            JsonObject offer = order.getJsonArray("offers").getJsonObject(i);
            offer.put(Field.ID, "F" + order.getLong("id_order_client_equipment") + "_" + i);
        }
    }

    @Override
    public Future<JsonObject> updateOrdersStatus(List<Integer> ids, String status, String justification) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "UPDATE " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " SET  status = ?, cause_status = ?" +
                " WHERE id in " + Sql.listPrepared(ids.toArray()) + " ; ";

        query += "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET  status = ?, cause_status = ? " +
                "WHERE id in ( SELECT ore.id_order_client_equipment FROM " + Crre.crreSchema + ".\"order-region-equipment\" ore " +
                "WHERE id in " + Sql.listPrepared(ids.toArray()) + " );";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add(id);
        }
        params.add(status.toUpperCase()).add(justification);
        for (Integer id : ids) {
            params.add(id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public void updateOldOrders(JsonArray ordersRegion, final Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "";
        for (int i = 0; i < ordersRegion.size(); i++) {
            query += "UPDATE " + Crre.crreSchema + ".\"order-region-equipment-old\" " +
                    " SET id_status = ?" +
                    " WHERE id = ?; ";
            params.add(ordersRegion.getJsonObject(i).getString(Field.STATUS));
            params.add(ordersRegion.getJsonObject(i).getString(Field.ID));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public Future<JsonObject> updateOldOrdersWithTransaction(JsonArray ordersRegion) {
        Promise<JsonObject> promise = Promise.promise();
        JsonArray params = new JsonArray();
        String query = "";
        final Map<String, List<JsonObject>> statusIdOrderMap = ordersRegion.stream()
                .map(JsonObject.class::cast)
                .collect(Collectors.groupingBy(order -> order.getString(Field.STATUS)));

        for (String statusId : statusIdOrderMap.keySet()) {
            List<String> idOrderList = statusIdOrderMap.get(statusId).stream()
                    .map(order -> order.getString(Field.ID))
                    .collect(Collectors.toList());
            query += "BEGIN;";
            query += "UPDATE " + Crre.crreSchema + ".\"order-region-equipment-old\" " +
                    " SET id_status = ?" +
                    " WHERE id IN " + Sql.listPrepared(idOrderList) + ";";
            query += "COMMIT;";
            params.add(statusId);
            params.addAll(new JsonArray(idOrderList));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonObject> updateOldOrderLDEModel(List<OrderLDEModel> listOrder) {
        Promise<JsonObject> promise = Promise.promise();

        List<JsonObject> ordersRegion = listOrder.stream()
                .map(orderLDEModel -> {
                    if (!orderLDEModel.getEtat().isEmpty() && NumberUtils.isParsable(orderLDEModel.getEtat()) &&
                            !orderLDEModel.getCGIId().isEmpty() && NumberUtils.isParsable(orderLDEModel.getCGIId()) &&
                            !orderLDEModel.getCGIId().equals("0")) {
                        return new JsonObject()
                                .put(Field.STATUS, orderLDEModel.getEtat())
                                .put(Field.ID, orderLDEModel.getCGIId());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ordersRegion.isEmpty()) {
            return Future.succeededFuture();
        }

        this.updateOldOrdersWithTransaction(new JsonArray(ordersRegion))
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::updateOldOrderLDEModel] Failed to update order: %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    @Override
    public List<TransactionElement> deletedOrders(List<Long> ordersClientIdList, String table) {
        List<Long> allOrderRegionsList = ordersClientIdList.stream().map(Long.class::cast).collect(Collectors.toList());

        return ListUtils.partition(allOrderRegionsList, 25000).stream()
                .map(ordersClientList -> this.deletedOrderList(ordersClientList, table))
                .collect(Collectors.toList());
    }

    private TransactionElement deletedOrderList(List<Long> ordersClientIdList, String table) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder("DELETE FROM " + Crre.crreSchema + ".\"" + table + "\" as t " +
                "WHERE t.id IN ( ");
        for (Long orderClientId : ordersClientIdList) {
            query.append("?,");
            params.add(orderClientId);
        }
        query = new StringBuilder(query.substring(0, query.length() - 1) + ")");
        return new TransactionElement(query.toString(), params);
    }

    private void deletedOrdersRecursive(JsonArray orders, String table, int e, Handler<Either<String, JsonObject>> handlerJsonObject) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder("DELETE FROM " + Crre.crreSchema + ".\"" + table + "\" as t " +
                "WHERE t.id IN ( ");
        for (int i = e * 25000; i < min((e + 1) * 25000, orders.size()); i++) {
            query.append("?,");
            params.add(orders.getLong(i));
        }
        query = new StringBuilder(query.substring(0, query.length() - 1) + ")");
        launchSQL(params, query, sql -> {
            if (sql.isRight()) {
                if ((e + 1) * 25000 >= orders.size()) {
                    handlerJsonObject.handle(new Either.Right<>(new JsonObject()));
                } else {
                    deletedOrdersRecursive(orders, table, e + 1, handlerJsonObject);
                }
            } else {
                handlerJsonObject.handle(new Either.Left<>("[CRRE@DefaultOrderRegionService.deletedOrdersRecursive] " +
                        "An error has occurred : " + sql.left().getValue()));
            }
        });
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
            params.add(listIdOrders.getJsonObject(i).getInteger(Field.STATUS)).add(listIdOrders.getJsonObject(i).getInteger(Field.ID));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }

    @Override
    public void setIdOrderRegion(Handler<Either<String, JsonObject>> handlerJsonObject) {
        String query = "SELECT SETVAL((SELECT PG_GET_SERIAL_SEQUENCE('" + Crre.crreSchema + ".project', 'id')), (SELECT (count(*) + 1) FROM " + Crre.crreSchema + ".project), FALSE);" +
                "SELECT SETVAL((SELECT PG_GET_SERIAL_SEQUENCE('" + Crre.crreSchema + ".\"order-region-equipment-old\"', 'id')), (SELECT max(id) FROM " + Crre.crreSchema + ".\"order-region-equipment-old\") + 1, FALSE);" +
                "SELECT SETVAL((SELECT PG_GET_SERIAL_SEQUENCE('" + Crre.crreSchema + ".\"order-region-equipment\"', 'id')), (SELECT max(id) FROM " + Crre.crreSchema + ".\"order-region-equipment-old\")+1, FALSE);" +
                "SELECT SETVAL((SELECT PG_GET_SERIAL_SEQUENCE('" + Crre.crreSchema + ".structure_group', 'id')), (SELECT (count(*) + 1) FROM " + Crre.crreSchema + ".structure_group), FALSE);";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handlerJsonObject));
    }

    @Override
    public Future<Map<ProjectModel, List<OrderRegionEquipmentModel>>> getOrderRegionEquipmentInSameProject
            (List<Integer> projectIdList, boolean old) {
        if (projectIdList.isEmpty()) {
            return Future.succeededFuture(new HashMap<>());
        }

        Promise<Map<ProjectModel, List<OrderRegionEquipmentModel>>> promise = Promise.promise();

        String query = "SELECT array_to_json(array_agg(o_r_e.*)), row_to_json(p.*) as project " +
                "FROM crre.\"order-region-equipment" + (old ? "-old" : "") + "\" o_r_e " +
                "INNER JOIN crre.project p on o_r_e.id_project = p.id " +
                "WHERE p.id IN " + Sql.listPrepared(projectIdList) + " " +
                "GROUP BY p.id";

        Sql.getInstance().prepared(query, new JsonArray(projectIdList), SqlResult.validResultHandler(stringJsonArrayEither -> {
            if (stringJsonArrayEither.isRight()) {
                promise.complete(stringJsonArrayEither.right().getValue().stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .collect(Collectors.toMap(jsonObject -> IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.PROJECT)), ProjectModel.class),
                                jsonObject -> IModelHelper.toList(new JsonArray(jsonObject.getString(Field.ARRAY_TO_JSON)), OrderRegionEquipmentModel.class))));
            } else {
                promise.fail(stringJsonArrayEither.left().getValue());
            }
        }));

        return promise.future();
    }

    @Override
    public List<OrderRegionBeautifyModel> orderResultToBeautifyModel(JsonArray structures, List<OrderRegionComplex> orderRegionComplexList, JsonArray equipments) {
        return orderRegionComplexList.stream()
                .flatMap(order -> {
                    OrderRegionBeautifyModel orderRegionBeautifyModel = new OrderRegionBeautifyModel();
                    orderRegionBeautifyModel.setOrderRegionComplex(order);
                    orderRegionBeautifyModel.setId(order.getOrderRegion().getId().toString());
                    orderRegionBeautifyModel.setTitle(order.getProject().getTitle());

                    JsonObject equipment = JsonHelper.jsonArrayToList(equipments, JsonObject.class).stream()
                            .filter(equipmentElement -> equipmentElement.getString(Field.ID).equals(order.getOrderRegion().getEquipmentKey()))
                            .findFirst()
                            .orElse(null);
                    if (equipment == null) {
                        return Stream.of(orderRegionBeautifyModel);
                    }

                    JsonObject priceDetails = getPriceTtc(equipment);
                    DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
                    DecimalFormat df2 = new DecimalFormat("#.##", dfs);
                    double priceTTC = priceDetails.getDouble("priceTTC") * order.getOrderRegion().getAmount();
                    double priceHT = priceDetails.getDouble("prixht") * order.getOrderRegion().getAmount();
                    orderRegionBeautifyModel.setPriceht(Double.parseDouble(df2.format(priceDetails.getDouble("prixht"))));
                    orderRegionBeautifyModel.setTva5((priceDetails.containsKey("partTVA5")) ?
                            Double.parseDouble(df2.format(priceDetails.getDouble("partTVA5") + priceDetails.getDouble("prixht"))) : null);
                    orderRegionBeautifyModel.setTva20((priceDetails.containsKey("partTVA20")) ?
                            Double.parseDouble(df2.format(priceDetails.getDouble("partTVA20") + priceDetails.getDouble("prixht"))) : null);
                    orderRegionBeautifyModel.setUnitedPriceTTC(Double.parseDouble(df2.format(priceDetails.getDouble("priceTTC"))));
                    orderRegionBeautifyModel.setPrice(Double.parseDouble(df2.format(priceDetails.getDouble("priceTTC"))));
                    orderRegionBeautifyModel.setTotalPriceHT(Double.parseDouble(df2.format(priceHT)));
                    orderRegionBeautifyModel.setTotalPriceTTC(Double.parseDouble(df2.format(priceTTC)));

                    orderRegionBeautifyModel.setName(equipment.containsKey("titre") ? equipment.getString("titre", "") : "");
                    orderRegionBeautifyModel.setImage(equipment.containsKey("urlcouverture") ? equipment.getString("urlcouverture", "") : "");
                    orderRegionBeautifyModel.setEan(equipment.containsKey("ean") ? equipment.getString("ean", "") : "");
                    orderRegionBeautifyModel.setEditor(equipment.containsKey("editeur") ? equipment.getString("editeur", "") : "");
                    orderRegionBeautifyModel.setDiffusor(equipment.containsKey("distributeur") ? equipment.getString("distributeur", "") : "");
                    orderRegionBeautifyModel.setType(equipment.containsKey("type") ? equipment.getString("type", "") : "");
                    if (equipment.getJsonArray("disciplines", new JsonArray()).size() > 0) {
                        orderRegionBeautifyModel.setGrade(equipment.getJsonArray("disciplines").getJsonObject(0).getString("libelle"));
                    } else {
                        orderRegionBeautifyModel.setGrade("");
                    }
                    if ("articlenumerique".equals(equipment.getString("type"))) {
                        orderRegionBeautifyModel.setEanLDE(equipment.getJsonArray("offres").getJsonObject(0).getString("eanlibraire"));
                    } else {
                        orderRegionBeautifyModel.setEanLDE(equipment.getString("ean"));
                    }

                    putStructuresNameUAI(structures, orderRegionBeautifyModel);
                    getUniqueTypeCatalogue(orderRegionBeautifyModel, order, equipment);

                    List<OrderRegionBeautifyModel> offers = new ArrayList<>();
                    if ("articlenumerique".equals(equipment.getString("type"))) {
                        offers = computeOffers(orderRegionBeautifyModel, equipment);
                        int totalFree = offers.stream().map(orderRegionOffer -> orderRegionOffer.getOrderRegion().getAmount()).reduce(0, Integer::sum);
                        orderRegionBeautifyModel.setTotalFree(totalFree);
                    }
                    orderRegionBeautifyModel.setOffers(new ArrayList<>(offers));
                    offers.add(orderRegionBeautifyModel);
                    return offers.stream();
                }).sorted(Comparator.comparing(OrderRegionBeautifyModel::getId)).collect(Collectors.toList());
    }

    private void getUniqueTypeCatalogue(OrderRegionBeautifyModel orderRegionBeautifyModel, OrderRegionComplex order, JsonObject equipment) {
        if (equipment.getString("typeCatalogue", "").contains("|")) {
            if (order.getCampaign().getUseCredit() != null && order.getCampaign().getUseCredit().contains("consumable")) {
                if ("articlepapier".equals(equipment.getString("type"))) {
                    orderRegionBeautifyModel.setTypeCatalogue("AO_IDF_CONSO");
                } else {
                    orderRegionBeautifyModel.setTypeCatalogue("Consommable");
                }
            } else {
                if ("articlepapier".equals(equipment.getString("type"))) {
                    if ((order.getCampaign().getCatalog() != null && order.getCampaign().getCatalog().contains("pro")) ||
                            !equipment.getString("typeCatalogue").contains("AO_IDF_PAP")) {
                        orderRegionBeautifyModel.setTypeCatalogue("AO_IDF_PAP_PRO");
                    } else {
                        orderRegionBeautifyModel.setTypeCatalogue("AO_IDF_PAP");
                    }
                } else {
                    if (ArrayUtils.contains(equipment.getString("typeCatalogue").split(Pattern.quote("|")), "Ressource")) {
                        orderRegionBeautifyModel.setTypeCatalogue("Ressource");
                    } else {
                        orderRegionBeautifyModel.setTypeCatalogue("Numerique");
                    }
                }
            }
        } else {
            orderRegionBeautifyModel.setTypeCatalogue(equipment.getString("typeCatalogue"));
        }
    }

    private void putStructuresNameUAI(JsonArray structures, OrderRegionBeautifyModel orderRegionBeautifyModel) {
        JsonObject structure = structures.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(structureElement -> structureElement.getString(Field.ID, "").equals(orderRegionBeautifyModel.getOrderRegion().getIdStructure()))
                .findFirst()
                .orElse(null);
        if (structure != null) {
            orderRegionBeautifyModel.setUaiStructure(structure.getString("uai"));
            orderRegionBeautifyModel.setNameStructure(structure.getString(Field.NAME));
            orderRegionBeautifyModel.setAddressStructure(structure.getString("address"));
        }
    }

    private void putEANLDE(JsonObject equipment, JsonObject order) {
        if (equipment.getString("type").equals("articlenumerique")) {
            order.put("eanLDE", equipment.getJsonArray("offres").getJsonObject(0).getString("eanlibraire"));
        } else {
            order.put("eanLDE", equipment.getString("ean"));
        }
    }

    private List<OrderRegionBeautifyModel> computeOffers(OrderRegionBeautifyModel orderRegionBeautifyModel, JsonObject equipment) {
        List<OrderRegionBeautifyModel> offers = new ArrayList<>();
        if (equipment.getJsonArray("offres").getJsonObject(0).getJsonArray("leps").size() > 0) {
            JsonArray leps = equipment.getJsonArray("offres").getJsonObject(0).getJsonArray("leps");
            Integer amount = orderRegionBeautifyModel.getOrderRegion().getAmount();
            int gratuit = 0;
            int gratuite = 0;
            for (int i = 0; i < leps.size(); i++) {
                JsonObject offer = leps.getJsonObject(i);
                JsonArray conditions = offer.getJsonArray("conditions");
                OrderRegionBeautifyModel orderRegionBeautifyModelOffer = new OrderRegionBeautifyModel()
                        .setTitle(offer.getJsonArray("licence").getJsonObject(0).getString("valeur"));
                if (conditions.size() > 1) {
                    for (int j = 0; j < conditions.size(); j++) {
                        int condition = conditions.getJsonObject(j).getInteger("conditionGratuite");
                        if (amount >= condition && gratuit < condition) {
                            gratuit = condition;
                            gratuite = conditions.getJsonObject(j).getInteger("gratuite");
                        }
                    }
                } else if (offer.getJsonArray("conditions").size() == 1) {
                    gratuit = offer.getJsonArray("conditions").getJsonObject(0).getInteger("conditionGratuite");
                    gratuite = (int) (offer.getJsonArray("conditions").getJsonObject(0).getInteger("gratuite") * Math.floor(amount / gratuit));
                }
                //add custom value for order
                orderRegionBeautifyModelOffer.setOrderRegionComplex(orderRegionBeautifyModel.getOrderRegionComplex());
                orderRegionBeautifyModelOffer.getOrderRegion().setIdProject(null);
                orderRegionBeautifyModelOffer.getOrderRegion().setReassort(null);
                orderRegionBeautifyModelOffer.setId("F" + orderRegionBeautifyModel.getOrderRegion().getId() + "_" + i);
                orderRegionBeautifyModelOffer.getOrderRegion().setAmount(gratuite);
                orderRegionBeautifyModelOffer.getOrderRegion().setComment(equipment.getString("ean"));
                orderRegionBeautifyModelOffer.setEan(offer.getString("ean"));
                orderRegionBeautifyModelOffer.setName(offer.getString("titre"));
                orderRegionBeautifyModelOffer.setUnitedPriceTTC(0.0);
                orderRegionBeautifyModelOffer.setTotalPriceHT(0.0);
                orderRegionBeautifyModelOffer.setTotalPriceTTC(0.0);
                orderRegionBeautifyModelOffer.setTypeCatalogue(orderRegionBeautifyModel.getTypeCatalogue());
                orderRegionBeautifyModelOffer.setUaiStructure(orderRegionBeautifyModel.getUaiStructure());
                orderRegionBeautifyModelOffer.setNameStructure(orderRegionBeautifyModel.getNameStructure());
                orderRegionBeautifyModelOffer.setAddressStructure(orderRegionBeautifyModel.getAddressStructure());

                if (gratuite > 0) {
                    offers.add(orderRegionBeautifyModelOffer);
                }
            }
        }
        return offers;
    }

    @Override
    public JsonObject generateExport(List<OrderRegionBeautifyModel> logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getExportHeader());
        HashSet<String> structures = new HashSet<>();
        for (OrderRegionBeautifyModel orderRegionBeautifyModel : logs) {
            report.append(generateExportLine(orderRegionBeautifyModel));
            if (!StringUtils.isEmpty(orderRegionBeautifyModel.getUaiStructure())) {
                structures.add(orderRegionBeautifyModel.getUaiStructure());
            }
        }
        return new JsonObject()
                .put("csvFile", report.toString())
                .put("nbEtab", structures.size());
    }

    private String getExportHeader() {
        return "ID unique" + ";" +
                "Date;" +
                "Nom étab;" +
                "UAI de l'étab;" +
                "Adresse de livraison;" +
                "Nom commande;" +
                "Campagne;" +
                "EAN de la ressource;" +
                "Titre de la ressource;" +
                "Editeur;" +
                "Distributeur;" +
                "Numerique;" +
                "Id de l'offre choisie;" +
                "Type;" +
                "Reassort;" +
                "Quantité;" +
                "Prix HT de la ressource;" +
                "Part prix 5,5%;" +
                "Part prix 20%;" +
                "Prix unitaire TTC;" +
                "Montant total HT;" +
                "Prix total TTC;" +
                "Commentaire;" +
                "2nde Generale;" +
                "1ere Generale;" +
                "Terminale Generale;" +
                "2nde Technologique;" +
                "1ere Technologique;" +
                "Terminale Technologique;" +
                "2nde Professionnelle;" +
                "1ere Professionnelle;" +
                "Terminale Professionnelle;" +
                "BMA 1ere annee;" +
                "BMA 2nde annee;" +
                "CAP 1ere annee;" +
                "CAP 2eme annee;" +
                "CAP 3eme annee" +
                "\n";
    }

    private String generateExportLine(OrderRegionBeautifyModel orderRegionBeautifyModel) {
        return orderRegionBeautifyModel.getId() + ";" +
                (orderRegionBeautifyModel.getOrderRegion().getCreationDate() != null ? orderRegionBeautifyModel.getOrderRegion().getCreationDate() : "") + ";" +
                (orderRegionBeautifyModel.getNameStructure() != null ? orderRegionBeautifyModel.getNameStructure() : "") + ";" +
                (orderRegionBeautifyModel.getUaiStructure() != null ? orderRegionBeautifyModel.getUaiStructure() : "") + ";" +
                (orderRegionBeautifyModel.getAddressStructure() != null ? orderRegionBeautifyModel.getAddressStructure() : "") + ";" +
                (orderRegionBeautifyModel.getTitle() != null ? orderRegionBeautifyModel.getTitle() : "") + ";" +
                (orderRegionBeautifyModel.getCampaign().getName() != null ? orderRegionBeautifyModel.getCampaign().getName() : "") + ";" +
                (orderRegionBeautifyModel.getEan() != null ? orderRegionBeautifyModel.getEan() : "") + ";" +
                (orderRegionBeautifyModel.getName() != null ? orderRegionBeautifyModel.getName() : "") + ";" +
                (orderRegionBeautifyModel.getEditor() != null ? orderRegionBeautifyModel.getEditor() : "") + ";" +
                (orderRegionBeautifyModel.getDiffusor() != null ? orderRegionBeautifyModel.getDiffusor() : "") + ";" +
                (orderRegionBeautifyModel.getType() != null ? orderRegionBeautifyModel.getType() : "") + ";" +
                (orderRegionBeautifyModel.getEanLDE() != null ? orderRegionBeautifyModel.getEanLDE() : "") + ";" +
                (orderRegionBeautifyModel.getTypeCatalogue() != null ? orderRegionBeautifyModel.getTypeCatalogue() : "") + ";" +
                (orderRegionBeautifyModel.getOrderRegion().getReassort() != null ? (orderRegionBeautifyModel.getOrderRegion().getReassort() ? "Oui" : "Non") : "") + ";" +
                exportPriceComment(orderRegionBeautifyModel) +
                exportStudents(orderRegionBeautifyModel) +
                "\n";
    }

    @Deprecated
    private String generateExportLine(JsonObject log) {
        return (log.containsKey("id_project") ? log.getLong(Field.ID).toString() : log.getString(Field.ID)) + ";" +
                (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("name_structure") != null ? log.getString("name_structure") : "") + ";" +
                (log.getString("uai_structure") != null ? log.getString("uai_structure") : "") + ";" +
                (log.getString("address_structure") != null ? log.getString("address_structure") : "") + ";" +
                (log.getString(Field.TITLE) != null ? log.getString(Field.TITLE) : "") + ";" +
                (log.getString("campaign_name") != null ? log.getString("campaign_name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getString(Field.NAME) != null ? log.getString(Field.NAME) : "") + ";" +
                (log.getString("editor") != null ? log.getString("editor") : "") + ";" +
                (log.getString("diffusor") != null ? log.getString("diffusor") : "") + ";" +
                (log.getString("type") != null ? log.getString("type") : "") + ";" +
                (log.getString("eanLDE") != null ? log.getString("eanLDE") : "") + ";" +
                (log.getString("typeCatalogue") != null ? log.getString("typeCatalogue") : "") + ";" +
                (log.getBoolean("reassort") != null ? (log.getBoolean("reassort") ? "Oui" : "Non") : "") + ";" +
                exportPriceComment(log) +
                exportStudents(log) +
                "\n";
    }
}
