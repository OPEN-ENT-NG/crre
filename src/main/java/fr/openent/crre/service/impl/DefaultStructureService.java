package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.neo4j.StatementsBuilder;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.neo4j.Neo4jResult.validEmptyHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class DefaultStructureService extends SqlCrudService implements StructureService {

    private final Neo4j neo4j;
    private final EventBus eb;

    private static final String SECONDE = "seconde";
    private static final String PREMIERE = "premiere";
    private static final String TERMINALE = "terminale";
    private static final String SECONDEPRO = "secondepro";
    private static final String PREMIEREPRO = "premierepro";
    private static final String TERMINALEPRO = "terminalepro";
    private static final String BMA1 = "bma1";
    private static final String BMA2 = "bma2";
    private static final String CAP1 = "cap1";
    private static final String CAP2 = "cap2";
    private static final String CAP3 = "cap3";
    private static final String PRO = "pro";
    private static final String GENERAL = "general";
    private static final String ID_STRUCTURE = "id_structure";
    private static final String RELIQUAT = "reliquat";

    public DefaultStructureService(String schema, EventBus eb) {
        super(schema, "");
        this.eb = eb;
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT structure.id_structure AS id, name, city, uai, " +
                "CASE WHEN rel_group_structure.id_structure is null THEN false ELSE true END as inRegroupment " +
                "FROM " + Crre.crreSchema + ".structure " +
                "LEFT OUTER JOIN " + Crre.crreSchema + ".rel_group_structure ON rel_group_structure.id_structure = structure.id_structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresWithoutRight(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (ss:Structure)<-[:BELONGS*0..1]-()<-[:DEPENDS]-(g:Group) WHERE g.name =~ '.*CRRE.*' and ss.UAI is not null " +
                "WITH collect(distinct(ss.id)) as deployedId " +
                "MATCH (s:Structure) WHERE NOT s.id IN deployedId and s.UAI is not null " +
                "RETURN left(s.zipCode, 2) as department, s.id as id, s.name as name, s.city as city, s.UAI as uai, " +
                "s.academy as academy, s.type as type_etab;";
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
                        "s.zipCode  as zipCode, s.city as city, s.type as type, sum(nbr_students)*2 AS minimum_licences_consumables";
                params.put("consumable_formations", new JsonArray(consumable_formations));
            }
            query += " ORDER BY s.name;";
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
                "where s.id IN {ids} RETURN distinct u.sector, count(u), s.id;";
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
        String query = "INSERT INTO " + Crre.crreSchema + ".students (id_structure) VALUES ";
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
            int total_licence = 0;
            int total_licence_consumable = 0;
            // Calcul tout type de licence si les étudiants sont pro et général
            if (structure_total.getBoolean(PRO) && structure_total.getBoolean(GENERAL)) {
                total_licence = structure_total.getInteger(SECONDE) * 9 +
                        structure_total.getInteger(PREMIERE) * 8 + structure_total.getInteger(TERMINALE) * 7 +
                        ((structure_total.getInteger(SECONDEPRO) + structure_total.getInteger(PREMIEREPRO) +
                                structure_total.getInteger(TERMINALEPRO) + structure_total.getInteger(CAP1) +
                                structure_total.getInteger(CAP2) + structure_total.getInteger(CAP3) +
                                structure_total.getInteger(BMA1) + structure_total.getInteger(BMA2)) * 3);
                // Calcul les licences si les étudiants sont general
            } else if (!structure_total.getBoolean(PRO) && structure_total.getBoolean(GENERAL)) {
                total_licence = structure_total.getInteger(SECONDE) * 9 +
                        structure_total.getInteger(PREMIERE) * 8 + structure_total.getInteger(TERMINALE) * 7;
                // Calcul les licences si les étudiants sont pro
            } else if (structure_total.getBoolean(PRO) && structure_total.getBoolean(GENERAL)) {
                total_licence = (structure_total.getInteger(SECONDEPRO) +
                        structure_total.getInteger(PREMIEREPRO) + structure_total.getInteger(TERMINALEPRO) +
                        structure_total.getInteger(CAP1) + structure_total.getInteger(CAP2) + structure_total.getInteger(CAP3) +
                        structure_total.getInteger(BMA1) + structure_total.getInteger(BMA2)) * 3;
            }
            if (consumableFormationsStudents.containsKey(structure_total.getString(ID_STRUCTURE))) {
                total_licence_consumable = consumableFormationsStudents.getInteger(structure_total.getString(ID_STRUCTURE)) * 2;
            }
            params.add(structure_total.getString(ID_STRUCTURE))
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
            if (j.getString("u.sector") != null) {
                switch (j.getString("u.sector")) {
                    case "SECONDE GENERALE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET seconde = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "1ERE GENERALE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET premiere = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "TERMINALE GENERALE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET terminale = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "SECONDE TECHNOLOGIQUE SPECIFIQUE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET secondetechno = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "1ERE  TECHNOLOGIQUE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET premieretechno = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "TERMINALE TECHNOLOGIQUE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET terminaletechno = ?, total_april = total_april + ?, " +
                                "general = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : 2NDE PRO (OU 1ERE ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET secondepro = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : 1ERE PRO (OU 2EME ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET premierepro = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BAC PRO 3 ANS : TERM PRO (OU 3EME ANNEE)": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET terminalepro = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "CAP EN 2 ANS : 1ERE ANNEE":
                    case "CAP EN 1 AN":
                    case "CAP EN 3 ANS : 1ERE ANNEE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET cap1 = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "CAP EN 2 ANS : 2EME ANNEE":
                    case "CAP EN 3 ANS : 2EME ANNEE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET cap2 = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "CAP EN 3 ANS : 3EME ANNEE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET cap3 = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BMA EN 2 ANS : 1ERE ANNEE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET bma1 = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
                        params.add(count).add(count).add(s);
                        break;
                    }
                    case "BMA EN 2 ANS : 2EME ANNEE": {
                        query += "UPDATE " + Crre.crreSchema + ".students SET bma2 = ?, total_april = total_april + ?, " +
                                "pro = true WHERE id_structure = ?; ";
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
        String query = "SELECT distinct(s.id_structure), premiere, terminale, seconde, premierepro, terminalepro, secondepro," +
                "cap1, cap2, cap3, bma1, bma2, s.pro, s.general" +
                " FROM " + Crre.crreSchema + ".students s" +
                " LEFT JOIN " + Crre.crreSchema + ".structure st ON (s.id_structure = st.id_structure)" +
                " WHERE st.catalog = 'Numerique' OR (st.mixte AND pro)";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateAmount(String id_structure, JsonObject students, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".students " +
                " SET seconde = ?, premiere = ?, terminale = ?, " +
                " secondepro = ?, premierepro = ?, terminalepro = ?, " +
                " bma1 = ?, bma2 = ?, cap1 = ?, cap2 = ?, cap3 = ?" +
                "WHERE id_structure = ?";
        values.add(students.getInteger(SECONDE)).add(students.getInteger(PREMIERE)).add(students.getInteger(TERMINALE))
                .add(students.getInteger(SECONDEPRO)).add(students.getInteger(PREMIEREPRO)).add(students.getInteger(TERMINALEPRO))
                .add(students.getInteger(BMA1)).add(students.getInteger(BMA2)).add(students.getInteger(CAP1))
                .add(students.getInteger(CAP2)).add(students.getInteger(CAP3)).add(id_structure);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getAmount(String id_structure, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT seconde, premiere, terminale, premierepro, terminalepro, secondepro, secondetechno, premieretechno, terminaletechno, cap1, cap2, cap3, bma1, bma2, " +
                "seconde + premiere + terminale + secondetechno + premieretechno + terminaletechno + premierepro + terminalepro + secondepro + cap1 + cap2 + cap3 + bma1 + bma2 as total, " +
                "total_april, pro, general " +
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
        String query = "SELECT DISTINCT id_structure FROM " + Crre.crreSchema + ".structure";
        sql.raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAllStructureByIds(List<String> ids, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT id_structure, s.catalog, s.mixte " +
                "FROM " + Crre.crreSchema + ".structure s " +
                "WHERE s.id_structure IN " + Sql.listPrepared(ids.toArray());
        for (String id : ids) {
            params.add(id);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
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
            query += "INSERT INTO " + Crre.crreSchema + ".licences (id_structure, amount, initial_amount) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (id_structure) DO UPDATE " +
                    "SET amount = licences.amount + ?, " +
                    "initial_amount = (" +
                    "  CASE " +
                    "    WHEN licences.initial_amount = 0 THEN ? " +
                    "    ELSE licences.initial_amount" +
                    "  END" +
                    "); ";
            JsonObject structure = structures.getJsonObject(i);
            params.add(structure.getString("id"))
                    .add(Integer.parseInt(structure.getString(RELIQUAT)))
                    .add(Integer.parseInt(structure.getString(RELIQUAT)))
                    .add(Integer.parseInt(structure.getString(RELIQUAT)))
                    .add(Integer.parseInt(structure.getString(RELIQUAT)));
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

    @Override
    public void linkRolesToGroup(String groupId, JsonArray roleIds, Handler<Either<String, JsonObject>> handler) {
        JsonObject params = new JsonObject();
        params.put("groupId", groupId);
        if (groupId != null && !groupId.trim().isEmpty()) {
            String deleteQuery =
                    "MATCH (m:Group)-[r:AUTHORIZED]-(:Role) " +
                            "WHERE m.id = {groupId} " +
                            "DELETE r";
            if (roleIds == null || roleIds.size() == 0) {
                neo4j.execute(deleteQuery, params, validEmptyHandler(handler));
            } else {
                StatementsBuilder s = new StatementsBuilder().add(deleteQuery, params);
                String createQuery =
                        "MATCH (n:Role), (m:Group) " +
                                "WHERE m.id = {groupId} AND n.id IN {roles} " +
                                "CREATE UNIQUE m-[:AUTHORIZED]->n";
                s.add(createQuery, params.copy().put("roles", roleIds));
                neo4j.executeTransaction(s.build(), null, true, validEmptyHandler(handler));
            }
        } else {
            handler.handle(new Either.Left<>("invalid.arguments"));
        }
    }

    @Override
    public void createOrUpdateManual(JsonObject group, String structureId, String classId,
                                     Handler<Either<String, JsonObject>> result) {
        JsonObject action = new JsonObject()
                .put("action", "manual-create-group")
                .put("structureId", structureId)
                .put("classId", classId)
                .put("group", group);

        eb.send("entcore.feeder", action, new DeliveryOptions().setSendTimeout(600000L),
                handlerToAsyncHandler(validUniqueResultHandler(0, result)));
    }

    @Override
    public void getRole(String roleName, Handler<Either<String, JsonObject>> handler) {
        JsonObject params = new JsonObject().put("linkName", "CRRE").put("roleName", roleName);
        String queryRole = "MATCH (a:Application)-[]->(ac:Action)<-[]-(r:Role)" +
                " WHERE a.name = {linkName} and toLower(r.name) CONTAINS toLower({roleName}) RETURN distinct(r.id) as id";
        neo4j.execute(queryRole, params, Neo4jResult.validUniqueResultHandler(handler));
    }

    public void linkRoleGroup(String groupId, String roleId, Handler<Either<String, JsonObject>> handler) {
        String queryLink = "MATCH (r:Role), (g:Group) " +
                "WHERE r.id = {roleId} and g.id = {groupId} " +
                "CREATE UNIQUE (g)-[:AUTHORIZED]->(r)";
        JsonObject params = new JsonObject()
                .put("groupId", groupId)
                .put("roleId", roleId);
        neo4j.execute(queryLink, params, Neo4jResult.validUniqueResultHandler(handler));
    }
}
