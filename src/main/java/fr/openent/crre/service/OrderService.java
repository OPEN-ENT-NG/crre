package fr.openent.crre.service;

import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.model.FilterModel;
import fr.openent.crre.model.OrderClientEquipmentModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * List orders of a campaign and a structure in database
     * @param filterModel filterModel
     * @return a future of result
     */
    Future<List<OrderUniversalModel>> listOrder(FilterModel filterModel);

    /**
     * Get the list of all orders
     * @param status order status to retrieve
     * @param idStructure
     * @param startDate
     * @param endDate
     * @param handler Function handler returning data
     */
    void listOrder(String status, String idStructure, Integer page, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    /**
     * Get the list of all users
     * @param status order status to retrieve
     * @param idStructure
     * @param handler Function handler returning data
     */
    void listUsers(String status, String idStructure, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void updateAmount(Integer id, Integer amount, Handler<Either<String, JsonObject>> handler);

    void updateReassort(Integer id, Boolean reassort, Handler<Either<String, JsonObject>> handler);

    void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> eitherHandler);

    Future<List<OrderClientEquipmentModel>> updateStatus(List<Integer> orderClientEquipmentIdList, OrderStatus orderStatus);

    Future<JsonArray> search(String query, Map<String, List<String>> filters, String idStructure, List<String> equipementIdList,
                             Integer idCampaign, String startDate, String endDate, Integer page);

    /**
     * Get customer orders from a list of ids
     *
     * @param orderClientEquipmentIdList list of order client id
     */
    Future<List<OrderClientEquipmentModel>> getOrderClientEquipmentList(List<Integer> orderClientEquipmentIdList);

    /**
     * Gets all customer orders that belong to the basket list provided as a parameter
     *
     * @param basketIdList list of basket id
     */
    Future<List<OrderClientEquipmentModel>> getOrderClientEquipmentListFromBasketId(List<Integer> basketIdList);
}
