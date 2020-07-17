package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface LogService {

    /**
     * List logs with pagination
     * @param page page log
     * @param handler function handler returning data
     */
    void list(Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Returns number of page
     * @param handler function handler returning data
     */
    void getLogsNumber(Handler<Either<String, JsonObject>> handler);
}
