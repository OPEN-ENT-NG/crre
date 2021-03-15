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
     * @param handler function handler returning data
     */
    void listOrder(Integer idCampaign, String idStructure, UserInfos user, List<String> ordersId,
                   Handler<Either<String, JsonArray>> handler);

    void listExport(List<Integer> idsOrders, Handler<Either<String, JsonArray>> catalog);
    /**
     * Get the list of all orders
     * @param status order status to retrieve
     * @param handler Function handler returning data
     */
    void listOrder(String status, Integer page, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Get the list of all users
     * @param status order status to retrieve
     * @param handler Function handler returning data
     */
    void listUsers(String status, Handler<Either<String, JsonArray>> handler);

    void rejectOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Valid order
     * @param ids order's ids
     * @param handler the Handler
     */
   void validateOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * get params for the exportCsvOrdersSelected
     * @param idsOrders list of idsOrders selected
     * @param handler function returning data
     */
    void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler);

    void updateAmount(Integer id, Integer amount, Handler<Either<String, JsonObject>> handler);

    void updateReassort(Integer id, Boolean reassort, Handler<Either<String, JsonObject>> handler);

    void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> eitherHandler);

    /**
     * Get file from a specific order id
     *
     * @param orderId order identifier
     * @param fileId  file identifier
     * @param handler Function handler returning data
     */
    void getFile(Integer orderId, String fileId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update the status orders
     *
     * @param idOrder orders to update
     * @param status status to update
     * @param handler Function handler returning data
     */
    void updateStatusOrder( Integer idOrder, JsonObject status, Handler<Either<String, JsonObject>> handler);

    /**
     * Get an order by id
     *
     * @param idOrder id of the order to get
     * @param handler Function handler returning data
     */
    void getOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler);

    void setInProgress(JsonArray ids, Handler<Either<String, JsonObject>> handler);

    void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchWithoutEquip(String query, JsonArray filters, UserInfos user, Integer id_campaign, Integer page,
                            Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchName(String word, Handler<Either<String, JsonArray>> handler);

    void searchWithAll(String query, JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filter(JsonArray filters, UserInfos user, JsonArray equipTab, Integer id_campaign, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterGrade(List<String> filter, String query, Handler<Either<String, JsonArray>> handler);

}
