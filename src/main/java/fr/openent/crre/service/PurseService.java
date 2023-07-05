package fr.openent.crre.service;

import fr.openent.crre.model.PurseImport;
import fr.openent.crre.model.PurseModel;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
     * Get purses by UAI
     *
     * @param page page number
     * @param query UAI filter
     */
    Future<List<PurseModel>> searchPursesByUAI(Integer page, String query);

    /**
     * Get purses by structure id
     *
     * @param page page number
     * @param idStructureList filter for id structure
     */
    Future<List<PurseModel>> getPurses(Integer page, List<String> idStructureList);

    /**
     * Update a purse based on his id structure
     *
     * @param idStructure Purse id
     * @param purse        purse object
     * @return
     */
    Future<Void> update(String idStructure, JsonObject purse);

    /**
     * Increment the added_initial_amount column value based on the new credits value
     * For example if we have 150 credits and the new value is 200 then added_initial_amount will be incremented by 50
     *
     * @param consumable if it's for consumable credits
     * @param newValue the new value for the credits
     * @param structureId the id of the structure when want to change
     */
    TransactionElement incrementAddedInitialAmountFromNewValue(boolean consumable, Double newValue, String structureId);

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
