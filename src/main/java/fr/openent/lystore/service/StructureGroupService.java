package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;


/**
 * Created by agnes.lapeyronnie on 28/12/2017.
 */
public interface StructureGroupService {
    /**
     * List all Structure Groups in database
     * @param handler Function handler returning data
     */
    void listStructureGroups(Handler<Either<String, JsonArray>> handler);

    /**
     * Create a structure group
     * @param structureGroup structureGroup to create
     * @param handler Function handler returning data
     */
    void create(JsonObject structureGroup, Handler<Either<String, JsonObject>> handler);


    /**
     * Update a structure group
     * @param id structure group id
     * @param structureGroup structureGroup to update
     * @param handler Function handler returning data
     */
    void update(Integer id, JsonObject structureGroup, Handler<Either<String, JsonObject>> handler);


    /**
     * Delete a structure group based on ids
     * @param ids structure groups to delete
     * @param handler Function handler returning data
     */
    void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);


    void listStructureGroupsByCampaign(Integer campaignId, Handler<Either<String, JsonArray>> handler);
}
