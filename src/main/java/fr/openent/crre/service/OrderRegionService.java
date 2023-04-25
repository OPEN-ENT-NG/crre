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

    /**
     * Get projects filtered
     *
     * @param filters {@link FilterModel} Filters (order) that contains date, status, searching text and more...
     * @param filtersItem {@link FilterItemModel} Filters (item) that editors, distributors and more...
     * @param itemSearchedIdsList {@link List<String>} List of item ids corresponding to searching item request
     * @param itemFilteredIdsList {@link List<String>}  List of item ids corresponding to filtering item request
     */
    Future<JsonArray> search(FilterModel filters, FilterItemModel filtersItem, List<String> itemSearchedIdsList, List<String> itemFilteredIdsList);

    List<TransactionElement> insertOldRegionOrders(List<OrderUniversalModel> orderRegionBeautify, boolean isRenew);

    List<TransactionElement> insertOldClientOrders(List<OrderUniversalModel> orderRegionBeautifyList);

    // TODO: verif si a delete isRenew
    /**
     * @deprecated Use {@link #insertOldRegionOrders(List, boolean)}
     */
    @Deprecated
    List<TransactionElement> insertOldRegionOrders(JsonArray orderRegions, boolean isRenew);

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

    void updateStatus(JsonArray listIdOrders, Handler<Either<String, JsonObject>> handlerJsonObject);

    void setIdOrderRegion(Handler<Either<String, JsonObject>> handlerJsonObject);

    JsonObject generateExport(Map<OrderUniversalModel, StructureNeo4jModel> orderStructureMap);

    /**
     * Gets the list of all orders region in the same project as the order region pass in parameter
     *
     * @param projectIdList list of projectId
     */
    Future<Map<ProjectModel, List<OrderRegionEquipmentModel>>> getOrderRegionEquipmentInSameProject(List<Integer> projectIdList, boolean old);
}
