package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class DefaultStructureService extends SqlCrudService implements StructureService {

    private final Neo4j neo4j;
    public DefaultStructureService(String schema){
        super(schema, "");
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL "+
                "RETURN left(s.zipCode, 2) as department, s.id as id, s.name as name,s.city as city,s.UAI as uai, " +
                "s.academy as academy, s.type as type_etab ";
        neo4j.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresByType(String type, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL AND s.contract = {type} "+
                "RETURN s.id as uai ";
        neo4j.execute(query, new JsonObject().put("type", type), Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IN {uais} return s.id as id, s.UAI as uai";

        Neo4j.getInstance().execute(query,
                new JsonObject().put("uais", uais),
                Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        try {
            String query = "MATCH (s:Structure) WHERE s.id IN {ids} return s.id as id, s.UAI as uai," +
                    " s.name as name, s.phone as phone, s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                    "s.zipCode  as zipCode, s.city as city, s.type as type ";

            Neo4j.getInstance().execute(query,
                    new JsonObject().put("ids", ids),
                    Neo4jResult.validResultHandler(handler));
        }catch (VertxException e){
            getStructureById(ids,handler);
        }
    }

    public void getStudentsByStructure(JsonArray structureIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles:['Student']}) " +
                "where s.id IN {ids} RETURN distinct u.level, count(u), s.id;";
        neo4j.execute(query, new JsonObject().put("ids", structureIds), Neo4jResult.validResultHandler(handler));

    }

    public void insertStructures(JsonArray structures, Handler<Either<String, JsonArray>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".students(id_structure) VALUES ";
        JsonArray params = new JsonArray();
        for(int i = 0; i < structures.size(); i++) {
            String structure = structures.getString(i);
            query += i < structures.size() - 1 ? "(?), " : "(?)";
            params.add(structure);
        }
        query += " ON CONFLICT DO NOTHING";
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void insertTotalStructure(JsonArray total, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".licences(id_structure, initial_amount, amount) VALUES";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < total.size(); i++) {
            JsonObject structure_total = total.getJsonObject(i);
            query += i < total.size() - 1 ? "(?, ?, ?), " : "(?, ?, ?) ";
            int total_licence = 0;
            if(structure_total.getBoolean("pro")) {
                total_licence = structure_total.getInteger("Seconde") * 3 + structure_total.getInteger("Premiere") * 3 + structure_total.getInteger("Terminale") * 3;
            } else {
                total_licence = structure_total.getInteger("Seconde") * 9 + structure_total.getInteger("Premiere") * 8 + structure_total.getInteger("Terminale") * 7;
            }
            params.add(structure_total.getString("id_structure")).add(total_licence).add(total_licence);
        }
        query += "ON CONFLICT (id_structure) DO UPDATE " +
                "SET initial_amount = excluded.initial_amount," +
                "amount = excluded.amount";
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
            if(j.getString("u.level") != null) {
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
        if(query.length()>5) {
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
        }else{
            handler.handle(new Either.Right<>(new JsonObject()));
        }

    }

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
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler) );
    }

    @Override
    public void getAmount(String id_structure, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " SELECT \"Seconde\", \"Premiere\", \"Terminale\", \"Seconde\" + \"Premiere\" + \"Terminale\" as total, " +
                "total_april, pro " +
                "FROM " + Crre.crreSchema + ".students " +
                "WHERE id_structure = ?";
        values.add(id_structure);
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler) );
    }

    @Override
    public void reinitAmountLicence(String id_structure, Integer total_licence, Handler<Either<String, JsonObject>> defaultResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".licences " +
                "SET initial_amount =  ?, amount = ? " +
                "WHERE id_structure = ?";
        values.add(total_licence).add(total_licence).add(id_structure);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(defaultResponseHandler));
    }

    @Override
    public void updateAmountLicence(String idStructure, String operation, Integer licences, Handler<Either<String, JsonObject>> handler) {
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".licences " +
                "SET amount = amount " +  operation + " ?  " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(licences)
                .add(idStructure);

        Sql.getInstance().prepared(updateQuery, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getAllStructure(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_structure FROM " + Crre.crreSchema + ".rel_group_structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

}
