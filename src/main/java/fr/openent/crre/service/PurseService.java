package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface PurseService {

    /**
     * Launch purse import
     * @param statementsValues Object containing structure ids as key and purse amount as value
     * @param handler Function handler
     */
    void launchImport(JsonObject statementsValues, boolean invalidDatas, Handler<Either<String, JsonObject>> handler);

    /**
     * Get purses by campaign id
     * @param handler handler function returning data
     */
    void getPursesStudentsAndLicences(List<String> params, Handler<Either<String, JsonArray>> handler);

    /**
     * Get purses by campaign id
     * @param handler handler function returning data
     */
    void getPursesStudentsAndLicences(Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Update a purse based on his id structure
     * @param id_structure Purse id
     * @param purse purse object
     * @param handler Function handler returning data
     */
    void update(String id_structure, JsonObject purse, Handler<Either<String, JsonObject>> handler);

    /**
     * decrease or increase an amount of Purse
     * @param price total price of an equipment (with options)
     * @param idStructure Structure id
     * @param operation "+" or "-"
     * @param handler
     */
    void updatePurseAmount(Double price, String idStructure, String operation, Handler<Either<String, JsonObject>> handler);

}
