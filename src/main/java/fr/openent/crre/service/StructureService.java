package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface StructureService {

    /**
     * list all Structures in database
     * @param handler function handler returning data
     */
    void getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    void getStudentsByStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler);

    void getTotalStructure(Handler<Either<String, JsonArray>> handler);

    void getAllStructure(Handler<Either<String, JsonArray>> handler);

    void insertStructures(JsonArray structures, Handler<Either<String, JsonArray>> handler);

    void insertStudents(JsonArray students, Handler<Either<String, JsonObject>> defaultResponseHandler);

    void insertTotalStructure(JsonArray total, Handler<Either<String, JsonObject>> handler);
}
