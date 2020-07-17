package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ContractService {

    /**
     * List all contracts in database
     * @param handler Function handler returning data
     */
    void getContracts (Handler<Either<String, JsonArray>> handler);

    /**
     * Create a contract
     * @param contract contract to create
     * @param handler Function handler returning data
     */
    void createContract (JsonObject contract, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a contract
     * @param contract contract to update
     * @param id contract id
     * @param handler Function handler returning data
     */
    void updateContract (JsonObject contract, Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete one or more contract
     * @param ids contracts to delete
     * @param handler Function handler returning data
     */
    void deleteContract (List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Return contract based on order ids
     * @param ids order ids
     * @param handler Function handler returning data
     */
    void getContract(JsonArray ids, Handler<Either<String, JsonArray>> handler);

}
