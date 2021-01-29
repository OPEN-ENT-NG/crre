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

    private Neo4j neo4j;
    public DefaultStructureService(String schema){
        super(schema, "");
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL "+
                "RETURN left(s.zipCode, 2) as department, s.id as id, s.name as name,s.city as city,s.UAI as uai, s.academy as academy, s.type as type_etab";
        neo4j.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
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
        String query = "MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles:['Student']}) where s.id IN {ids} RETURN distinct u.level, count(u), s.id;\n";
        neo4j.execute(query, new JsonObject().put("ids", structureIds),
                Neo4jResult.validResultHandler(handler));

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
        String query = "INSERT INTO " + Crre.crreSchema + ".structure_student(id_structure, total) VALUES";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < total.size(); i++) {
            JsonObject structure_total = total.getJsonObject(i);
            query += i < total.size() - 1 ? "(?, ?), " : "(?, ?) ";
            params.add(structure_total.getString("id_structure")).add(structure_total.getInteger("total"));
        }
        query += "ON CONFLICT (id_structure) DO UPDATE " +
                "SET total = excluded.total";
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
            switch (j.getString("u.level")) {
                case "6EME": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"6eme\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "5EME": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"5eme\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "4EME (NC 4E AES)": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"4eme\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "3EME": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"3eme\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "SECONDE GENERALE & TECHNO YC BT": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Seconde\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "PREMIERE GENERALE & TECHNO YC BT": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Premiere\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "TERMINALE GENERALE & TECHNO YC BT": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Terminale\" = ?, \"pro\" = false WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "BAC PRO 3 ANS : 2NDE PRO (OU 1ERE ANNEE)": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Seconde\" = ?, \"pro\" = true WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "BAC PRO 3 ANS : 1ERE PRO (OU 2EME ANNEE)": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Premiere\" = ?, \"pro\" = true WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                case "BAC PRO 3 ANS : TERMINALE PRO (OU 3EME ANNEE)": {
                    query += "UPDATE " + Crre.crreSchema + ".students SET \"Terminale\" = ?, \"pro\" = true WHERE id_structure = ?; ";
                    params.add(count).add(s);
                    break;
                }
                default:
                    break;
            }
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    public void getTotalStructure(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT \"Premiere\" + \"Terminale\" + \"Seconde\" + \"6eme\" + \"5eme\" + \"4eme\" + \"3eme\" as total, id_structure" +
                " FROM " + Crre.crreSchema + ".students";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    public void getAllStructure(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT id_structure FROM " + Crre.crreSchema + ".rel_group_structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

}
