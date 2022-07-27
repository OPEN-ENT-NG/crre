package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.util.List;

public interface OrderRegionService {
    void createOrdersRegion(JsonObject order, UserInfos event, Number id_project, Handler<Either<String, JsonObject>> handler);

    void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler);

    void createProject (String title,  Handler<Either<String, JsonObject>> handler);

    void getAllIdsStatus(Handler<Either<String, JsonArray>> handler);

    void getAllOrderRegionByProject(int idProject, boolean filterRejectedOrders, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getOrdersRegionById(List<Integer> idsOrder, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getAllProjects(UserInfos user, String startDate, String endDate, Integer page, boolean filterRejectedSentOrders,
                        String idStructure, boolean oldTable, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                Integer page, Boolean old, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getLastProject(Handler<Either<String, JsonObject>> arrayResponseHandler);

    void insertOldOrders(JsonArray orderRegions, boolean isRenew, Handler<Either<String, JsonObject>> handlerJsonArray) throws ParseException;

    void insertOldClientOrders(JsonArray orderRegions, Handler<Either<String, JsonObject>> handler) throws ParseException;

    void updateOrders(List<Integer> ids, String status, String justification, Handler<Either<String, JsonObject>> handler);


    void updateOldOrders(JsonArray ordersRegion, Handler<Either<String, JsonObject>> handler);

    void deletedOrders(JsonArray ordersClient, String table, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getStatusByOrderId(Handler<Either<String, JsonArray>> arrayResponseHandler);

    void updateStatus(JsonArray listIdOrders, Handler<Either<String, JsonObject>> handlerJsonObject);

    void setIdOrderRegion(Handler<Either<String, JsonObject>> handlerJsonObject);

    void beautifyOrders(JsonArray structures, JsonArray orderRegion, JsonArray equipments, JsonArray ordersClient, JsonArray ordersRegion);

    String generateExport(JsonArray orderRegion);
}
