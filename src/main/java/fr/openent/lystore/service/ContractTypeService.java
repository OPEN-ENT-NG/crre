package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface ContractTypeService {

    /**
     * List all contract types in database
     * @param handler Function handler returning data
     */
    void listContractTypes(Handler<Either<String, JsonArray>> handler);
}
