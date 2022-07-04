package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.BasketService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
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

public class DefaultBasketService extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketService.class);

    private final Integer PAGE_SIZE = 15;

    public DefaultBasketService(String schema, String table) {
        super(schema, table);
    }

    public void listBasket(Integer idCampaign, String idStructure,  UserInfos user, Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT id, amount, comment , processing_date, id_campaign, id_structure, id_equipment, reassort " +
                "FROM " + Crre.crreSchema + ".basket_equipment basket " +
                "WHERE basket.id_campaign = ? " +
                "AND basket.id_structure = ? " +
                "AND basket.owner_id = ? " +
                "GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure) " +
                "ORDER BY basket.id DESC;";
        values.add(idCampaign).add(idStructure).add(user.getUserId());

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getMyBasketOrders(UserInfos user, Integer page, Integer id_campaign, String startDate, String endDate,
                                  boolean oldTable, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT distinct b.* FROM " + Crre.crreSchema + ".basket_order b " +
                "INNER JOIN " + Crre.crreSchema + "." + (oldTable ? "order_client_equipment_old" :"order_client_equipment") + " oce on (oce.id_basket = b.id) " +
                "WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? " +
                "ORDER BY b.id DESC ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(id_campaign);
        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void create(final JsonObject basket, UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Crre.crreSchema + ".basket_equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                try {
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getBasketEquipmentCreationStatement(id, basket, user));
                    sql.transaction(statements, event1 -> handler.handle(SqlQueryUtils.getTransactionHandler(event1, id)));
                } catch (ClassCastException e) {
                    LOGGER.error("An error occurred when casting tags ids", e);
                    handler.handle(new Either.Left<>(""));
                }
            } else {
                LOGGER.error("An error occurred when selecting next val");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    public void delete(final Integer idBasket,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentBasketDeletion(idBasket));
        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event, idBasket)));
    }

    public void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler ) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET  amount = ? " +
                " WHERE id = ?; ";
        values.add(amount).add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler) );
    }

    public void updateComment(Integer idBasket, String comment, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String commentParameter = "".equals(comment.trim()) ? "null" : "?";
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET comment = " + commentParameter +
                " WHERE id = ?; ";
        if (!"".equals(comment.trim())) {
            values.add(comment);
        }
        values.add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public void updateReassort(Integer idBasket, Boolean reassort, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET reassort = ? WHERE id = ?; ";

        values.add(reassort);
        values.add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public void listebasketItemForOrder(Integer idCampaign, String idStructure, String idUser, JsonArray baskets,
                                        Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String basketFilter = baskets.size() > 0 ? "AND basket.id IN " + Sql.listPrepared(baskets.getList()) : "";
        String query = "SELECT basket.id id_basket,basket.amount, " +
                "basket.comment, basket.processing_date, basket.id_campaign, basket.id_structure, basket.reassort, basket.id_equipment, " +
                "nextval('" + Crre.crreSchema + ".order_client_equipment_id_seq' ) as id_order, " +
                "campaign.purse_enabled " +
                "FROM  " + Crre.crreSchema + ".basket_equipment basket " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON (basket.id_campaign = campaign.id) " +
                "WHERE basket.id_campaign = ? AND basket.owner_id = ?" +
                "AND basket.id_structure = ? " + basketFilter +
                "GROUP BY (basket.id, basket.amount, basket.processing_date,basket.id_campaign, basket.id_structure, campaign.purse_enabled);";
        values.add(idCampaign)
                .add(idUser)
                .add(idStructure);

        if (baskets.size() > 0) {
            for (int i = 0; i < baskets.size(); i++) {
                values.add(baskets.getInteger(i));
            }
        }

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void search(String query, UserInfos user, JsonArray equipTab, int id_campaign,
                       String startDate, String endDate, Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oe ON (bo.id = oe.id_basket) " +
                "WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ? ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(id_campaign);

        sqlquery = SQLConditionQueryEquipments(query, equipTab, values, old, sqlquery);

        sqlquery += ") AND bo.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += " ORDER BY bo.id DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    static String SQLConditionQueryEquipments(String query, JsonArray equipTab, JsonArray values, Boolean old, String sqlquery) {
        if(!old) {
            if (!query.equals("")) {
                sqlquery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? ";
                values.add(query);
                values.add(query);
            }
            if(!equipTab.isEmpty()) {
                if (!query.equals("")) {
                    sqlquery += "OR ";
                } else {
                    sqlquery += "AND (";
                }
                sqlquery += "oe.equipment_key IN (";
                for (int i = 0; i < equipTab.size(); i++) {
                    sqlquery += "?,";
                    values.add(equipTab.getJsonObject(i).getString("ean"));
                }
                sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
            }
        } else {
            sqlquery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_name ~* ?";
            values.add(query);
            values.add(query);
            values.add(query);
        }
        return sqlquery;
    }

    public void takeOrder(final HttpServerRequest request, final JsonArray baskets, Integer idCampaign, UserInfos user,
                          String idStructure, final String nameStructure, JsonArray baskets_objects, String nameBasket,
                          final Handler<Either<String, JsonObject>> handler) {
        try {
            JsonArray test = new fr.wseduc.webutils.collections.JsonArray();
            JsonObject basket_data;
            int amount = 0;
            double total = 0;
            for (int i = 0; i < baskets.size(); i++) {
                basket_data = baskets.getJsonObject(i);
                amount += basket_data.getInteger("amount") ;
            }
            test.add(getInsertBasketName(user, idStructure, idCampaign, nameBasket, total, amount));

            sql.transaction(test, event_id -> {
                JsonObject results_id = event_id.body().getJsonArray("results")
                        .getJsonObject(event_id.body().getJsonArray("results").size()-1);
                JsonArray objectResultId = results_id.getJsonArray("results").getJsonArray(0);
                int id_basket = objectResultId.getInteger(0);

                JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                JsonObject basket;
                Boolean purse_enabled = baskets.getJsonObject(0).getBoolean("purse_enabled");
                for (int i = 0; i < baskets.size(); i++) {
                    basket = baskets.getJsonObject(i);
                    statements.add(getInsertEquipmentOrderStatement(basket, user.getUserId(), id_basket));
                }
                statements.add(getDeletionBasketsEquipmentStatments(idCampaign, idStructure, user.getUserId(), baskets_objects, purse_enabled, user));
                sql.transaction(statements, event -> {
                    JsonObject results = event.body().getJsonArray("results")
                            .getJsonObject(event.body().getJsonArray("results").size()-1);
                    JsonArray objectResult = results.getJsonArray("results").getJsonArray(0);
                    String jsonValue = objectResult.getString(0) == null ? "{}" : objectResult.getString(0);
                    getTransactionHandler(event, new JsonObject(jsonValue), handler);
                });
            });


        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting baskets elements", e);
            handler.handle(new Either.Left<>(""));
        }
    }

    private JsonObject getInsertBasketName(UserInfos user, String idStructure, Integer idCampaign, String basket_name, double total, int amount) {

        String insertBasketNameQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_order(" +
                        "name, id_structure, id_campaign, name_user, id_user, total, amount, created)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) returning id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(basket_name)
                .add(idStructure)
                .add(idCampaign)
                .add(user.getUsername())
                .add(user.getUserId())
                .add(total)
                .add(amount);


        return new JsonObject()
                .put("statement", insertBasketNameQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getEquipmentBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Crre.crreSchema + ".basket_equipment " +
                        "WHERE id= ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idBasket);

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Returns a basket equipment insert statement
     *
     * @param id    basket Id
     * @param basket basket Object
     * @return basket equipment relationship transaction statement
     */
    private JsonObject getBasketEquipmentCreationStatement(Number id, JsonObject basket, UserInfos user) {


        String insertBasketEquipmentRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_equipment(" +
                        "id, amount, processing_date, id_equipment, id_campaign, id_structure, owner_id, owner_name)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(basket.getInteger("amount"))
                .add(basket.getString("processing_date"))
                .add(basket.getString("equipment"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add(user.getUserId())
                .add(user.getUsername());


        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }


    /**
     * Basket to order
     * @param basket
     * @param userId
     * @param id_basket
     * @return
     */
    private JsonObject getInsertEquipmentOrderStatement(JsonObject basket, String userId, int id_basket) {
        StringBuilder queryEquipmentOrder;
        JsonArray params;
        try {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, amount,  id_campaign, id_structure, status, " +
                            " equipment_key, comment, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
            params = getObjects(basket);
            params.add(userId)
                    .add(id_basket)
                    .add(basket.getBoolean("reassort"));

        } catch (java.lang.NullPointerException e) {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, amount,  id_campaign, id_structure, status, " +
                            " equipment_key, comment, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
            params = getObjects(basket);
            params.add(userId)
                    .add(id_basket)
                    .add(basket.getBoolean("reassort"));
        }
        return new JsonObject()
                .put("statement", queryEquipmentOrder.toString())
                .put("values", params)
                .put("action", "prepared");

    }

    private JsonArray getObjects(JsonObject basket) {
        JsonArray params;
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(basket.getInteger("id_order"))
                .add(basket.getInteger("amount"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add("WAITING")
                .add(basket.getString("id_equipment"))
                .add(basket.getString("comment"));
        return params;
    }

    private static JsonObject getDeletionBasketsEquipmentStatments(Integer idCampaign, String idStructure, String idUser, JsonArray baskets,
                                                                   Boolean purse_enabled, UserInfos user) {
        String basketFilter = baskets.size() > 0 ? "AND basket_equipment.id IN " + Sql.listPrepared(baskets.getList()) : "";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure).add(idUser);
        for (int i = 0; i < baskets.size(); i++) {
            params.add(baskets.getInteger(i));
        }
        if (purse_enabled) {
            params.add(idStructure);
        }
        params.add(idCampaign).add(idStructure);
        params.add(user.getUserId());
        String queryEquipmentOrder = " DELETE FROM " + Crre.crreSchema + ".basket_equipment " +
                " WHERE id_campaign = ? AND id_structure = ? AND owner_id = ? " + basketFilter + " RETURNING " +
                getReturningQueryOfTakeOrder(purse_enabled);
        return new JsonObject()
                .put("statement", queryEquipmentOrder)
                .put("values", params )
                .put("action", "prepared");
    }

    private static String getReturningQueryOfTakeOrder(Boolean purse_enabled) {
        if (purse_enabled) {
            return "( SELECT row_to_json(row(p.amount, count(o.id ) )) " +
                    " FROM " + Crre.crreSchema + ".purse p, " + Crre.crreSchema + ".order_client_equipment o " +
                    " where p.id_structure = ? " +
                    " AND  o.id_campaign = ? " +
                    " AND o.id_structure = ? AND o.status != 'VALID' AND o.user_id = ? " +
                    " GROUP BY(p.amount) )";
        } else {
            return "(SELECT row_to_json(row(count(o.id)))\n" +
                    "FROM " + Crre.crreSchema + ".order_client_equipment o " +
                    "WHERE  o.id_campaign = ? " +
                    "  AND o.id_structure = ? " +
                    "  AND o.status != 'VALID'" +
                    "  AND o.user_id = ?)";
        }
    }
    /**
     * Returns the amount of purse from an order transactions.
     *
     * @param event PostgreSQL event
     * @param basicBDObject
     * @return Transaction handler
     */
    private static void getTransactionHandler(Message<JsonObject> event, JsonObject basicBDObject,
                                              Handler<Either<String, JsonObject>> handler) {
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("nb_order", basicBDObject.getInteger(basicBDObject.containsKey("f2") ? "f2" : "f1"));
            if (basicBDObject.containsKey("f2")) {
                try{
                    returns.put("amount", basicBDObject.getDouble("f1"));
                }catch (Exception e){
                    returns.put("amount",basicBDObject.getInteger("f1"));
                }
            }
            handler.handle(new Either.Right<>(returns));
        } else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<>(""));
        }
    }
}
