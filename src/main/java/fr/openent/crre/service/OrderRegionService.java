package fr.openent.crre.service;

import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.model.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface OrderRegionService {
    TransactionElement getTransactionCreateOrdersRegion(JsonObject order, Number idProject);

    void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler);

    Future<List<Integer>> getAllIdsStatus();

    /**
     * Get orders by projects filtered
     *
     * @param idsProject          {@link List<Integer>} List of project ids
     * @param filters             {@link FilterModel} Filters containing status, searching text ...
     * @param itemSearchedIdsList {@link List<String>} List of item ids corresponding to searching item request
     * @param itemFilteredIdsList {@link List<String>}  List of item ids corresponding to filtering item request
     */
    Future<JsonArray> getAllOrderRegionByProject(List<Integer> idsProject, FilterModel filters, FilterItemModel filtersItem, List<String> itemSearchedIdsList, List<String> itemFilteredIdsList);


    Future<List<OrderRegionEquipmentModel>> getOrdersRegionById(List<Integer> orderRegionEquipmentIdList);

    Future<List<OrderRegionEquipmentModel>> getOrdersRegionByStatus(OrderStatus status);

    Future<List<OrderRegionComplex>> getOrdersRegionById(List<Integer> idsOrder, Boolean oldTable);

    void getOrdersRegionById(List<Integer> idsOrder, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler);

    /**
     * Get projects filtered
     *
     * @param filters {@link FilterModel} Filters (order) that contains date, status, searching text and more...
     * @param filtersItem {@link FilterItemModel} Filters (item) that editors, distributors and more...
     * @param itemSearchedIdsList {@link List<String>} List of item ids corresponding to searching item request
     * @param itemFilteredIdsList {@link List<String>}  List of item ids corresponding to filtering item request
     */
    Future<JsonArray> search(FilterModel filters, FilterItemModel filtersItem, List<String> itemSearchedIdsList, List<String> itemFilteredIdsList);

    List<TransactionElement> insertOldOrders(List<OrderRegionBeautifyModel> orderRegionBeautify, boolean isRenew);

    List<TransactionElement> insertOldOrders(JsonArray orderRegions, boolean isRenew);

    List<TransactionElement> insertOldClientOrders(List<OrderRegionBeautifyModel> orderRegionBeautifyList);

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

    List<OrderRegionBeautifyModel> orderResultToBeautifyModel(JsonArray structures, List<OrderRegionComplex> orderRegionComplexList, JsonArray equipments);

    JsonObject generateExport(List<OrderRegionBeautifyModel> logs);

    /**
     * Gets the list of all orders region in the same project as the order region pass in parameter
     *
     * @param projectIdList list of projectId
     */
    Future<Map<ProjectModel, List<OrderRegionEquipmentModel>>> getOrderRegionEquipmentInSameProject(List<Integer> projectIdList, boolean old);
}
