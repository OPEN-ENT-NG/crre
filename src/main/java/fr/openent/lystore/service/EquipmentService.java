package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface EquipmentService {

    /**
     * List all equipments in database
     * @param page page number
     * @param order order name
     * @param reverse reverse list
     * @param filters search queries
     * @param handler function handler returning data
     */
    void listEquipments(Integer page, String order, Boolean reverse, List<String> filters, Handler<Either<String, JsonArray>> handler);

    /**
     * List equipments of Campaign and a structure  in database
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param page Page number
     * @param filters Filter list
     * @param handler function handler returning data
     */
    void listEquipments(Integer idCampaign, String idStructure, Integer page, List<String> filters,
                        Handler<Either<String, JsonArray>> handler);

    /**
     * Get an Equipment information's
     * @param idEquipment equipment identifier
     * @param handler function handler returning data
     */
    void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler);

    /**
     * Create an equipment
     * @param equipment equipment to create
     * @param handler function handler returning data
     */
    void create(JsonObject equipment, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an equipment
     * @param id equipment id to update
     * @param equipment equipment to update
     * @param handler function handler returning data
     */
    void updateEquipment(Integer id, JsonObject equipment, Handler<Either<String, JsonObject>> handler);
    /**
     * Update options of an equipment
     * @param id equipment id to update
     * @param equipment equipment to update
     * @param resultsObject the object returned by [prepareUpdateOptions()]
     * @param handler function handler returning data
     */
    void updateOptions(Number id, JsonObject equipment,  JsonObject resultsObject,
                       Handler<Either<String, JsonObject>> handler);
    /**
     * Delete an equipment
     * @param ids equipment ids to delete
     * @param handler function handler returning data
     */
    void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Update status for ids list
     * @param ids Ids list
     * @param status Status
     * @param handler Function handler returning data
     */
    void setStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler);

    /**
     * Get Basket's ids for an equipment and alloc sequences for options to create
     * @param numberOptionsCreate number of options to create
     * @param idEquipment id of the equipment
     * @param handler
     * return : ids of baskets who contains the Equipment
     *          ids allocated to create new options
     */
    void prepareUpdateOptions (Number numberOptionsCreate, Number idEquipment,
                                Handler<Either<String, JsonObject>> handler);

    /**
     * Search equipment based on query. Search field are name and reference
     *
     * @param query   queyr searching
     * @param handler Function handler returning data
     */
    void search(String query,  List<String> listFields, Handler<Either<String, JsonArray>> handler);

    /**
     * Create equipments due to an import
     *
     * @param equipments Array of equipments
     * @param handler    Function handler returning data
     */
    void importEquipments(JsonArray equipments, JsonArray referencesToUpdate, Handler<Either<String, JsonObject>> handler);

    /**
     * Get equipment page numbers
     * @param filters Filters list
     * @param handler Function handler returning data
     */
    void getNumberPages(List<String> filters, Handler<Either<String, JsonObject>> handler);

    /**
     * Get equipment pages number
     *
     * @param idCampaign  Campaign identifier
     * @param idStructure Structure identifier
     * @param filters     Filter list
     * @param handler     Function handler returning data
     */
    void getNumberPages(Integer idCampaign, String idStructure, List<String> filters, Handler<Either<String, JsonObject>> handler);

    void listAllEquipments(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);
}
