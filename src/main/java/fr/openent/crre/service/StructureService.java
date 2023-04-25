package fr.openent.crre.service;

import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.StructureNeo4jModel;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.ParseException;
import java.util.List;

public interface StructureService {

    /**
     * list all Structures in database
     * @param handler function handler returning data
     */
    void getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructuresWithoutRight(Handler<Either<String, JsonArray>> handler);

    /**
     * List all Structures by type and/or ids
     * @param structureTypes {@link List<String>} List of structure types (Public, Private ..)
     * @param structureIds {@link List<String>} List of structure ids
     */
    Future<JsonArray> getStructuresFilter(List<String> structureTypes, List<String> structureIds);

    void getStructureByUAI(JsonArray uais, List<String> consumable_formations, Handler<Either<String, JsonArray>> handler);

    Future<List<StructureNeo4jModel>> getStructureNeo4jById(List<String> structureIdList);

    /**
     * @deprecated Use {@link #getStructureNeo4jById(List)}
     */
    @Deprecated
    void getStructureById(JsonArray ids, List<String> consumable_formations, Handler<Either<String, JsonArray>> handler);

    void searchStructureByNameUai(String q, Handler<Either<String, JsonArray>> handler);

    void getStudentsByStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler);

    JsonObject getNumberStudentsConsumableFormations(JsonArray students, List<String> consumableFormations);

    void getConsumableFormation(Handler<Either<String, JsonArray>> handler);

    void getTotalStructure(Handler<Either<String, JsonArray>> handler);

    TransactionElement getTransactionUpdateAmountLicence(String idStructure, String operation, Integer licences, Boolean consumable);

    void getAllStructure(Handler<Either<String, JsonArray>> handler);

    void getAllStructureByIds(List<String> ids, Handler<Either<String, JsonArray>> handler);

    void insertTotalStructure(JsonArray total, JsonObject consumableFormationsStudents, Handler<Either<String, JsonObject>> handler);

    void insertStructures(JsonArray structures, Handler<Either<String, JsonArray>> handler);

    void insertStudents(JsonArray students, Handler<Either<String, JsonObject>> defaultResponseHandler);

    void updateAmount(String id_structure, JsonObject students, Handler<Either<String, JsonObject>> handler);

    void getAmount(String id_structure, Handler<Either<String, JsonObject>> handler);

    void reinitAmountLicence(String id_structure, Integer difference, Handler<Either<String, JsonObject>> defaultResponseHandler);

    /**
     * Use {@link #getTransactionUpdateAmountLicence(String, String, Integer, Boolean)}
     */
    @Deprecated
    void updateAmountLicence(String idStructure, String operation, Integer licences, Handler<Either<String, JsonObject>> handler);

    /**
     * Use {@link #getTransactionUpdateAmountLicence(String, String, Integer, Boolean)}
     */
    @Deprecated
    void updateAmountConsumableLicence(String idStructure, String operation, Integer licences, Handler<Either<String, JsonObject>> handler);

    void insertNewStructures(JsonArray structures, Handler<Either<String, JsonObject>> handler) throws ParseException;

    void updateReliquats(JsonArray structures, Handler<Either<String, JsonObject>> handler) throws ParseException;

    void getAllStructuresDetail(Handler<Either<String, JsonArray>> handler);

    void getAllStructuresDetailByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    void insertStudentsInfos(JsonArray ids, Handler<Either<String, JsonObject>> eitherHandler);

    void linkRolesToGroup(String groupId, JsonArray roleIds, Handler<Either<String, JsonObject>> handler);

    void createOrUpdateManual(JsonObject group, String structureId, String classId,
                              Handler<Either<String, JsonObject>> result);

    void getRole(String roleName, Handler<Either<String, JsonObject>> handler);
}
