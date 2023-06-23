package fr.openent.crre.service;

import fr.openent.crre.model.PurseImport;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface PurseService {

    /**
     * Launch purse import
     *
     * @param statementsValues Object containing structure ids as key and purse amount as value
     * @param uaiErrorList
     */
    Future<PurseImport> launchImport(JsonObject statementsValues, List<String> uaiErrorList);

    /**
     * Get purses by campaign id
     * @param handler handler function returning data
     */
    void getPursesStudentsAndLicences(List<String> params, Handler<Either<String, JsonArray>> handler);

    /**
     * Get purses by campaign id
     * @param handler handler function returning data
     */
    void getPursesStudentsAndLicences(Integer page, JsonArray idStructures,  Handler<Either<String, JsonArray>> handler);

    /**
     * Update a purse based on his id structure
     *
     * @param idStructure Purse id
     * @param purse        purse object
     * @return
     */
    Future<Void> update(String idStructure, JsonObject purse);

    /**
     * decrease or increase an amount of Purse
     * @param price total price of an equipment (with options)
     * @param idStructure Structure id
     * @param operation "+" or "-"
     * @param handler
     */
    void updatePurseAmount(Double price, String idStructure, String operation, Boolean consumable, Handler<Either<String, JsonObject>> handler);

    TransactionElement getTransactionUpdatePurseAmount(Double price, String idStructure, String operation, Boolean consumable);
}
