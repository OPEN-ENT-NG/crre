package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface OrderRegionService {
    void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void updateOrderRegion(JsonObject order,int idOrder, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void createOrdersRegion(JsonObject order, UserInfos event, Number id_project, Handler<Either<String, JsonObject>> handler);

    void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void getOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void getAllOrderRegion(Handler<Either<String, JsonArray>> handler);

    void createProject (String title,  Handler<Either<String, JsonObject>> handler);

    void getAllOrderRegionByProject(int idProject, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getAllProjects(UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(String query, UserInfos user, JsonArray equipTab, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchWithoutEquip(String query, UserInfos user, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchName(String word, Handler<Either<String, JsonArray>> handler);

    void filter(UserInfos user, String startDate, String endDate, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearch(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, JsonArray filters, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearchWithoutEquip(UserInfos user, String query, String startDate, String endDate, JsonArray filters, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler);
}
