package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

//import static fr.openent.crre.helpers.ElasticSearchHelper.filter_waiting;
import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
    private final PurseService purseService ;
    private final EmailSendService emailSender ;
    private final StructureService structureService;

    public DefaultOrderService(
            String schema, String table, EmailSender emailSender){
        super(schema,table);
        this.purseService = new DefaultPurseService();
        this.emailSender = new EmailSendService(emailSender);
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Override
    public void listOrder(Integer idCampaign, String idStructure, UserInfos user,  Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT oe.equipment_key, oe.id as id, oe.comment, oe.price_proposal, oe.price, oe.tax_amount, oe.amount,to_char(oe.creation_date, 'dd-MM-yyyy') creation_date, oe.id_campaign," +
                " oe.id_structure, oe.name, oe.summary, oe.image, oe.status, oe.id_contract, oe.rank, oe.id_basket, " +
                " array_to_json(array_agg(order_opts)) as options," +
                "array_to_json(array_agg(DISTINCT order_file.*)) as files  " +
                "FROM "+ Crre.crreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN "+ Crre.crreSchema + ".order_client_options order_opts ON " +
                "oe.id = order_opts.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".order_file ON oe.id = order_file.id_order_client_equipment " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign ON oe.id_campaign = campaign.id " +
                "WHERE id_campaign = ? AND id_structure = ?";
        if(user != null){
            query += "AND user_id = ? ";
        }
        query +="GROUP BY ( oe.id,campaign.priority_enabled) " +
                "ORDER BY CASE WHEN campaign.priority_enabled = false " +
                "THEN oe.creation_date END ASC";

        values.add(idCampaign).add(idStructure);
        if(user != null)
            values.add(user.getUserId());

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }

    @Override
    public  void listOrder(String status, Handler<Either<String, JsonArray>> handler){
        String query = "SELECT oce.* , bo.name as basket_name, bo.name_user as user_name, to_json(campaign.* ) campaign,  array_to_json(array_agg( DISTINCT oco.*)) as options, " +
                "array_to_json(array_agg( distinct structure_group.name)) as structure_groups," +
                "             ROUND((( SELECT CASE          " +
                "            WHEN oce.price_proposal IS NOT NULL THEN 0     " +
                "            WHEN oce.override_region IS NULL THEN 0 " +
                "            WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0         " +
                "            ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)         " +
                "            END           " +
                "             FROM   " + Crre.crreSchema + ".order_client_options oco  " +
                "              where oco.id_order_client_equipment = oce.id " +
                "             ) + oce.price + oce.price * oce.tax_amount/100 " +
                "              ) * oce.amount   ,2 ) " +
                "             as Total, "+
                " array_to_json(array_agg(DISTINCT order_file.*)) as files " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo " +
                "ON bo.id = oce.id_basket " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                " LEFT JOIN " + Crre.crreSchema + ".order_file ON oce.id = order_file.id_order_client_equipment ";
                if(!status.contains("ALL")) {
                    query += "WHERE oce.status = ? ";
                }
                query += "GROUP BY (bo.name, bo.name_user, oce.id, campaign.id) " +
                         "ORDER BY oce.id; ";
                if(!status.contains("ALL")) {
                    sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(status), SqlResult.validResultHandler(handler));
                } else {
                    sql.raw(query, SqlResult.validResultHandler(handler));
                }
    }

    @Override
    public void listOrders(List<Integer> ids, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT oce.* , oce.price * oce.amount as total_price , " +
                "to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign, " +
                "array_to_json(array_agg(  oco.*)) as options " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "LEFT JOIN "+ Crre.crreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN "+ Crre.crreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN " + Crre.crreSchema + ".supplier ON contract.id_supplier = supplier.id " +
                "INNER JOIN "+ Crre.crreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +
                " GROUP BY ( oce.id, gr.id, contract.id, supplier.id, campaign.id); " +
                "ORDER BY oce.id; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add( id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresId(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, id_structure " +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE number_validation IN " + Sql.listPrepared(ids.getList()) + ";";

        Sql.getInstance().prepared(query, ids, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrders(JsonArray ids, String structureId, Boolean isNumberValidation, Boolean groupByStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount " + (groupByStructure ? ", id_structure " : "") +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE " + (isNumberValidation ? "number_validation" : "id") + " IN " + Sql.listPrepared(ids.getList());
        if (structureId != null) {
            query += "AND id_structure = ?";
        }
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract " + (groupByStructure ? ", id_structure " : "") +
                "UNION " +
                "SELECT opt.price, opt.tax_amount, opt.name, opt.id_contract, SUM(opt.amount) as amount " +
                (structureId != null || groupByStructure ? ", equipment.id_structure " : "") +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment " + (groupByStructure ? ", equipment.id_structure " : "") +
                "FROM " + Crre.crreSchema + ".order_client_options options " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE " + (isNumberValidation ? "number_validation" : "id_order_client_equipment") + " IN " + Sql.listPrepared(ids.getList()) +
                (structureId != null ? " AND equipment.id_structure = ?" : "") +
                ") as opt";
        query += (groupByStructure || structureId != null ? " INNER JOIN " + Crre.crreSchema + ".order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" : "");
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract" + (groupByStructure ? ", equipment.id_structure" : "");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < ids.size(); j++) {
                if (isNumberValidation) {
                    params.add(ids.getString(j));
                } else {
                    params.add(ids.getInteger(j));
                }
            }
            if (structureId != null) {
                params.add(structureId);
            }
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getOrderByValidatioNumber(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE number_validation IN " + Sql.listPrepared(ids.getList());

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < ids.size(); i++) {
            params.add(ids.getString(i));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersGroupByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT row.number_validation, row.status, contract.name as contract_name, contract.id as id_contract, supplier.name as supplier_name, " +
                "array_to_json(array_agg(structure_group.name)) as structure_groups, count(distinct row.id_structure) as structure_count, supplier.id as supplierId, " +
                Crre.crreSchema + ".order.label_program, " + Crre.crreSchema + ".order.order_number " +
                "FROM " + Crre.crreSchema + ".order_client_equipment row " +
                "INNER JOIN " + Crre.crreSchema + ".contract ON (row.id_contract = contract.id) " +
                "INNER JOIN " + Crre.crreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (row.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Crre.crreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id) " +
                "LEFT OUTER JOIN " + Crre.crreSchema + ".order ON (row.id_order = " + Crre.crreSchema + ".order.id)  " +
                "WHERE row.status IN " + Sql.listPrepared(status.getList()) +
                " GROUP BY row.number_validation, contract.name, supplier.name, contract.id, supplierId, row.status, " + Crre.crreSchema +
                ".order.label_program, " + Crre.crreSchema + ".order.order_number;";

        this.sql.prepared(query, status, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersDetailsIndexedByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, amount::text, number_validation " +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE status IN " + Sql.listPrepared(status.getList()) +
                " UNION ALL " +
                "SELECT order_client_options.price, order_client_options.tax_amount, order_client_equipment.amount::text, order_client_equipment.number_validation " +
                "FROM " + Crre.crreSchema + ".order_client_options " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment ON (order_client_equipment.id = order_client_options.id_order_client_equipment) " +
                "WHERE order_client_equipment.status IN " + Sql.listPrepared(status.getList());

        JsonArray statusList = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < status.size(); j++) {
                statusList.add(status.getString(j));
            }
        }

        this.sql.prepared(query, statusList, SqlResult.validResultHandler(handler));
    }

    @Override
    public void cancelValidation(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment SET number_validation = '', status = 'WAITING' ,id_order = NULL " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.getList());

        this.sql.prepared(query, validationNumbers, SqlResult.validUniqueResultHandler(handler));
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


/*    @Override
    public void updateTotalAmount(Integer id, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String query = " UPDATE " + Crre.crreSchema + ".basket_order bo " +
                " SET amount = subquery.sum " +
                " FROM (SELECT bo.id, sum(oe.amount) "+
                " FROM " + Crre.crreSchema + ".basket_order bo " +
                " JOIN " + Crre.crreSchema + ".order_client_equipment oe " +
                " ON (bo.id = oe.id_basket) " +
                " WHERE bo.id = (SELECT bo.id " +
                " FROM " + Crre.crreSchema + ".basket_order bo " +
                " JOIN " + Crre.crreSchema + ".order_client_equipment oe " +
                " ON (bo.id = oe.id_basket) " +
                " WHERE oe.id = ?) " +
                " GROUP BY bo.id) AS subquery" +
                " WHERE bo.id = subquery.id" +
                " RETURNING bo.*;";

        values.add(id);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }*/
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
    public void updatePriceProposal(Integer id, Double price_proposal, Handler<Either<String, JsonObject>> handler) {
        JsonArray values;
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment" +
                "SET price_proposal = " + (price_proposal == null ? " null " : " ? ") +
                "WHERE id = ?;";
        values = price_proposal == null ? new JsonArray().add(id) : new JsonArray().add(price_proposal).add(id);
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

        String query = "SELECT oe.price, oe.tax_amount, oe.amount, oe.creation_date, oe.status, oe.equipment_key, oe.comment, bo.name as basket_name " +
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
    public void orderForDelete(Integer idOrder, Handler<Either<String, JsonObject>> handler) {

        String query = "SELECT  oe.id, oe.name,date_trunc('day',oe.creation_date)as creation_date, " +
                " id_campaign, id_structure," +
                " CASE count(opts) " +
                "WHEN 0 THEN ROUND((oe.price + (oe.tax_amount * oe.price)/100), 2) * oe.amount "+
                "ELSE (price_all_options +(ROUND(oe.price + (oe.tax_amount * oe.price)/100, 2))) * oe.amount " +
                "END as price_total_equipment "+
                "FROM "+ Crre.crreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM((ROUND(price +(tax_amount * price)/100,2))) as price_all_options," +
                " id_order_client_equipment FROM "+ Crre.crreSchema + ".order_client_options " +
                "GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment WHERE id= ? " +
                " GROUP BY oe.id, price_all_options";

        sql.prepared(query,new fr.wseduc.webutils.collections.JsonArray().add(idOrder),SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deletableOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(oce.id) as count" +
                " FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                "WHERE oce.status != 'WAITING' AND oce.id = ? ;";

        JsonArray params = new JsonArray().add(idOrder);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }





    @Override
    public void deleteOrder(final Integer idOrder, JsonObject order,
                            final String idStructure, UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        Integer idCampaign = order.getInteger("id_campaign");
        String getCampaignPurseEnabledQuery = "SELECT purse_enabled FROM " + Crre.crreSchema + ".campaign WHERE id = ?";
        JsonArray params = new JsonArray().add(idCampaign);
        Sql.getInstance().prepared(getCampaignPurseEnabledQuery, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray results = event.right().getValue();
                boolean purseEnabled = (results.size() > 0 && results.getJsonObject(0).getBoolean("purse_enabled"));
                Double price = Double.valueOf(order.getString("price_total_equipment"));
                try {
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                    if (purseEnabled) {
                        statements.add(purseService.updatePurseAmountStatement(price, idCampaign, idStructure, "+"));
                    }
                    statements.add(getOptionsOrderDeletion(idOrder));
                    statements.add(getEquipmentOrderDeletion(idOrder));
                    if (purseEnabled) {
                        statements.add(getNewPurse(idCampaign, idStructure));
                    }
                    statements.add(getNewNbOrder(idCampaign, idStructure, user));

                    sql.transaction(statements, event1 -> {
                        JsonArray results1 = event1.body().getJsonArray("results");
                        JsonObject res = new JsonObject();
                        JsonObject newPurse = purseEnabled ? results1.getJsonObject(3) : new JsonObject();
                        JsonObject newOrderNumber = results1.getJsonObject(purseEnabled ? 4 : 2);
                        JsonArray newPurseArray = purseEnabled ? newPurse.getJsonArray("results").getJsonArray(0) : new JsonArray();
                        JsonArray newOrderNumberArray = newOrderNumber.getJsonArray("results").getJsonArray(0);
                        res.put("f1", newPurseArray.size() > 0
                                ? Double.parseDouble(newPurseArray.getString(0))
                                : 0);
                        res.put("f2", newOrderNumberArray.size() > 0
                                ? Double.parseDouble(newOrderNumberArray.getLong(0).toString())
                                : 0);

                        getTransactionHandler(event1, res, handler);

                    });
                } catch (ClassCastException e) {
                    LOGGER.error("An error occurred when casting order elements", e);
                    handler.handle(new Either.Left<>(""));
                }
            } else {
                handler.handle(new Either.Left<>("An error occurred when getting campaign"));
            }
        }));
    }

    @Override
    public  void windUpOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler){
        JsonObject statement = getUpdateStatusStatement(ids);
        sql.prepared(statement.getString("statement"),
                statement.getJsonArray("values"),
                SqlResult.validUniqueResultHandler(handler));
    }

    private JsonObject getOptionsOrderDeletion (Integer idOrder){
        String queryDeleteOptionsOrder = "DELETE FROM " + Crre.crreSchema + ".order_client_options"
                + " WHERE id_order_client_equipment = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idOrder);
        return new JsonObject()
                .put("statement", queryDeleteOptionsOrder)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void sendOrders(List<Integer> ids,final Handler<Either<String, JsonObject>> handler){

        this.listOrders(ids, event -> {
            if (event.isRight()) {
                JsonArray res = event.right().getValue();
                final JsonObject ordersObject = formatSendOrdersResult(res);
                structureService.getStructureById(ordersObject.getJsonArray("id_structures"),
                        structureArray -> {
                            if(structureArray.isRight()){
                                Either<String, JsonObject> either;
                                JsonObject returns = new JsonObject()
                                        .put("ordersCSF",
                                                getOrdersFormatedCSF(ordersObject.getJsonArray("order"),
                                                        structureArray.right().getValue()))
                                        .put("ordersBC",
                                                getOrdersFormatedBC(ordersObject.getJsonArray("order"),
                                                        structureArray.right().getValue()))
                                        .put("total",
                                                getTotalsOrdersPrices(ordersObject.getJsonArray("order")));
                                either = new Either.Right<>(returns);
                                handler.handle(either);
                            }
                        });
            } else {
                handler.handle(new Either.Left<>("An error occurred when collecting orders"));
            }
        });
    }

    @Override
    public void updateStatusToSent(final List<String> ids, String status, final String engagementNumber, final String labelProgram, final String dateCreation,
                                   final String orderNumber, final Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT distinct id_order " +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE order_client_equipment.number_validation IN " + Sql.listPrepared(ids.toArray());
        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray(ids), SqlResult.validResultHandler(updateOrCreateEvent -> {
            if (updateOrCreateEvent.isRight()) {
                JsonArray orderIds =  updateOrCreateEvent.right().getValue();
                JsonObject orderObject = orderIds.getJsonObject(0);
                if (null == orderObject.getInteger("id_order")) {
                    String nextValQuery = "SELECT nextval('" + Crre.crreSchema + ".order_id_seq') as id";
                    Sql.getInstance().raw(nextValQuery, SqlResult.validUniqueResultHandler(eventId -> {
                        if (eventId.isRight()) {
                            Number orderId = eventId.right().getValue().getInteger("id");
                            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                    .add(getOrderCreateStatement(orderId, engagementNumber, labelProgram, dateCreation, orderNumber))
                                    .add(getAddOrderClientRef(orderId, ids))
                                    .add(getUpdateClientOrderStatement(new fr.wseduc.webutils.collections.JsonArray(ids)));

                            Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                        } else {
                            handler.handle(new Either.Left<>(eventId.left().getValue()));
                        }
                    }));
                } else {
                    Number orderId = (orderIds.getJsonObject(0)).getInteger("id_order");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getUpdateOrderStatement(engagementNumber, labelProgram, dateCreation, orderNumber, orderId))
                            .add(getUpdateClientOrderStatement(new fr.wseduc.webutils.collections.JsonArray(ids)));

                    Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                }
            } else {
                handler.handle(new Either.Left<>(updateOrCreateEvent.left().getValue()));
            }
        }));
    }

    private JsonObject getAddOrderClientRef(Number orderId, List<String> validationNumbers) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                "SET id_order = ? " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.toArray());

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(orderId);
        for (String number : validationNumbers)  {
            params.add(number);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getUpdateOrderStatement (String engagementNumber, String labelProgram, String dateCreation, String orderNumber, Number orderId) {
        String query = "UPDATE " + Crre.crreSchema + ".order " +
                "SET engagement_number = ?, label_program = ?, date_creation = to_date(?, 'DD/MM/YYYY'), order_number = ? " +
                "WHERE id = ?;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(engagementNumber).add(labelProgram).add(dateCreation)
                .add(orderNumber).add(orderId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getOrderCreateStatement(Number id, String engagementNumber, String labelProgram, String dateCreation,
                                               String orderNumber) {

        String query = "INSERT INTO " + Crre.crreSchema + ".order(id, engagement_number, label_program, date_creation, order_number) " +
                "VALUES (?, ?, ?, to_date(?, 'DD/MM/YYYY'), ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(engagementNumber)
                .add(labelProgram)
                .add(dateCreation)
                .add(orderNumber);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
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

    private JsonObject getUpdateClientOrderStatement(JsonArray validationNumbers) {
        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET  status = SENT " +
                " WHERE number_validation in " + Sql.listPrepared(validationNumbers.getList()) +";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < validationNumbers.size(); i++) {
            params.add(validationNumbers.getString(i));
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonArray getOrdersFormatedCSF (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject orderOld;
        for (int i = 0 ; i< ordersArray.size(); i++){
            orderOld = ordersArray.getJsonObject(i);
            JsonObject order = orderOld
                    .put("structure",
                            (getStructureObject( structures, orderOld.getString("id_structure"))));
            order.put("hasOptions", orderOld.getJsonArray("options").size() != 0);
            orders.add(order);
        }

        return orders;
    }

    private JsonArray getOrdersFormatedBC (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new fr.wseduc.webutils.collections.JsonArray();
        boolean isIn;
        JsonObject orderOld;
        JsonObject orderNew;
        for (int i = 0 ; i< ordersArray.size(); i++){
            isIn = false;
            orderOld = ordersArray.getJsonObject(i);
            for(int j = 0; j<orders.size(); j++){
                orderNew = orders.getJsonObject(j);
                if(orderOld.getInteger("equipment_key").equals(orderNew.getInteger("equipment_key"))){
                    isIn = true;
                    JsonArray structure;
                    structure = orderNew.getJsonArray("structures");
                    structure.add(getStructureObject( structures,
                            orderOld.getString("id_structure"),
                            orderOld.getInteger("amount").toString(),
                            orderOld.getString("number_validation")));
                    orderNew.put("structures", structure);
                    int amount = (Integer.parseInt(orderOld.getInteger("amount").toString()) +
                            Integer.parseInt( orderNew.getInteger("amount").toString())) ;
                    orderNew.put("amount", Integer.toString(amount));
                }
            }
            if(! isIn) {
                JsonObject order = new JsonObject()
                        .put("price", orderOld.getString("price"))
                        .put("tax_amount", orderOld.getString("tax_amount"))
                        .put("amount", orderOld.getInteger("amount"))
                        .put("id_campaign", orderOld.getInteger("id_campaign").toString())
                        .put("name", orderOld.getString("name"))
                        .put("summary", orderOld.getString("summary"))
                        .put("description", orderOld.getString("description"))
                        .put("image", orderOld.getString("image"))
                        .put("technical_spec", orderOld.getString("technical_spec"))
                        .put("id_contract", orderOld.getInteger("id_contract").toString())
                        .put("equipment_key", orderOld.getInteger("equipment_key"))
                        .put("contract", new JsonObject( orderOld.getString("contract")))
                        .put("supplier",new JsonObject( orderOld.getString("supplier")) )
                        .put("campaign", new JsonObject( orderOld.getString("campaign")))
                        .put("options", orderOld.getJsonArray("options"))
                        .put("structures", new fr.wseduc.webutils.collections.JsonArray()
                                .add(getStructureObject( structures,
                                        orderOld.getString("id_structure"),
                                        orderOld.getInteger("amount").toString(),
                                        orderOld.getString("number_validation"))));
                orders.add(order);
            }
        }
        return orders;
    }

    private JsonObject getStructureObject(JsonArray structures, String structureId ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if((structures.getJsonObject(i)).getString("id").equals(structureId)){
                structure =  structures.getJsonObject(i);
            }
        }
        return structure;
    }

    private JsonObject getStructureObject(JsonArray structures, String structureId,
                                          String amount, String numberValidation ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if((structures.getJsonObject(i)).getString("id").equals(structureId)){
                structure = (structures.getJsonObject(i)).copy();
                structure.put("amount", amount)
                        .put("number_validation", numberValidation);
            }
        }
        return structure;
    }

    private JsonObject getTotalsOrdersPrices(JsonArray orders){

        double tva = 0;
        double total = 0;
        final int Const = 100;
        double totalTTC ;
        try {
            tva = Double.parseDouble((orders.getJsonObject(0)).getString("tax_amount"));
        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting tax amount", e);
        }
        for (int i = 0; i < orders.size(); i++) {
            try {
                total += Double.parseDouble((orders.getJsonObject(0)).getString("price")) *
                        Double.parseDouble((orders.getJsonObject(0)).getInstant("amount").toString());
            }catch (ClassCastException e) {
                LOGGER.error("An error occurred when casting order price", e);
            }
        }
        totalTTC = (total * tva)/Const + total;
        return new JsonObject()
                .put("totalPrice", total)
                .put("tva", tva)
                .put("totalTTC", totalTTC)
                ;
    }

    private JsonObject formatSendOrdersResult(JsonArray orders){
        JsonObject orderObject = new JsonObject();
        JsonArray structures = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray ordersList = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            structures.add(order.getString("id_structure"));
            order.put("options",
                    !order.getString("options").contains("null")
                            ? new fr.wseduc.webutils.collections.JsonArray(order.getString("options"))
                            : new fr.wseduc.webutils.collections.JsonArray());
            ordersList.add(order);
        }
        orderObject.put("order", ordersList)
                .put ("id_structures", structures);
        return orderObject;
    }

    private JsonObject getEquipmentOrderDeletion (Integer idOrder){
        String queryDeleteEquipmentOrder = "DELETE FROM " + Crre.crreSchema + ".order_client_equipment"
                + " WHERE id = ? ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idOrder);

        return new JsonObject()
                .put("statement", queryDeleteEquipmentOrder)
                .put("values", params)
                .put("action", "prepared");
    }

    private  JsonObject getNewPurse(Integer idCampaign, String idStructure){
        String query = "SELECT amount FROM " + Crre.crreSchema + ".purse " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);

        return  new JsonObject()
                .put("statement",query)
                .put("values",params)
                .put("action", "prepared");
    }
    private JsonObject getNewNbOrder(Integer idCampaign, String idStructure, UserInfos user) {

        String query = "SELECT count(id) FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? AND status != 'VALID' AND user_id = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure).add(user.getUserId());

        return  new JsonObject()
                .put("statement",query)
                .put("values",params)
                .put("action", "prepared");
    }

    @Override
    public void validateOrders(final HttpServerRequest request, final UserInfos user, final List<Integer> ids,
                               final String url, final Handler<Either<String, JsonObject>> handler){
        String getIdQuery = "Select "+ Crre.crreSchema + ".get_validation_number() as numberOrder ";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                try {
                    final String numberOrder = event.right().getValue().getString("numberorder");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getValidateStatusStatement(ids, numberOrder))
                            .add(getAgentInformation( ids));
                    sql.transaction(statements, jsonObjectMessage -> {


                        final JsonArray rows = ((jsonObjectMessage).body()
                                .getJsonArray("results").getJsonObject(1)).getJsonArray("results");
                        JsonArray names = new fr.wseduc.webutils.collections.JsonArray();
                        final int agentNameIndex = 2;
                        final int structureIdIndex = 4;
                        JsonArray structureIds = new fr.wseduc.webutils.collections.JsonArray();
                        for (int j = 0; j < rows.size(); j++) {


                            names.add((rows.getJsonArray(j)).getString(agentNameIndex));
                            structureIds.add((rows.getJsonArray(j)).getString(structureIdIndex));
                        }
                        final JsonArray agentNames = names;
                        emailSender.getPersonnelMailStructure(structureIds,
                                stringJsonArrayEither -> {


                                    final JsonObject result = new JsonObject()
                                            .put("number_validation", numberOrder)
                                            .put("agent", agentNames);
                                    handler.handle(new Either.Right<>(result));
                                    emailSender.sendMails(request, result,  rows,  user,  url,
                                            stringJsonArrayEither.right().getValue());
                                });
                    });
                } catch (ClassCastException e) {
                    LOGGER.error("An error occurred when casting numberOrder", e);
                    handler.handle(new Either.Left<>(""));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else {
                LOGGER.error("An error occurred when selecting number of the order");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }


    private static JsonObject getAgentInformation(List<Integer> ids){
        String query = "SELECT oce.id, contract.name, agent.name, agent.email, oce.id_structure , oce.id_campaign" +
                " FROM " + Crre.crreSchema + ".order_client_equipment oce " +
                " INNER JOIN " + Crre.crreSchema + ".contract ON contract.id = oce.id_contract " +
                " INNER JOIN " + Crre.crreSchema + ".agent ON contract.id_agent= agent.id " +
                " WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +"" +
                " ORDER BY id_structure,id_campaign ;  ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add( id);
        }
        return new JsonObject().put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getValidateStatusStatement(List<Integer> ids, String numberOrder){

        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET  status = VALID, number_validation = ?  " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(numberOrder);

        for (Integer id : ids) {
            params.add( id);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getUpdateStatusStatement(List<Integer> ids){

        String query = "UPDATE " + Crre.crreSchema + ".order_client_equipment " +
                " SET  status = DONE " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add( id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private static void getTransactionHandler(Message<JsonObject> event, JsonObject amountPurseNbOrder,
                                              Handler<Either<String, JsonObject>> handler) {
        JsonObject result = event.body();
        if (result.containsKey("status")&& "ok".equals(result.getString("status"))){
            JsonObject returns = new JsonObject();

            returns.put("amount", amountPurseNbOrder.getDouble("f1"));
            returns.put("nb_order",amountPurseNbOrder.getDouble("f2"));
            handler.handle(new Either.Right<>(returns));
        }  else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<>(""));
        }

    }

    @Override
    public void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT oce.id, oce.id_structure as idStructure, contract.name as namecontract," +
                " supplier.name as namesupplier, campaign.name as namecampaign, oce.amount as qty," +
                " oce.creation_date as date, CASE count(priceOptions)"+
                "WHEN 0 THEN ROUND ((oce.price+( oce.tax_amount*oce.price)/100)*oce.amount,2)"+
                "ELSE ROUND((priceOptions +( oce.price + ROUND((oce.tax_amount*oce.price)/100,2)))*oce.amount,2) "+
                "END as priceTotal "+
                "FROM "+ Crre.crreSchema +".order_client_equipment  oce "+
                "LEFT JOIN (SELECT ROUND (SUM(( price +( tax_amount*price)/100)*amount),2) as priceOptions, "+
                "id_order_client_equipment FROM "+ Crre.crreSchema +".order_client_options  GROUP BY id_order_client_equipment) opts "+
                "ON oce.id = opts.id_order_client_equipment "+
                "LEFT JOIN "+ Crre.crreSchema +".contract ON contract.id=oce.id_contract "+
                "INNER JOIN "+ Crre.crreSchema +".campaign ON campaign.id = oce.id_campaign "+
                "INNER JOIN "+ Crre.crreSchema +".supplier ON contract.id_supplier = supplier.id "+
                "WHERE oce.id in "+ Sql.listPrepared(idsOrders.toArray()) +
                " GROUP BY oce.id, idStructure, qty,date,oce.price, oce.tax_amount, oce.id_campaign, priceOptions," +
                " namecampaign, namecontract, namesupplier ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for(Integer id : idsOrders){
            params.add(id);
        }

        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }
    @Override
    public void updateRank( JsonArray orders, Handler<Either<String, JsonObject>> handler) {
        String query= "UPDATE " + Crre.crreSchema + ".order_client_equipment SET "+
                "rank = ? " +
                "WHERE id = ? RETURNING order_client_equipment.id  ;  " +
                "UPDATE " + Crre.crreSchema + ".order_client_equipment SET "+
                "rank = ? " +
                "WHERE id = ? RETURNING order_client_equipment.id ; ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for(Object object : orders){
            values.add(((JsonObject) object).getInteger("rank"));
            values.add(((JsonObject) object).getInteger("id"));
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(new JsonObject()
                .put("statement",query)
                .put("values",values)
                .put("action","prepared"));
        sql.transaction(statements, SqlResult.validRowsResultHandler(handler));
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
    public void getOneOrderClient(int idOrder, String status, Handler<Either<String, JsonObject>> handler){
        String query = "" +
                "SELECT oce.*, " +
                "       (  " +
                "               (SELECT " +
                "                  CASE " +
                "                  WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)  IS NULL THEN 0 " +
                "                  WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                  ELSE  SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "                  END " +
                "               FROM " + Crre.crreSchema +".order_client_options oco " +
                "               WHERE id_order_client_equipment = oce.id) + " +
                "                                                         (CASE  " +
                "                                                             WHEN oce.price_proposal IS NOT NULL THEN (oce.price_proposal)  " +
                "                                                             ELSE (oce.price + ((oce.price * oce.tax_amount) /100))  " +
                "                                                         END)) AS price_single_ttc,  " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(campaign.*) campaign " +
                "FROM  " + Crre.crreSchema + ".order_client_equipment oce " +
                "LEFT JOIN  " + Crre.crreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN  " + Crre.crreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "WHERE oce.status = '" + status + "' AND oce.id = ? " +
                "GROUP BY (oce.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          campaign.id)";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getOrderBCParams(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT DISTINCT engagement_number, label_program , order_number " +
                " FROM " + Crre.crreSchema + ".order od " +
                " INNER JOIN " + Crre.crreSchema + ".order_client_equipment oce on oce.id_order = od.id " +
                " WHERE oce.number_validation = ?";

        JsonArray params = new JsonArray().add(validationNumbers.getString(0));

        Sql.getInstance().prepared(query,params,SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void searchWithoutEquip(String query, UserInfos user, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.id_campaign = ? AND oe.status = 'WAITING' AND (bo.name ~* ? OR bo.name_user ~* ?) AND oe.id_structure IN (";

        values.add(id_campaign);
        values.add(query);
        values.add(query);
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery += " ORDER BY oce.id;";

        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void search(String query, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE oe.id_campaign = ? AND oe.status = 'WAITING' AND (bo.name ~* ? OR bo.name_user ~* ? OR oe.equipment_key IN (";

        values.add(id_campaign);
        if(query != "") {
            values.add(query);
            values.add(query);
        } else {
            values.add("bo.name");
            values.add("bo.name_user");
        }

        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getInteger("id"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += ") AND oe.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery += " ORDER BY oce.id;";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void searchName(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearchName(word, handler);
    }

/*    @Override
    public void filterGrade(List<String> filter, String query, Handler<Either<String, JsonArray>> handler) {
        if(query == "") {
            filter_waiting(filter, null, handler);
        } else {
            filter_waiting(filter, query, handler);
        }
    }*/
}

