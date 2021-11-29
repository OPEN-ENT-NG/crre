package fr.openent.crre.service.impl;


import fr.openent.crre.Crre;
import fr.openent.crre.service.StructureGroupService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

/**
 * Created by agnes.lapeyronnie on 28/12/2017.
 */
public class DefaultStructureGroupService extends SqlCrudService implements StructureGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructureGroupService.class);
    private final DefaultStructureService structureService;

    public DefaultStructureGroupService(String schema, String table){
        super(schema, table);
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
    }

    @Override
    public void listStructureGroups(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, description, array_to_json(array_agg(id_structure)) as structures FROM "
                + Crre.crreSchema + ".structure_group " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure" +
                " on structure_group.id = rel_group_structure.id_structure_group " +
                "group by (id, name , description ) ORDER BY id;";
        this.sql.prepared(query,new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(final JsonObject structureGroup, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "Select nextval('"+ Crre.crreSchema + ".structure_group_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if(event.isRight()) {
                try{
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getStructureGroupCreationStatement(id,structureGroup));

                    JsonArray idsStructures = structureGroup.getJsonArray("structures");
                    JsonArray allIds = new JsonArray();
                    JsonArray newIds = new JsonArray();
                    sql.raw("SELECT DISTINCT r.id_structure FROM " + Crre.crreSchema + ".rel_group_structure r ", SqlResult.validResultHandler(event2 -> {
                        setAllAndNewIds(idsStructures, allIds, newIds, event2);
                        statements.add(getGroupStructureRelationshipStatement(id, idsStructures));

                        sql.transaction(statements, event1 -> {
                            if(!newIds.isEmpty()) {
                                getStudentsByStructures(newIds);
                            }
                            handler.handle(SqlQueryUtils.getTransactionHandler(event1, id));
                        });
                    }));

                }catch(ClassCastException e){
                    LOGGER.error("An error occured when casting structures ids " + e);
                    handler.handle(new Either.Left<>(""));
                }
            }else{
               LOGGER.error("An error occurred when selecting next val");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    private void getStudentsByStructures(JsonArray structures) {
        structureService.insertStudentsInfos(structures, event -> {
            if(event.isRight()) {
                LOGGER.info("Insert total success");
            } else {
                LOGGER.error("Failed to insert : " + event.left());
            }
        });
    }

    private void setAllAndNewIds(JsonArray idsStructures, JsonArray allIds, JsonArray newIds, Either<String, JsonArray> event2) {
        JsonArray structure_id = event2.right().getValue();
        for (int i = 0; i < structure_id.size(); i++) {
            allIds.add(structure_id.getJsonObject(i).getString("id_structure"));
        }
        for (int j = 0; j < idsStructures.size(); j++) {
            if (!allIds.contains(idsStructures.getString(j))) {
                newIds.add(idsStructures.getString(j));
            }
        }
    }

    @Override
    public void update(final Integer id, JsonObject structureGroup,final Handler<Either<String, JsonObject>> handler) {
        JsonArray idsStructures = structureGroup.getJsonArray("structures");
        JsonArray allIds = new JsonArray();
        JsonArray newIds = new JsonArray();
        sql.raw("SELECT DISTINCT r.id_structure FROM " + Crre.crreSchema + ".rel_group_structure r ", SqlResult.validResultHandler(event -> {
            setAllAndNewIds(idsStructures, allIds, newIds, event);
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                    .add(getStructureGroupUpdateStatement(id,structureGroup))
                    .add(getStructureGroupRelationshipDeletion(id))
                    .add(getGroupStructureRelationshipStatement(id,idsStructures));
            sql.transaction(statements, event2 -> {
                if(!newIds.isEmpty()) {
                    getStudentsByStructures(newIds);
                }
                handler.handle(SqlQueryUtils.getTransactionHandler(event2, id));
            });
        }));
    }

    @Override
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getStructureGroupRelationshipDeletion(ids))
                .add(getStructureGroupDeletion(ids));

        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0))));
    }

    /**
     * Returns a structureGroup creation statement
     * @param id             structureGroup id
     * @param structureGroup structureGroup to create
     * @return structureGroup creation statement
     */
    private JsonObject getStructureGroupCreationStatement(Number id, JsonObject structureGroup){
        String insertStructureGroupQuery = "INSERT INTO "+ Crre.crreSchema +
                ".structure_group(id, name, description) VALUES (?,?,?) RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
       .add(id)
       .add(structureGroup.getString("name"))
       .add(structureGroup.getString("description"));
        return new JsonObject()
                .put("statement", insertStructureGroupQuery)
                .put("values",params)
                .put("action","prepared");
    }

    /**
     * Returns  a structureGroup idStructure relationship transaction statement
     * @param idStructureGroup group id
     * @param idsStructure structure ids
     * @return structureGroup idStructure relationship transaction statement
     */
    private JsonObject getGroupStructureRelationshipStatement(Number idStructureGroup, JsonArray idsStructure) {
        StringBuilder insertGroupStructureRelationshipQuery = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        insertGroupStructureRelationshipQuery.append("INSERT INTO ").append(Crre.crreSchema)
        .append(".rel_group_structure(id_structure,id_structure_group) VALUES ");

        for(int i = 0; i < idsStructure.size();i++ ){
            String idStructure = idsStructure.getString(i);
            insertGroupStructureRelationshipQuery.append("(?,?)");
            params.add(idStructure)
                    .add(idStructureGroup);
            if(i != idsStructure.size()-1){
                insertGroupStructureRelationshipQuery.append(",");
            }
        }
        insertGroupStructureRelationshipQuery.append(" ON CONFLICT DO NOTHING RETURNING id_structure;");
        return new JsonObject()
                .put("statement",insertGroupStructureRelationshipQuery.toString())
                .put("values",params)
                .put("action","prepared");
    }

    /**
     * Returns the update statement
     * @param id structure_group
     * @param structureGroup to update
     * @return update statement
     */
    private JsonObject getStructureGroupUpdateStatement(Number id, JsonObject structureGroup){
        String query = "UPDATE "+ Crre.crreSchema + ".structure_group " +
                "SET name = ?, description = ? WHERE id = ?;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(structureGroup.getString("name"))
                .add(structureGroup.getString("description"))
                .add(id);
        return new JsonObject()
        .put("statement", query)
        .put("values",params)
        .put("action","prepared");
    }

    /**
     * Delete in rel_group_structure
     * @param idStructureGroup of structureGroup
     * @return Delete statement
     */
    private JsonObject getStructureGroupRelationshipDeletion(Number idStructureGroup){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure WHERE id_structure_group = ?;";

        return new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray().add(idStructureGroup))
                .put("action", "prepared");
    }

    /**
     * Delete all ids group in rel_group_structure
     * @param ids list of id group
     * @return Delete statement
     */
    private JsonObject getStructureGroupRelationshipDeletion(List<Integer> ids){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure " +
                "WHERE id_structure_group IN " +Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action","prepared");
    }

    /**
     * Delete all ids structureGroup in structure_group
     * @param ids list of id_group_structure
     * @return Delete statement
     */
    private JsonObject getStructureGroupDeletion(List<Integer> ids){
        String query = "DELETE FROM "+ Crre.crreSchema +".structure_group " +
                "WHERE id IN "+Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action","prepared");
    }

}
