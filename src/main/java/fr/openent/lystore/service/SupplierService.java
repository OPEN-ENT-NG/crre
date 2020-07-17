package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface SupplierService {

    /**
     * List all holders in database
     * @param handler Function handler returning data
     */
    void getSuppliers(Handler<Either<String, JsonArray>> handler);

    /**
     * Create an agent based on agent object
     * @param holder object containing data
     * @param handler Function handler returning data
     */
    void createSupplier(JsonObject holder, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an agent based on agent object
     * @param id Agent id to update
     * @param supplier supplier object
     * @param handler Function handler returning data
     */
    void updateSupplier(Integer id, JsonObject supplier, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete an Agent based on ids
     * @param ids holder ids to delete
     * @param handler Function handler returning data
     */
    void deleteSupplier(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Get supplier by Id
     * @param id supplier id
     * @param handler Function handler returning data
     */
    void getSupplier(String id, Handler<Either<String, JsonObject>> handler);

    void getSupplierByValidationNumbers(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler);
}
