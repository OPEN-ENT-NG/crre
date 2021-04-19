package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface OldOrderRegionService {


    void getOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void getAllOrderRegionByProject(int idProject, boolean filterRejectedOrders, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getOrdersRegionById(List<Integer> idsOrder, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getAllProjects(UserInfos user, String startDate, String endDate, Integer page, boolean filterRejectedSentOrders, String idStructure, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchName(String word, Handler<Either<String, JsonArray>> handler);

    void filter_only(UserInfos user, String startDate, String endDate, String idStructure, JsonArray filters,
                     Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearch(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                      Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);


    void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler);

    void updateOrders(List<Integer> ids, String status, String justification, Handler<Either<String, JsonObject>> handler);
}
