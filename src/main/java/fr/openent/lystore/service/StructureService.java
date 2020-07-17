package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface StructureService {

    /**
     * list all Structures in database
     * @param handler function handler returning data
     */
    void getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructureTypes(Handler<Either<String,JsonArray>> handler);

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);
}
