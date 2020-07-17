package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class SqlUtils {

    private SqlUtils() {
        throw new IllegalAccessError("Utility class");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderService.class);

    public static void deleteIds(String table, List<Integer> ids,
                                 io.vertx.core.Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.lystoreSchema + "." + table + " WHERE ")
                .append(SqlQueryUtils.prepareMultipleIds(ids));
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }

    /**
     * Returns a total price to orders by a id_operation with option order and select The Good Place,Eh! sorry good price ;)
     *
     * @param idOperation Number
     * @param handler     Function handler returning json object ex: '{"id":46,"amount":"9212.26"}'
     */
    public static void getSumPriceOperation(Number idOperation, Handler<Either<String, JsonObject>> handler) {
        try {
            Future<JsonObject> getTotalOrderClientFuture = Future.future();
            Future<JsonObject> getTotalOrderRegionFuture = Future.future();

            CompositeFuture.all(getTotalOrderClientFuture, getTotalOrderRegionFuture).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) {
                    String message = "Failed to retrieve operation";
                    handler.handle(new Either.Left<>(message));
                    return;
                }
                JsonObject result = new JsonObject();
                Double totalPriceClient, totalPriceRegion;
                String resultTotalOperation;


                JsonObject getTotalClient = getTotalOrderClientFuture.result();
                JsonObject getTotalRegion = getTotalOrderRegionFuture.result();

                totalPriceClient = Double.parseDouble(getTotalClient.getString("price_total_orders_clients") != null? getTotalClient.getString("price_total_orders_clients"):"0.0");
                totalPriceRegion = Double.parseDouble(getTotalRegion.getString("price_total_orders_regions") != null? getTotalRegion.getString("price_total_orders_regions"):"0.0");

                resultTotalOperation = String.valueOf(totalPriceClient + totalPriceRegion);

                handler.handle(new Either.Right<>(result.put("id", idOperation).put("amount", resultTotalOperation)));
            });

            getTotalOrderClient(idOperation, FutureHelper.handlerJsonObject(getTotalOrderClientFuture));
            getTotalOrderRegion(idOperation, FutureHelper.handlerJsonObject(getTotalOrderRegionFuture));

        } catch (Exception e) {
            LOGGER.error("Error in SqlUtils ->", e);
        }
    }

    private static void getTotalOrderClient(Number idOperation, Handler<Either<String, JsonObject>> handler){
        String status = "IN PROGRESS";
        String queryGetTotalOperationClient = " " +
                "SELECT (ROUND(SUM((  " +
                "                     (SELECT CASE  " +
                "                   WHEN oce.override_region IS true THEN 0  " +
                "                                 WHEN oce.price_proposal IS NOT NULL THEN 0  " +
                "                                 WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0  " +
                "                                 ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)  " +
                "                             END  " +
                "                      FROM   " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "                      WHERE id_order_client_equipment = oce.id) + (CASE  " +
                "                                         WHEN oce.override_region IS true THEN 0  " +
                "                                                                       WHEN oce.price_proposal IS NOT NULL THEN (oce.price_proposal)  " +
                "                                                                       ELSE (ROUND(oce.price + ((oce.price * oce.tax_amount) /100), 2))  " +
                "                                                                   END)) * oce.amount), 2)) AS price_total_orders_clients  " +
                "FROM   " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "WHERE oce.id_operation = ? " +
                "AND oce.status = '" + status + "' " +
                "  ";

        Sql.getInstance().prepared(queryGetTotalOperationClient, new JsonArray().add(idOperation), SqlResult.validUniqueResultHandler(handler));
    }

    private static void getTotalOrderRegion(Number idOperation, Handler<Either<String, JsonObject>> handler){
        String status = "IN PROGRESS";
        String queryGetTotalOperationRegion = " " +
                "SELECT (ROUND(SUM(ore.price * ore.amount), 2)) AS price_total_orders_regions " +
                "FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore " +
                "WHERE ore.id_operation = ? " +
                "AND ore.status = '" + status + "' ;";

        Sql.getInstance().prepared(queryGetTotalOperationRegion, new JsonArray().add(idOperation), SqlResult.validUniqueResultHandler(handler));
    }

    /**
     * Use function getSumPriceOperation() for count total to price to every operations
     *
     * @param idsOperations JsonArray
     * @param handler       Function handler returning json array ex [{"id":45,"amount":"41805.08"},{"id":46,"amount":"9212.26"},{"id":47,"amount":"1073.62"}]
     */
    public static void getAllPriceOperation(JsonArray idsOperations, Handler<Either<String, JsonArray>> handler) {
        try {
            List<Future> futuresArray = new ArrayList<>();
            JsonArray result = new JsonArray();

            for (int i = 0; i < idsOperations.size(); i++) {
                Future future = Future.future();
                Number id = idsOperations.getInteger(i);
                SqlUtils.getSumPriceOperation(id, FutureHelper.handlerJsonObject(future));
                futuresArray.add(future);
            }

            CompositeFuture.join(futuresArray).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) {
                    String message = "Failed to retrieve operation";
                    handler.handle(new Either.Left<>(message));
                    return;
                }
                for (int i = 0; i < futuresArray.size(); i++) {
                    JsonObject resultFuture = (JsonObject) futuresArray.get(i).result();
                    result.add(resultFuture);
                }
                handler.handle(new Either.Right<>(result));
            });
        } catch (Exception e) {
            LOGGER.error("Error in SqlUtils ->", e);
        }
    }

    /**
     * Returns an array to object with a count to order affect to operation without order edit by region and with new region order
     *
     * @param idsOperations JsonArray
     * @param handler       Function handler json array ex [{"id":45,"nb_orders":"5"},{"id":46,"nb_orders":"1"},{"id":47,"nb_orders":"3"}]
     */
    public static void getCountOrderInOperation(JsonArray idsOperations, Handler<Either<String, JsonArray>> handler) {
        try {
            JsonArray idsOperationsMergeTwoArray = SqlQueryUtils.multiplyArray(2, idsOperations);
            String status = "IN PROGRESS";
            String queryGetTotalOperation = "" +
                    "SELECT id,  " +
                    "       SUM(nb_orders) AS nb_orders  " +
                    "FROM  " +
                    "  (SELECT oce.id_operation AS id,  " +
                    "   oce.id AS c,  " +
                    "          count(*) AS nb_orders  " +
                    "   FROM    " + Lystore.lystoreSchema + ".order_client_equipment AS oce  " +
                    "   WHERE oce.id_operation IN " +
                    Sql.listPrepared(idsOperations.getList()) + " " +
                    "     AND oce.status = '" + status + "'  " +
                    "     AND oce.override_region = FALSE  " +
                    "   GROUP BY (oce.id_operation, c)  " +
                    "   UNION  " +
                    "   SELECT ore.id_operation AS id,  " +
                    "   ore.id AS r,  " +
                    "                COUNT (*) AS nb_orders  " +
                    "   FROM    " + Lystore.lystoreSchema + ".\"order-region-equipment\" AS ore  " +
                    "   WHERE ore.id_operation IN " +
                    Sql.listPrepared(idsOperations.getList()) + " " +
                    "   GROUP BY (ore.id_operation, r)) AS OPERATION  " +
                    "GROUP BY (operation.id)";

            Sql.getInstance().prepared(queryGetTotalOperation, idsOperationsMergeTwoArray, SqlResult.validResultHandler(handler));
        } catch (Exception e) {
            LOGGER.error("Error in SqlUtils ->", e);
        }
    }

    /**
     * Use function getAllPriceOperation() for count total to price to every instructions group by operations
     *
     * @param idsInstructions JsonArray
     * @param handler Function handler returning json array ex [{"id":120,"amount":"10 285,88"},{"id":121,"amount":"41 805,08"}]
     */
    public static void getSumOperations(JsonArray idsInstructions, Handler<Either<String, JsonArray>> handler) {
        try {
            String queryGetOperationsAndInstructionIds = "SELECT  " +
                    "id AS id_operation, " +
                    "id_instruction " +
                    "FROM  " + Lystore.lystoreSchema + ".operation " +
                    "WHERE id_instruction in " +
                    Sql.listPrepared(idsInstructions.getList());
            Sql.getInstance().prepared(queryGetOperationsAndInstructionIds, idsInstructions, SqlResult.validResultHandler(event -> {
                JsonArray result = new JsonArray();
                JsonArray resultRequest = event.right().getValue();

                JsonArray idsOperationGetByLoop = new JsonArray();
                for(int i = 0 ; i<resultRequest.size() ; i++){
                    JsonObject rowRequest = resultRequest.getJsonObject(i);
                    idsOperationGetByLoop.add(rowRequest.getInteger("id_operation"));
                }
                getAllPriceOperation(idsOperationGetByLoop, eventFinal -> {
                    JsonArray resultAmount = eventFinal.right().getValue();

                    for(int i = 0 ; i<resultRequest.size() ; i++){
                        Integer idToRequest = resultRequest.getJsonObject(i).getInteger("id_operation");
                        for(int j = 0 ; j<resultAmount.size() ; j++){
                            if(idToRequest.equals(resultAmount.getJsonObject(j).getInteger("id"))){
                                resultRequest.getJsonObject(i).put("amount", resultAmount.getJsonObject(j).getString("amount"));
                            }
                        }
                    }
                    for (int i = 0 ; i<idsInstructions.size() ; i++){
                        JsonObject correspondenceIds  = new JsonObject();
                        Double sumPriceOperations = 0d;
                        for( int j = 0 ; j<resultRequest.size() ; j++){
                            Double amount = Double.parseDouble( resultRequest.getJsonObject(j).getString("amount"));
                            Integer idInstruction = resultRequest.getJsonObject(j).getInteger("id_instruction");
                            if(idInstruction.equals(idsInstructions.getInteger(i))) sumPriceOperations += amount;
                        }
                        correspondenceIds.put("id", idsInstructions.getInteger(i))
                                .put("amount",  Double.toString(sumPriceOperations));
                        result.add(correspondenceIds);
                    }
                    handler.handle(new Either.Right<>(result));
                });
            }));
        } catch (Exception e) {
            LOGGER.error("Error in SqlUtils ->", e);
        }
    }
}
