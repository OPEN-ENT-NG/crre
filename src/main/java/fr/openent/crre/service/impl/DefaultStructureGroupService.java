package fr.openent.crre.service.impl;


import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StructureGroupService;
import fr.openent.crre.utils.SqlQueryUtils;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

/**
 * Created by agnes.lapeyronnie on 28/12/2017.
 */
public class DefaultStructureGroupService implements StructureGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructureGroupService.class);
    private final ServiceFactory serviceFactory;

    public DefaultStructureGroupService(ServiceFactory serviceFactory){
        this.serviceFactory = serviceFactory;
    }

    @Override
    public Future<List<StructureGroupModel>> listStructureGroups() {
        Promise<List<StructureGroupModel>> promise = Promise.promise();

        String query = "SELECT id, name, description, array_to_json(array_agg(id_structure)) as structures FROM "
                + Crre.crreSchema + ".structure_group " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure" +
                " on structure_group.id = rel_group_structure.id_structure_group " +
                "group by (id, name , description ) ORDER BY id;";
        String errorMessage = String.format("[CRRE@%s::listStructureGroups] Error when listing structure", this.getClass().getSimpleName());
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, StructureGroupModel.class, errorMessage)));

        return promise.future();
    }

    @Override
    public Future<JsonObject> create(final StructureGroupModel structureGroup) {
        Promise<JsonObject> promise = Promise.promise();

        Future<Integer> getNextValFuture = SqlHelper.getNextVal("structure_group_id_seq");
        Future<List<String>> getOldIdStructureListFuture = this.getOldIdStructureList();

        CompositeFuture.all(Arrays.asList(getNextValFuture, getOldIdStructureListFuture))
                .compose(actualIdStructureList -> {
                    Integer nextVal = getNextValFuture.result();
                    List<TransactionElement> statements = new ArrayList<>();
                    statements.add(getStructureGroupCreationStatement(nextVal, structureGroup));
                    statements.add(getGroupStructureRelationshipStatement(nextVal, structureGroup.getStructures()));
                    String errorMessage = String.format("[CRRE@%s::create] Fail to create structure group", this.getClass().getSimpleName());
                    return TransactionHelper.executeTransaction(statements, errorMessage);
                })
                .onSuccess(result -> {
                    Integer nextVal = getNextValFuture.result();
                    List<String> newIdStructureList = getNewIdStructure(structureGroup.getStructures(), getOldIdStructureListFuture.result());
                    if (!newIdStructureList.isEmpty()) {
                        getStudentsByStructures(newIdStructureList);
                    }
                    promise.complete(new JsonObject().put(Field.ID, nextVal));
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    //Todo revoir les logs
    private void getStudentsByStructures(List<String> structures) {
        this.serviceFactory.getStructureService().insertStudentsInfos(new JsonArray(new ArrayList<>(structures)), event -> {
            if(event.isRight()) {
                LOGGER.info("Insert total success");
            } else {
                LOGGER.error("Failed to insert : " + event.left());
            }
        });
    }

    private List<String> getNewIdStructure(List<String> idsStructures, List<String> actualIdStructureList) {
        return idsStructures.stream()
                .filter(idStructure -> !actualIdStructureList.contains(idStructure))
                .collect(Collectors.toList());
    }

    @Override
    public Future<JsonObject> update(final Integer id, StructureGroupModel structureGroup) {
        Promise<JsonObject> promise = Promise.promise();

        Future<List<String>> oldIdStructureListFuture = this.getOldIdStructureList();

        oldIdStructureListFuture
                .compose(oldIdStructureList -> {

                    List<TransactionElement> statements = new ArrayList<>();
                    statements.add(getStructureGroupUpdateStatement(id, structureGroup));
                    statements.add(getStructureGroupRelationshipDeletion(id));
                    statements.add(getGroupStructureRelationshipStatement(id, structureGroup.getStructures()));
                    String errorMessage = String.format("[CRRE@%s::update] Fail to update structure group", this.getClass().getSimpleName());
                    return TransactionHelper.executeTransaction(statements, errorMessage);
                })
                .onSuccess(res -> {
                    List<String> newIds = getNewIdStructure(structureGroup.getStructures(), oldIdStructureListFuture.result());
                    if(!newIds.isEmpty()) {
                        getStudentsByStructures(newIds);
                    }
                    promise.complete(new JsonObject().put(Field.ID, id));
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<JsonObject> delete(final List<Integer> ids) {
        Promise<JsonObject> promise = Promise.promise();

        if (ids.isEmpty()) {
            promise.complete(new JsonObject());
            return promise.future();
        }

        List<TransactionElement> transactionElementList = new ArrayList<>();
        transactionElementList.add(getStructureGroupRelationshipDeletion(ids));
        transactionElementList.add(getStructureGroupDeletion(ids));

        String errorMessage = String.format("[CRRE@%s::delete] Fail to delete structure group", this.getClass().getSimpleName());
        TransactionHelper.executeTransaction(transactionElementList, errorMessage)
                .onSuccess(result -> promise.complete(new JsonObject().put(Field.ID, new JsonArray(new ArrayList<>(ids)))))
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Returns a structureGroup creation statement
     * @param id             structureGroup id
     * @param structureGroup structureGroup to create
     * @return structureGroup creation statement
     */
    private TransactionElement getStructureGroupCreationStatement(Integer id, StructureGroupModel structureGroup){
        String insertStructureGroupQuery = "INSERT INTO "+ Crre.crreSchema +
                ".structure_group(id, name, description) VALUES (?,?,?) RETURNING id;";
        JsonArray params = new JsonArray()
       .add(id)
       .add(structureGroup.getName())
       .add(structureGroup.getDescription());
        return new TransactionElement(insertStructureGroupQuery, params);
    }

    /**
     * Returns  a structureGroup idStructure relationship transaction statement
     * @param idStructureGroup group id
     * @param idsStructure structure ids
     * @return structureGroup idStructure relationship transaction statement
     */
    private TransactionElement getGroupStructureRelationshipStatement(Integer idStructureGroup, List<String> idsStructure) {
        StringBuilder insertGroupStructureRelationshipQuery = new StringBuilder();
        JsonArray params = new JsonArray();
        insertGroupStructureRelationshipQuery.append("INSERT INTO ").append(Crre.crreSchema)
                .append(".rel_group_structure(id_structure,id_structure_group) VALUES ");

        insertGroupStructureRelationshipQuery.append(idsStructure.stream()
                .map(idStructure -> {
                    params.add(idStructure)
                            .add(idStructureGroup);
                    return "(?,?)";
                })
                .collect(Collectors.joining(",")));

        insertGroupStructureRelationshipQuery.append(" ON CONFLICT DO NOTHING RETURNING id_structure;");
        return new TransactionElement(insertGroupStructureRelationshipQuery.toString(), params);
    }

    /**
     * Returns the update statement
     * @param id structure_group
     * @param structureGroup to update
     * @return update statement
     */
    private TransactionElement getStructureGroupUpdateStatement(Number id, StructureGroupModel structureGroup){
        String query = "UPDATE "+ Crre.crreSchema + ".structure_group " +
                "SET name = ?, description = ? WHERE id = ?;";
        JsonArray params = new JsonArray()
                .add(structureGroup.getName())
                .add(structureGroup.getDescription())
                .add(id);
        return new TransactionElement(query, params);
    }

    /**
     * Delete in rel_group_structure
     * @param idStructureGroup of structureGroup
     * @return Delete statement
     */
    private TransactionElement getStructureGroupRelationshipDeletion(Number idStructureGroup){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure WHERE id_structure_group = ?;";

        return new TransactionElement(query, new JsonArray().add(idStructureGroup));
    }

    /**
     * Delete all ids group in rel_group_structure
     * @param ids list of id group
     * @return Delete statement
     */
    private TransactionElement getStructureGroupRelationshipDeletion(List<Integer> ids){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure " +
                "WHERE id_structure_group IN " +Sql.listPrepared(ids.toArray());
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new TransactionElement(query, params);
    }

    /**
     * Delete all ids structureGroup in structure_group
     * @param ids list of id_group_structure
     * @return Delete statement
     */
    private TransactionElement getStructureGroupDeletion(List<Integer> ids){
        String query = "DELETE FROM "+ Crre.crreSchema +".structure_group " +
                "WHERE id IN "+Sql.listPrepared(ids.toArray());
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new TransactionElement(query, params);
    }

    private Future<List<String>> getOldIdStructureList() {
        Promise<List<String>> promise = Promise.promise();

        Sql.getInstance().raw("SELECT DISTINCT r.id_structure FROM " + Crre.crreSchema + ".rel_group_structure r ", SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                String message = String.format("An error occurred when selecting next val %s", event.left().getValue());
                LOGGER.error(String.format("[CRRE@%s::getNext] %s", this.getClass().getSimpleName(), message));
                promise.fail(message);
            } else  {
                List<String> actualIdStructureList = event.right().getValue().stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toList());
                promise.complete(actualIdStructureList);
            }
        }));

        return promise.future();
    }
}
