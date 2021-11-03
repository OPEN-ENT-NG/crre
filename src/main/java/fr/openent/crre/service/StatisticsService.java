package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

public interface StatisticsService {

    void exportMongo(JsonObject jsonObject, Handler<Either<String, JsonObject>> handler);

    void getOrdersByCampaign(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getAmount(String type, String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getOrdersCompute(String ressources, HashMap<String, ArrayList<String>> params, boolean publicField, boolean isReassort, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getLicencesCompute(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getStructureCompute(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder, boolean isReassort, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getAllYears(Handler<Either<String, JsonObject>> handlerJsonObject);

    void getStatsByStructure(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getStats(String type, String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void deleteStatsDay(Handler<Either<String, JsonObject>> handlerJsonObject);
}
