package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ProgramService {

    /**
     * List all programs in database
     * @param handler Function handler returning data
     */
    void listPrograms (Handler<Either<String, JsonArray>> handler);

    /**
     * Returns program based on provided id
     * @param id Program id
     * @param handler Function handler returning data
     */
    void getProgramById (Number id, Handler<Either<String, JsonObject>> handler);
}
