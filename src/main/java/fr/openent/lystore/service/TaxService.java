package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface TaxService {

    /**
     * List all taxes in database
     * @param handler function handler returning data
     */
    void list (Handler<Either<String, JsonArray>> handler);
}
