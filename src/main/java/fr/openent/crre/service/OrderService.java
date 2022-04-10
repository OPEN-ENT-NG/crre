package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface OrderService {
    /**
     * List orders of a campaign and a structure in data base
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param user user who is connected
     * @param startDate
     * @param endDate
     * @param handler function handler returning data
     */
    void listOrder(Integer idCampaign, String idStructure, UserInfos user, List<String> ordersId,
                   String startDate, String endDate, boolean oldTable,  Handler<Either<String, JsonArray>> handler);

    void listExport(List<Integer> idsOrders, UserInfos user, String idCampaign, String statut, String startDate, String endDate, boolean oldTable, Handler<Either<String, JsonArray>> catalog);

    /**
     * Get the list of all orders
     * @param status order status to retrieve
     * @param startDate
     * @param endDate
     * @param handler Function handler returning data
     */
    void listOrder(String status, Integer page, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    void listOrderAmount(String status, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonObject>> handler);

    void listOrderCredit(String status, UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    /**
     * Get the list of all users
     * @param status order status to retrieve
     * @param handler Function handler returning data
     */
    void listUsers(String status, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void rejectOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Valid order
     * @param ids order's ids
     * @param handler the Handler
     */
   void validateOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    void updateAmount(Integer id, Integer amount, Handler<Either<String, JsonObject>> handler);

    void updateReassort(Integer id, Boolean reassort, Handler<Either<String, JsonObject>> handler);

    void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> eitherHandler);

    void setInProgress(JsonArray ids, Handler<Either<String, JsonObject>> handler);

    void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, String startDate, String endDate, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);
}
