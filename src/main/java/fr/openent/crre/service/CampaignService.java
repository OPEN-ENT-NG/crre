package fr.openent.crre.service;

import fr.openent.crre.model.Campaign;
import fr.openent.crre.model.StructureGroupModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface CampaignService {

    /**
     * List all campaigns in database
     * @param handler function handler returning data
     */
    void listCampaigns(Handler<Either<String, JsonArray>> handler);

    /**
     * List all campaigns of a structure
     * @param  idStructure of the structure
     * @param handler function handler returning data
     */
    void listCampaigns(String idStructure, Handler<Either<String, JsonArray>> handler, UserInfos user);
    /**
     * Get a campaign information
     * @param id id campaign to get
     */
    Future<JsonObject> getCampaign(Integer id);

    /**
     * Create a campaign
     * @param campaign campaign model to create
     * @param groups list of associated structureGroup
     */
    Future<Campaign> create(Campaign campaign, List<StructureGroupModel> groups);

    /**
     * Update a campaign
     * @param campaign campaign model to update
     * @param groups list of associated structureGroup
     */
    Future<Campaign> update(Campaign campaign, List<StructureGroupModel> groups);
    /**
     * Update an accessibility campaign
     * @param id campaign id to update
     * @param campaign campaign object
     * @param handler function handler returning data
     */
    void updateAccessibility(Integer id, JsonObject campaign, Handler<Either<String, JsonObject>> handler);
    /**
     * Delete an campaign
     * @param ids campaign ids to delete
     * @param handler function handler returning data
     */
    void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * List all structure identifiers in the campaign
     *
     * @param idCampaign Campaign identifier
     * @param handler    Function handler returning data
     */
    void getStructures(Integer idCampaign, Handler<Either<String, JsonArray>> handler);

    /**
     * List all types in the campaign
     *
     * @param handler    Function handler returning data
     */
    void getCampaignTypes(Handler<Either<String, JsonArray>> handler);
}
