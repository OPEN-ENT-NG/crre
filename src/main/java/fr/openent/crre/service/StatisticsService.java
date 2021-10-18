package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface StatisticsService {

    void exportMongo(JsonObject jsonObject, Handler<Either<String, JsonObject>> handler);

    void getFreeLicences(String id, Handler<Either<String, JsonArray>> handler);

    void getTotalRessources(String id, Handler<Either<String, JsonArray>> handler);

    void getRessources(String id_structure, String type, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getOrdersByYear(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getOrdersByCampaign(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getLicences(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray);

    void getOrdersCompute(String ressources, HashMap<String, ArrayList<String>> params, boolean publicField, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getLicencesCompute(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getStructureCompute(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder, Handler<Either<String, JsonObject>> handlerJsonObject);

    void getAllYears(Handler<Either<String, JsonObject>> handlerJsonObject);
}
