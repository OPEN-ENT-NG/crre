package fr.openent.crre.service;

import fr.openent.crre.model.OrderLDEModel;
import fr.openent.crre.model.OrderRegionEquipmentModel;
import fr.openent.crre.model.ProjectModel;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;

public interface OrderRegionService {
    TransactionElement getTransactionCreateOrdersRegion(JsonObject order, Number idProject);

    void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler);

    /**
     * @deprecated Use {@link #createProject(String)}
     */
    @Deprecated
    void createProject(String title,  Handler<Either<String, JsonObject>> handler);

    Future<ProjectModel> createProject(String title);

    void getAllIdsStatus(Handler<Either<String, JsonArray>> handler);

    void getAllOrderRegionByProject(int idProject, boolean filterRejectedOrders, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler);

    Future<List<OrderRegionEquipmentModel>> getOrdersRegionById(List<Integer> orderRegionEquipmentIdList);

    Future<JsonObject> getNewOrdersCount();

    void getOrdersRegionById(List<Integer> idsOrder, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getAllProjects(UserInfos user, String startDate, String endDate, Integer page, boolean filterRejectedSentOrders,
                        String idStructure, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler);

    /**
     * @deprecated Use {@link #getLastProject()}
     */
    @Deprecated
    void getLastProject(Handler<Either<String, JsonObject>> arrayResponseHandler);

    Future<JsonObject> getLastProject();

    List<TransactionElement> insertOldOrders(JsonArray orderRegions, boolean isRenew);

    List<TransactionElement> insertOldClientOrders(JsonArray orderRegions);

    Future<JsonObject> updateOrdersStatus(List<Integer> ids, String status, String justification);

    void updateOldOrders(JsonArray ordersRegion, Handler<Either<String, JsonObject>> handler);


    /**
     * Update LDE order status using transaction to avoid deadlock type errors
     *
     * @param ordersRegion order region list
     * @return Future
     */
    Future<JsonObject> updateOldOrdersWithTransaction(JsonArray ordersRegion);

    Future<JsonObject> updateOldOrderLDEModel(List<OrderLDEModel> listOrder);

    List<TransactionElement> deletedOrders(List<Long> ordersClientIdList, String table);

    void getStatusByOrderId(Handler<Either<String, JsonArray>> arrayResponseHandler);

    void updateStatus(JsonArray listIdOrders, Handler<Either<String, JsonObject>> handlerJsonObject);

    void setIdOrderRegion(Handler<Either<String, JsonObject>> handlerJsonObject);

    void beautifyOrders(JsonArray structures, JsonArray orderRegion, JsonArray equipments, List<Long> ordersClient);

    JsonObject generateExport(JsonArray orderRegion);

    /**
     * Gets the list of all orders region in the same project as the order region pass in parameter
     * @param projectIdList list of projectId
     */
    Future<Map<ProjectModel, List<OrderRegionEquipmentModel>>> getOrderRegionEquipmentInSameProject(List<Integer> projectIdList, boolean old);
}
