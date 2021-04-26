package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.util.List;

public interface OldOrderService {

    void getAllOrderRegionByProject(int idProject, boolean filterRejectedOrders, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filter_only(UserInfos user, String startDate, String endDate, String idStructure, JsonArray filters,
                     Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearch(UserInfos user, String query, String startDate, String endDate, String idStructure, JsonArray filters,
                      Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void insertOldOrders(JsonArray orderRegions, boolean isRenew, Handler<Either<String, JsonObject>> handlerJsonArray) throws ParseException;

    void insertOldClientOrders(JsonArray orderRegions, Handler<Either<String, JsonObject>> handler) throws ParseException;


}
