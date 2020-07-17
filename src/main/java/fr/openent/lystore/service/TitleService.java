package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TitleService {
    /**
     * List all the titles for a provided campaign identifier
     *
     * @param idCampaign Campaign identifier
     * @param handler Function handler returning data
     */
    void getTitles(Integer idCampaign, Handler<Either<String, JsonArray>> handler);

    /**
     * List all titles based on campaign identifier and structure identifier
     *
     * @param idCampaign  Campaign identifier
     * @param structureId Structure identifier
     * @param handler     Function handler returning data
     */
    void getTitles(Integer idCampaign, String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * Import titles for a specific campaign
     *
     * @param idCampaign campaign identifier
     * @param importMap  Map containing data using existing titles
     * @param newTitlesMap Map contining data using new titles
     * @param handler    Function handler returning data
     */
    void importTitlesForCampaign(Integer idCampaign, JsonObject importMap, JsonObject newTitlesMap, Handler<Either<String, JsonObject>> handler);

    /**
     * List all titles.
     *
     * @param handler Function handler returning data
     */
    void getTitles(Handler<Either<String, JsonArray>> handler);

    /**
     * List all relation based on provided campaign identifier
     *
     * @param idCampaign Campaign identifier
     * @param handler    Function Handler returning data
     */
    void getRelationForCampaign(Integer idCampaign, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete a relation between campaign, title and structure
     *
     * @param idCampaign  Campaign identifier
     * @param idTitle     Title identifier
     * @param idStructure Structure identifier
     * @param handler     Function handler returning data
     */
    void deleteRelation(Integer idCampaign, Integer idTitle, String idStructure, Handler<Either<String, JsonObject>> handler);

    void getTitlesAdmin(Integer campaignId, Handler<Either<String, JsonArray>> handler);
}
