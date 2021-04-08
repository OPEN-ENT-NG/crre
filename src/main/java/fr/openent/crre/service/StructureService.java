package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StructureService {

    /**
     * list all Structures in database
     * @param handler function handler returning data
     */
    void getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructuresByTypeAndFilter(String type, List<String> filterStructures, Handler<Either<String, JsonArray>> handler);

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    void searchStructureByNameUai(String q, JsonArray ids, Integer page, Handler<Either<String, JsonArray>> handler);

    void getStudentsByStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler);

    void getTotalStructure(Handler<Either<String, JsonArray>> handler);

    void getAllStructure(Handler<Either<String, JsonArray>> handler);

    void insertTotalStructure(JsonArray total, Handler<Either<String, JsonObject>> handler);

    void insertStructures(JsonArray structures, Handler<Either<String, JsonArray>> handler);

    void insertStudents(JsonArray students, Handler<Either<String, JsonObject>> defaultResponseHandler);

    void updateAmount(String id_structure, Integer seconde, Integer premiere, Integer terminale, Handler<Either<String, JsonObject>> handler);

    void getAmount(String id_structure, Handler<Either<String, JsonObject>> handler);

    void reinitAmountLicence(String id_structure, Integer difference, Handler<Either<String, JsonObject>> defaultResponseHandler);

    void updateAmountLicence(String idStructure, String operation, Integer licences, Handler<Either<String, JsonObject>> handler);

}
