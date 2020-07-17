package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface EquipmentTypeService {
    /**
     * List all equipments types in database
     * @param handler function handler returning data
     */
    void list (Handler<Either<String, JsonArray>> handler);
}
