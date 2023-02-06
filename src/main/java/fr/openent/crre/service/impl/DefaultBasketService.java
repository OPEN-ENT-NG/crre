package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.BasketOrder;
import fr.openent.crre.model.BasketOrderItem;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.BasketService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultBasketService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketService.class);

    private final Integer PAGE_SIZE = 15;

    public DefaultBasketService() {
    }

    @Override
    public Future<List<BasketOrderItem>> listBasketOrderItem(Integer idCampaign, String idStructure, UserInfos user) {
        Promise<List<BasketOrderItem>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "SELECT id, amount, comment , processing_date, id_campaign, id_structure, id_item, reassort " +
                "FROM " + Crre.crreSchema + ".basket_order_item basket " +
                "WHERE basket.id_campaign = ? " +
                "AND basket.id_structure = ? " +
                "AND basket.owner_id = ? " +
                "GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure) " +
                "ORDER BY basket.id DESC;";
        values.add(idCampaign).add(idStructure).add(user.getUserId());

        String errorMessage = String.format("[CRRE@%s::listBasketOrderItem] Fail to retrieve basket order item list", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrderItem.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrder>> getMyBasketOrders(UserInfos user, Integer page, Integer idCampaign, String startDate, String endDate,
                                                       boolean oldTable) {
        Promise<List<BasketOrder>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "SELECT distinct b.* FROM " + Crre.crreSchema + ".basket_order b " +
                "INNER JOIN " + Crre.crreSchema + "." + (oldTable ? "order_client_equipment_old" :"order_client_equipment") + " oce on (oce.id_basket = b.id) " +
                "WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? " +
                "ORDER BY b.id DESC ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(idCampaign);
        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        String errorMessage = String.format("[CRRE@%s::getMyBasketOrders] Fail to get user basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonObject> create(final BasketOrderItem basketOrderItem, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        Future<Integer> nextValFuture = SqlHelper.getNextVal("basket_order_item_id_seq");
        nextValFuture
                .compose(nextVal -> {
                    List<TransactionElement> statements = Collections.singletonList(getBasketOrderItemCreationStatement(nextVal, basketOrderItem, user));
                    String errorMessage = String.format("[CRRE@%s::create] Fail to create basket order item", this.getClass().getSimpleName());
                    return TransactionHelper.executeTransaction(statements, errorMessage);
                })
                .onSuccess(res -> promise.complete(new JsonObject()
                        .put(Field.ID, nextValFuture.result())
                        .put(Field.IDCAMPAIGN, basketOrderItem.getIdCampaign())))
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<JsonObject> delete(Integer idBasket){
        Promise<JsonObject> promise = Promise.promise();

        List<TransactionElement> statements = Collections.singletonList(getBasketOrderItemDeletion(idBasket));
        String errorMessage = String.format("[CRRE@%s::delete] Fail to delete basket order item", this.getClass().getSimpleName());
        TransactionHelper.executeTransaction(statements, errorMessage)
                .onSuccess(res -> promise.complete(new JsonObject()
                        .put(Field.ID, idBasket)))
                        .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<JsonObject> updateAmount(UserInfos user, Integer idBasket, Integer amount) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "UPDATE " + Crre.crreSchema + ".basket_order_item " +
                " SET  amount = ? " +
                " WHERE id = ? AND owner_id = ?; ";
        values.add(amount).add(idBasket).add(user.getUserId());

        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonObject> updateComment(Integer idBasket, String comment) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String commentParameter = "".equals(comment.trim()) ? "null" : "?";
        String query = "UPDATE " + Crre.crreSchema + ".basket_order_item " +
                " SET comment = " + commentParameter +
                " WHERE id = ?; ";
        if (!"".equals(comment.trim())) {
            values.add(comment);
        }
        values.add(idBasket);

        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonObject> updateReassort(Integer idBasket, Boolean reassort) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String query = "UPDATE " + Crre.crreSchema + ".basket_order_item " +
                " SET reassort = ? WHERE id = ?; ";

        values.add(reassort);
        values.add(idBasket);

        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrderItem>> listBasketItemForOrder(Integer idCampaign, String idStructure, String idUser, List<Integer> basketIdList) {
        Promise<List<BasketOrderItem>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String basketFilter = basketIdList.size() > 0 ? "AND basket.id IN " + Sql.listPrepared(basketIdList) : "";
        String query = "SELECT basket.id ,basket.amount, " +
                "basket.comment, basket.processing_date, basket.id_campaign, basket.id_structure, basket.reassort, basket.id_item, " +
                "nextval('" + Crre.crreSchema + ".order_client_equipment_id_seq' ) as id_order " +
                "FROM " + Crre.crreSchema + ".basket_order_item basket " +
                "WHERE basket.id_campaign = ? AND basket.owner_id = ? " +
                "AND basket.id_structure = ? " + basketFilter +
                " GROUP BY (basket.id, basket.amount, basket.processing_date,basket.id_campaign, basket.id_structure);";
        values.add(idCampaign)
                .add(idUser)
                .add(idStructure);

        basketIdList.forEach(values::add);

        String errorMessage = String.format("[CRRE@%s::listBasketItemForOrder] Fail to list basket item for order", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrderItem.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<List<BasketOrder>> search(String query, UserInfos user, JsonArray equipTab, int idCampaign,
                       String startDate, String endDate, Integer page, Boolean old) {
        Promise<List<BasketOrder>> promise = Promise.promise();

        JsonArray values = new JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN " + Crre.crreSchema + (old ? ".order_client_equipment_old" : ".order_client_equipment") + " AS oe ON (bo.id = oe.id_basket) " +
                "WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ? ";
        values.add(startDate).add(endDate).add(user.getUserId()).add(idCampaign);

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
        String errorMessage = String.format("[CRRE@%s::search] Fail to search basket order ", this.getClass().getSimpleName());
        Sql.getInstance().prepared(sqlquery, values, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, BasketOrder.class), errorMessage));

        return promise.future();
    }

    static String SQLConditionQueryEquipments(String query, JsonArray equipTab, JsonArray values, Boolean old, String sqlQuery) {
        if(!old) {
            if (!query.equals("")) {
                sqlQuery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? ";
                values.add(query);
                values.add(query);
            }
            if(!equipTab.isEmpty()) {
                if (!query.equals("")) {
                    sqlQuery += "OR ";
                } else {
                    sqlQuery += "AND (";
                }
                sqlQuery += "oe.equipment_key IN (";
                for (int i = 0; i < equipTab.size(); i++) {
                    sqlQuery += "?,";
                    values.add(equipTab.getJsonObject(i).getString("ean"));
                }
                sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 1) + ")";
            }
        } else {
            sqlQuery += "AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_name ~* ?";
            values.add(query);
            values.add(query);
            values.add(query);
        }
        return sqlQuery;
    }

    @Override
    public Future<JsonObject> takeOrder(List<BasketOrderItem> basketOrderItemList, Integer idCampaign, UserInfos user,
                          String idStructure, String nameBasket) {
        Promise<JsonObject> promise = Promise.promise();

        int amount = basketOrderItemList.stream()
                .map(BasketOrderItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
        List<TransactionElement> statements = Collections.singletonList(getInsertBasketName(user, idStructure, idCampaign, nameBasket, 0, amount));

        String errorMessage = String.format("[CRRE@%s::takeOrder] Fail to insert basket name" , this.getClass().getSimpleName());
        TransactionHelper.executeTransaction(statements, errorMessage)
                .compose(transactionResult -> {
                    int idBasket = transactionResult.getJsonArray(0).getJsonObject(0).getInteger(Field.ID);

                    List<TransactionElement> otherStatements = basketOrderItemList.stream()
                            .map(basketOrderItem -> getInsertEquipmentOrderStatement(basketOrderItem, user.getUserId(), idBasket))
                            .collect(Collectors.toList());

                    List<Integer> basketOrderItemIdList = basketOrderItemList.stream()
                            .map(BasketOrderItem::getId)
                            .collect(Collectors.toList());
                    otherStatements.add(getDeletionBasketsOrderItemStatements(idCampaign, idStructure, user.getUserId(), basketOrderItemIdList, user));
                    String otherErrorMessage = String.format("[CRRE@%s::takeOrder] Fail to interact with basket item" , this.getClass().getSimpleName());

                    return TransactionHelper.executeTransaction(otherStatements, otherErrorMessage);
                })
                .onSuccess(transactionResult -> {
                    JsonObject basicBDObject = new JsonObject(transactionResult.getJsonArray(transactionResult.size()-1).getJsonObject(0).getString("row_to_json"));
                    promise.complete(getTransactionFuture(basicBDObject));
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private TransactionElement getInsertBasketName(UserInfos user, String idStructure, Integer idCampaign, String basketName, double total, int amount) {

        String insertBasketNameQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_order(" +
                        "name, id_structure, id_campaign, name_user, id_user, total, amount, created)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) returning id;";
        JsonArray params = new JsonArray()
                .add(basketName)
                .add(idStructure)
                .add(idCampaign)
                .add(user.getUsername())
                .add(user.getUserId())
                .add(total)
                .add(amount);


        return new TransactionElement(insertBasketNameQuery, params);
    }

    private TransactionElement getBasketOrderItemDeletion(Integer idBasket) {
        String insertBasketOrderItemRelationshipQuery =
                "DELETE FROM " + Crre.crreSchema + ".basket_order_item " +
                        "WHERE id= ? ;";

        JsonArray params = new JsonArray()
                .add(idBasket);

        return new TransactionElement(insertBasketOrderItemRelationshipQuery, params);
    }

    /**
     * Returns a basket order item insert statement
     *
     * @param id    basket Id
     * @param basketOrderItem basket order item Object
     * @return basket item relationship transaction statement
     */
    private TransactionElement getBasketOrderItemCreationStatement(Number id, BasketOrderItem basketOrderItem, UserInfos user) {


        String insertBasketOrderItemRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_order_item(" +
                        "id, amount, processing_date, id_item, id_campaign, id_structure, owner_id, owner_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        JsonArray params = new JsonArray()
                .add(id)
                .add(basketOrderItem.getAmount())
                .add(basketOrderItem.getProcessingDate())
                .add(basketOrderItem.getIdItem())
                .add(basketOrderItem.getIdCampaign())
                .add(basketOrderItem.getIdStructure())
                .add(user.getUserId())
                .add(user.getUsername());


        return new TransactionElement(insertBasketOrderItemRelationshipQuery, params);
    }


    /**
     * Basket to order
     * @param basket
     * @param userId
     * @param idBasket
     * @return
     */
    private TransactionElement getInsertEquipmentOrderStatement(BasketOrderItem basketOrderItem, String userId, int idBasket) {
        StringBuilder queryEquipmentOrder;
        JsonArray params;
        try {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, amount,  id_campaign, id_structure, status, " +
                            " equipment_key, comment, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
            params = getObjects(basketOrderItem);
            params.add(userId)
                    .add(idBasket)
                    .add(Boolean.TRUE.equals(basketOrderItem.getReassort()));

        } catch (NullPointerException e) {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, amount,  id_campaign, id_structure, status, " +
                            " equipment_key, comment, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ");
            params = getObjects(basketOrderItem);
            params.add(userId)
                    .add(idBasket)
                    .add(Boolean.TRUE.equals(basketOrderItem.getReassort()));
        }
        return new TransactionElement(queryEquipmentOrder.toString(), params);
    }

    private JsonArray getObjects(BasketOrderItem basketOrderItem) {
        JsonArray params;
        params = new JsonArray();

        params.add(basketOrderItem.getIdOrder())
                .add(basketOrderItem.getAmount())
                .add(basketOrderItem.getIdCampaign())
                .add(basketOrderItem.getIdStructure())
                .add("WAITING")
                .add(basketOrderItem.getIdItem())
                .add(basketOrderItem.getComment());
        return params;
    }

    private static TransactionElement getDeletionBasketsOrderItemStatements(Integer idCampaign, String idStructure, String idUser, List<Integer> basketOrderItemIdList,
                                                                            UserInfos user) {
        String basketFilter = basketOrderItemIdList.size() > 0 ? "AND basket_order_item.id IN " + Sql.listPrepared(basketOrderItemIdList) : "";

        JsonArray params = new JsonArray()
                .add(idCampaign).add(idStructure).add(idUser);
        basketOrderItemIdList.forEach(params::add);
        params.add(idStructure);
        params.add(idCampaign).add(idStructure);
        params.add(user.getUserId());
        String queryOrderItem = " DELETE FROM " + Crre.crreSchema + ".basket_order_item " +
                " WHERE id_campaign = ? AND id_structure = ? AND owner_id = ? " + basketFilter + " RETURNING " +
                getReturningQueryOfTakeOrder();
        return new TransactionElement(queryOrderItem, params);
    }

    private static String getReturningQueryOfTakeOrder() {
        return "( SELECT row_to_json(row(ROUND(p.amount::numeric,2)::double precision, count(o.id ) )) " +
                " FROM " + Crre.crreSchema + ".purse p, " + Crre.crreSchema + ".order_client_equipment o " +
                " where p.id_structure = ? " +
                " AND  o.id_campaign = ? " +
                " AND o.id_structure = ? AND o.status != 'VALID' AND o.user_id = ? " +
                " GROUP BY(p.amount) )";
    }

    private static JsonObject getTransactionFuture(JsonObject basicBDObject) {
        JsonObject returns = new JsonObject()
                .put("nb_order", basicBDObject.getInteger(basicBDObject.containsKey("f2") ? "f2" : "f1"));
        if (basicBDObject.containsKey("f2")) {
            try{
                returns.put("amount", basicBDObject.getDouble("f1"));
            }catch (Exception e){
                returns.put("amount",basicBDObject.getInteger("f1"));
            }
        }
        return returns;
    }
}
