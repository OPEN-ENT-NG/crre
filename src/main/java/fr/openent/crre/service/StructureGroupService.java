package fr.openent.crre.service;

import fr.openent.crre.model.StructureGroupModel;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
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
     */
    Future<List<StructureGroupModel>> listStructureGroups();

    /**
     * Create a structure group
     * @param structureGroup structureGroup to create
     */
    Future<JsonObject> create(StructureGroupModel structureGroup);


    /**
     * Update a structure group
     * @param id structure group id
     * @param structureGroup structureGroup to update
     */
    Future<JsonObject> update(Integer id, StructureGroupModel structureGroup);


    /**
     * Delete a structure group based on ids
     * @param ids structure groups to delete
     */
    Future<JsonObject> delete(List<Integer> ids);

}
