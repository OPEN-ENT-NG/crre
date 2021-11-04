package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class DefaultStructureService extends SqlCrudService implements StructureService {

    private final Neo4j neo4j;

    public DefaultStructureService(String schema) {
        super(schema, "");
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL " +
                "RETURN left(s.zipCode, 2) as department, s.id as id, s.name as name,s.city as city,s.UAI as uai, " +
                "s.academy as academy, s.type as type_etab ";
        neo4j.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresByTypeAndFilter(String type, List<String> filterStructures, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL ";
        JsonObject values = new JsonObject();
        if (type != null) {
            query += "AND s.contract = {type} ";
            values.put("type", type);
        }
        if (filterStructures != null && !filterStructures.isEmpty()) {
            query += "AND s.id IN {stuctureIds} ";
            values.put("stuctureIds", new JsonArray(filterStructures));
        }
        query += "RETURN s.id as idStructure; ";
        neo4j.execute(query, values, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructureByUAI(JsonArray uais, List<String> consumable_formations, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)";
        JsonObject params = new JsonObject();
        if (consumable_formations == null) {
            query += " WHERE s.UAI IN {uais} RETURN s.id as id, s.UAI as uai, s.type as type";
            params.put("uais", uais);
        } else {
            query += "<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles:['Student']}) " +
                    "WHERE s.UAI IN {uais} WITH s.id as id, s.UAI as uai, s.type as type, " +
                    "CASE WHEN u.level IN {consumable_formations} " +
                    "THEN count(u) " +
                    "ELSE 0 " +
                    "END as nbr_students " +
                    "RETURN id, uai, type, sum(nbr_students) AS nbr_students_consumables;";
            params.put("uais", uais).put("consumable_formations", new JsonArray(consumable_formations));
        }

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructureById(JsonArray ids, List<String> consumable_formations, Handler<Either<String, JsonArray>> handler) {
        try {
            String query = "MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles:['Student']})" +
                    " WHERE s.id IN {ids} ";
            JsonObject params = new JsonObject().put("ids", ids);
            if (consumable_formations == null) {
                query += "RETURN DISTINCT s.id as id, s.UAI as uai, s.name as name, s.phone as phone, " +
                        "s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                        "s.zipCode  as zipCode, s.city as city, s.type as type ";
            } else {
                query += "WITH s, " +
                        "CASE WHEN u.level IN {consumable_formations} " +
                        "THEN count(u) " +
                        "ELSE 0 " +
                        "END as nbr_students " +
                        "RETURN DISTINCT s.id as id, s.UAI as uai, s.name as name, s.phone as phone, " +
                        "s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                        "s.zipCode  as zipCode, s.city as city, s.type as type, sum(nbr_students)*2 AS minimum_licences_consumables;";
                params.put("consumable_formations", new JsonArray(consumable_formations));
            }
            Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
        } catch (VertxException e) {
            getStructureById(ids, null, handler);
        }
    }

    @Override
    public void searchStructureByNameUai(String q, Handler<Either<String, JsonArray>> handler) {
        q = ".*" + q + ".*";
        String query = "MATCH (s:Structure) WHERE (toLower(s.name) =~ {word} OR toLower(s.UAI) =~ {word}) return s.id as id, s.UAI as uai," +
                " s.name as name, s.phone as phone, s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                "s.zipCode  as zipCode, s.city as city, s.type as type;";

        Neo4j.getInstance().execute(query,
                new JsonObject().put("word", q),
                Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStudentsByStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles:['Student']}) " +
                "where s.id IN {ids} RETURN distinct u.level, count(u), s.id;";
        neo4j.execute(query, new JsonObject().put("ids", structureIds), Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getConsumableFormation(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT label" +
                " FROM " + Crre.crreSchema + ".consumable_formation";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertStructures(JsonArray structures, Handler<Either<String, JsonArray>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".students(id_structure) VALUES ";
        JsonArray params = new JsonArray();
        for (int i = 0; i < structures.size(); i++) {
            String structure = structures.getString(i);
            query += i < structures.size() - 1 ? "(?), " : "(?)";
            params.add(structure);
        }
        query += " ON CONFLICT (id_structure) DO UPDATE " +
                "SET total_april = 0;";
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertTotalStructure(JsonArray total, JsonObject consumableFormationsStudents,
                                     Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".licences" +
                "(id_structure, initial_amount, amount, consumable_initial_amount, consumable_amount) VALUES";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < total.size(); i++) {
            JsonObject structure_total = total.getJsonObject(i);
            query += i < total.size() - 1 ? "(?, ?, ?, ?, ?), " : "(?, ?, ?, ?, ?) ";
            int total_licence;
            int total_licence_consumable = 0;
            if (structure_total.getBoolean("pro")) {
                total_licence = structure_total.getInteger("Seconde") * 3 +
                        structure_total.getInteger("Premiere") * 3 + structure_total.getInteger("Terminale") * 3;
            } else {
                total_licence = structure_total.getInteger("Seconde") * 9 +
                        structure_total.getInteger("Premiere") * 8 + structure_total.getInteger("Terminale") * 7;
            }
            if (consumableFormationsStudents.containsKey(structure_total.getString("id_structure"))) {
                total_licence_consumable = consumableFormationsStudents.getInteger(structure_total.getString("id_structure")) * 2;
            }
            params.add(structure_total.getString("id_structure"))
                    .add(total_licence).add(total_licence).add(total_licence_consumable).add(total_licence_consumable);
        }
        query += "ON CONFLICT (id_structure) DO UPDATE " +
                "SET initial_amount = excluded.initial_amount, amount = excluded.amount," +
                "consumable_initial_amount = excluded.consumable_amount, consumable_amount = excluded.consumable_amount";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void insertStudents(JsonArray students, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";
        for (int i = 0; i < students.size(); i++) {
            JsonObject j = students.getJsonObject(i);
            String s = j.getString("s.id");
            Integer count = j.getInteger("count(u)");
            if (j.getString("u.level") != null) {
                switch (j.getString("u.level")) {
                    case "SECONDE GENERALE & TECHNO YC BT": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Seconde\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = false WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "PREMIERE GENERALE & TECHNO YC BT": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Premiere\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = false WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "TERMINALE GENERALE & TECHNO YC BT": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Terminale\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = false WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : 2NDE PRO (OU 1ERE ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Seconde\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : 1ERE PRO (OU 2EME ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Premiere\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : TERM PRO (OU 3EME ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET \"Terminale\" = ?, total_april = total_april + ?, " +
                                "\"pro\" = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (query.length() > 5) {
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        } else {
            handler.handle(new Either.Right<>(new JsonObject()));
        }
    }

    @Override
    public JsonObject getNumberStudentsConsumableFormations(JsonArray students, List<String> consumableFormations) {
        JsonObject consumableFormationsStudentsPerStructures = new JsonObject();
        for (int i = 0; i < students.size(); i++) {
            JsonObject j = students.getJsonObject(i);
            String s = j.getString("s.id");
            Integer count = j.getInteger("count(u)");
            if (j.getString("u.level") != null && consumableFormations.contains(j.getString("u.level"))) {
                if (consumableFormationsStudentsPerStructures.containsKey(s)) {
                    count += consumableFormationsStudentsPerStructures.getInteger(s);
                }
                consumableFormationsStudentsPerStructures.put(s, count);
            }
        }
        return consumableFormationsStudentsPerStructures;
    }

    @Override
    public void getTotalStructure(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT \"Premiere\", \"Terminale\", \"Seconde\", id_structure, pro" +
                " FROM " + Crre.crreSchema + ".students";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateAmount(String id_structure, Integer seconde, Integer premiere, Integer terminale, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".students " +
                " SET \"Seconde\" = ?, \"Premiere\" = ?, \"Terminale\" = ? " +
                "WHERE id_structure = ?";
        values.add(seconde).add(premiere).add(terminale).add(id_structure);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getAmount(String id_structure, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " SELECT \"Seconde\", \"Premiere\", \"Terminale\", \"Seconde\" + \"Premiere\" + \"Terminale\" as total, " +
                "total_april, pro " +
                "FROM " + Crre.crreSchema + ".students " +
                "WHERE id_structure = ?";
        values.add(id_structure);
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void reinitAmountLicence(String id_structure, Integer difference, Handler<Either<String, JsonObject>> defaultResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".licences " +
                "SET initial_amount = initial_amount + ?, amount = amount + ? " +
                "WHERE id_structure = ?";
        values.add(difference).add(difference).add(id_structure);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(defaultResponseHandler));
    }

    @Override
    public void updateAmountLicence(String idStructure, String operation, Integer licences,
                                    Handler<Either<String, JsonObject>> handler) {
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".licences " +
                "SET amount = amount " + operation + " ?  " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(licences)
                .add(idStructure);

        Sql.getInstance().prepared(updateQuery, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateAmountConsumableLicence(String idStructure, String operation, Integer licences,
                                              Handler<Either<String, JsonObject>> handler) {
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".licences " +
                "SET consumable_amount = consumable_amount " + operation + " ?  " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(licences)
                .add(idStructure);

        Sql.getInstance().prepared(updateQuery, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAllStructure(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_structure FROM " + Crre.crreSchema + ".rel_group_structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertNewStructures(JsonArray structures, Handler<Either<String, JsonObject>> handler) throws ParseException {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                " INSERT INTO " + Crre.crreSchema + ".structure" +
                " (uai, id_structure, name, public, mixte, catalog, city, region) VALUES ";

        for (int i = 0; i < structures.size(); i++) {
            query += "(?, ?, ?, ?, ?, ?, ?, ?),";
            JsonObject structure = structures.getJsonObject(i);
            params.add(structure.getString("uai"))
                    .add(structure.getString("id"))
                    .add(structure.getString("name"))
                    .add(structure.getString("public"))
                    .add((structure.getString("mixte").equals("Vrai")))
                    .add(structure.getString("catalog"))
                    .add(structure.getString("city"))
                    .add(structure.getString("region"));
        }
        query = query.substring(0, query.length() - 1);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateReliquats(JsonArray structures, Handler<Either<String, JsonObject>> handler) throws ParseException {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "";
        for (int i = 0; i < structures.size(); i++) {
            query += "UPDATE " + Crre.crreSchema + ".licences " +
                    "SET amount = amount + ? " +
                    "WHERE id_structure = ?; ";
            JsonObject structure = structures.getJsonObject(i);
            params.add(Integer.parseInt(structure.getString("reliquat")))
                    .add(structure.getString("id"));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void getAllStructuresDetail(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_structure, name, uai, public, mixte, catalog, technical, general, city, region" +
                " FROM " + Crre.crreSchema + ".structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAllStructuresDetailByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_structure as id, uai" +
                " FROM " + Crre.crreSchema + ".structure" +
                " WHERE uai IN " + Sql.listPrepared(uais);
        sql.prepared(query, uais, SqlResult.validResultHandler(handler));
    }

    @Override
    public void insertStudentsInfos(JsonArray ids, Handler<Either<String, JsonObject>> eitherHandler) {
        Future<JsonArray> getStudentsByStructureFuture = Future.future();
        Future<JsonArray> insertStructuresFuture = Future.future();

        CompositeFuture.all(getStudentsByStructureFuture, insertStructuresFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray students = getStudentsByStructureFuture.result();
                insertStudents(students, result -> {
                    if (result.isRight()) {
                        getConsumableFormation(formations -> {
                            if (formations.isRight()) {
                                JsonArray res_consumable_formations = formations.right().getValue();
                                List<String> consumable_formations = res_consumable_formations
                                        .stream()
                                        .map((json) -> ((JsonObject) json).getString("label"))
                                        .collect(Collectors.toList());
                                JsonObject consumableFormationsStudents =
                                        getNumberStudentsConsumableFormations(students, consumable_formations);
                                getTotalStructure(total_structure -> {
                                    JsonArray total = total_structure.right().getValue();
                                    insertTotalStructure(total, consumableFormationsStudents, event2 -> {
                                        if (event2.isRight()) {
                                            eitherHandler.handle(new Either.Right<>(event2.right().getValue()));
                                        } else {
                                            eitherHandler.handle(new Either.Left<>("Failed to insert : " + event2.left()));
                                        }
                                    });
                                });
                            } else {
                                eitherHandler.handle(new Either.Left<>("Failed to get all consumables formations : " + formations.left()));
                            }
                        });
                    } else {
                        eitherHandler.handle(new Either.Left<>("Failed to get students or insert into structure : " + result.left()));
                    }
                });
            } else {
                eitherHandler.handle(new Either.Left<>("Failed to get students or insert into structure : " + event.cause()));
            }
        });
        insertStructures(ids, handlerJsonArray(insertStructuresFuture));
        getStudentsByStructure(ids, handlerJsonArray(getStudentsByStructureFuture));
    }
}
