package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;
/**
 * Created by agnes.lapeyronnie on 20/02/2018.
 */
public interface OrderService {
    /**
     * List orders of a campaign and a structure in data base
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
    void listOrder(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);

    void listExport(Integer idCampaign, String idStructure,Handler<Either<String, JsonArray>> handler);
    /**
     * Get the list of all orders
     * @param status order status to retrieve
     * @param handler Function handler returning data
     */
    void listOrder(String status, Handler<Either<String, JsonArray>> handler);
    /**
     * Valid order ( change status to 'VALID', add validation number to the order,
     * then send mail to Agents )
     * @param request the request
     * @param user user informations
     * @param ids order's ids
     * @param url url to send in the mail
     * @param handler the Handler
     */
   void validateOrders(HttpServerRequest request, UserInfos user, List<Integer> ids, String url,
                       Handler<Either<String, JsonObject>> handler);
    /**
     * order to delete
     * @param idOrder id order
     * @param handler function handler returning idCampaign, price of Equipment
     */
    void orderForDelete(Integer idOrder, Handler<Either<String, JsonObject>> handler);

 /**
  * Check if the order can be delete
  *
  * @param idOrder
  * @param handler
  */
 void deletableOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler);

 /**
     * delete an order
     * @param idOrder id of the order item
     * @param order order to delete
     * @param idstructure id structure
     * @param handler function returning data
     */
    void deleteOrder( Integer idOrder, JsonObject order, String idstructure,
                      Handler<Either<String,JsonObject>> handler);

    /**
     * Wind up orders
     * @param ids List containing ids
     * @param handler Function handler returning data
     **/
    void windUpOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * get params for the exportCsvOrdersSelected
     * @param idsOrders list of idsOrders selected
     * @param handler function returning data
     */
    void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler);

    /**
     * Send orders
     * @param ids List containing ids
     * @param handler Function handler returning data
     */
    void sendOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Update status order
     * @param ids order id list
     * @param status status to update
     * @param engagementNumber engagement number
     * @param labelProgram Program label
     * @param dateCreation Creation date
     * @param orderNumber Order number
     * @param handler Function handler returning data
     */
    void updateStatusToSent(final List<String> ids, String status, final String engagementNumber, final String labelProgram, final String dateCreation,
                            final String orderNumber, final Handler<Either<String, JsonObject>> handler);

    /**
     * List orders based on ids
     * @param ids order ids
     * @param handler Function handler returning data
     */
    void listOrders(List<Integer> ids, Handler<Either<String, JsonArray>> handler);

    /**
     * Get structure ids based on provided order ids
     * @param ids order ids
     * @param handler Function handler returning data
     */
    void getStructuresId(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    /**
     * List an union of equipments and options based on order ids
     * @param ids order ids
     * @param structureId structure id
     * @param groupByStructure Group result by structure
     * @param handler function handler returning data
     * @param isNumberValidation Set true if it based on number validation
     */
    void getOrders(JsonArray ids, String structureId, Boolean isNumberValidation, Boolean groupByStructure, Handler<Either<String, JsonArray>> handler);

    void getOrderByValidatioNumber(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    /**
     * List all orders group by number validation and fitlered by status
     * @param status Status
     * @param handler Function handler returning data
     */
    void getOrdersGroupByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler);

    /**
     * Returns all orders indexed by validation number and filtered by status
     * @param status Status
     * @param handler Function handler returning data
     */
    void getOrdersDetailsIndexedByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler);

    void getOrdersForCSVExportByValidationNumbers(JsonArray validationNumbers, Handler<Either<String, JsonArray>> handler);

    /**
     * Cancel validation order. Set all orders to waiting status. All orders update are based on validation numbers;
     * @param validationNumbers validatio numbers list
     * @param handler Function handler returning data
     */
    void cancelValidation(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler);

    void getOrderFileId(String orderNumber, Handler<Either<String, JsonObject>> handler);

    void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> eitherHandler);

    /**
     * Get file from a specific order id
     *
     * @param orderId order identifier
     * @param fileId  file identifier
     * @param handler Function handler returning data
     */
    void getFile(Integer orderId, String fileId, Handler<Either<String, JsonObject>> handler);

    void updatePriceProposal(Integer id, Double price_proposal, Handler<Either<String, JsonObject>> eitherHandler);

    /**
     * Update the rank of two orders
     *
     * @param orders orders to update
     * @param handler Function handler returning data
     */
    void updateRank( JsonArray orders, Handler<Either<String, JsonObject>> handler);

    /**
     * Update the operations of  orders
     *
     * @param orders orders to update
     * @param handler Function handler returning data
     */
    void updateOperation( Integer id, JsonArray orders, Handler<Either<String, JsonObject>> handler);

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

    /**
     * Get an order by id
     *  @param idOrder id of the order to get
     * @param idOrders
     * @param handler Function handler returning data
     */
    void updateOperationInProgress(Integer idOrder, JsonArray idOrders, Handler<Either<String, JsonObject>> handler);

    void setInProgress(JsonArray ids, Handler<Either<String, JsonObject>> handler);

    void getOneOrderClient(int idOrderClient,String status,  Handler<Either<String, JsonObject>> handler);

    void getOrderBCParams(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler);
}
