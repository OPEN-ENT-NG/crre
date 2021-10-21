package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.StatisticsService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.time.LocalDate;
import java.util.*;


public class DefaultStatisticsService extends SqlCrudService implements StatisticsService {

    public DefaultStatisticsService(String table) {
        super(table);
    }

    @Override
    public void exportMongo(JsonObject jsonObject, Handler<Either<String, JsonObject>> handler) {
        MongoDb.getInstance().insert("" + Crre.crreSchema + ".statistics", jsonObject, MongoDbResult.validResultHandler(handler));
    }

    @Override
    public void getStats(String type, String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "";
        JsonArray params = new JsonArray().add(id_structure);
        if(type.equals("orders")) {
            query += "SELECT count(*)::integer AS total, ";
        }
        if(type.equals("ressources")) {
            query += "SELECT sum(equipment_price * amount)::double precision AS total, ";
        }
        if(type.equals("free")) {
            query += "SELECT sum(total_free)::integer AS total, ";
        }
        if(type.equals("articlenumerique") || type.equals("articlepapier")) {
            query += "SELECT sum(amount)::integer AS total, ";
        }
        if(type.equals("all_ressources")) {
            query += "SELECT sum(amount)::integer AS total, ";
        }
        query += "reassort, " +
                "CAST(" +
                "   CASE " +
                "       WHEN EXTRACT(MONTH FROM creation_date) < 5 " +
                "           THEN (EXTRACT(YEAR FROM creation_date)) " +
                "           ELSE  (EXTRACT(YEAR FROM creation_date) + 1) " +
                "       END AS character varying" +
                ") AS year " +
                "FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" " +
                "WHERE owner_id != 'renew2021-2022' AND id_structure = ? ";

        if(type.equals("articlenumerique") || type.equals("articlepapier")) {
            query += "AND equipment_format = ? ";
            params.add(type);
        }
        query += "GROUP BY year, reassort;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getOrdersByCampaign(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT c.name, c.id, count(*)::integer AS total " +
                "FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" " +
                "LEFT JOIN " + Crre.crreSchema + ".campaign c ON (c.id = id_campaign) " +
                "WHERE owner_id != 'renew2021-2022' AND id_structure = ? " +
                "GROUP BY c.id, c.name;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getLicences(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT amount, initial_amount " +
                "FROM " + Crre.crreSchema + ".licences " +
                "WHERE id_structure = ?;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getOrdersCompute(String type, HashMap<String, ArrayList<String>> params, boolean publicField, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(ordersMongo(type, params, publicField).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getLicencesCompute(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(licencesMongo(params).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getStructureCompute(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(structuresMongo(params, MoreOneOrder).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getAllYears(Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(yearsMongo().toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getStatsByStructure(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(statsByStructureMongo(params).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    private JsonObject filterMatch(HashMap<String, ArrayList<String>> params) {
        JsonObject match = new JsonObject()
                .put("date", LocalDate.now().minusDays(1).toString());

        Set<Map.Entry<String, ArrayList<String>>> set = params.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            if (!me.getKey().equals("year") && !me.getKey().equals("reassort")) {
                JsonObject parameter = new JsonObject().put("$in", me.getValue());
                match.put(me.getKey(), parameter);
            }
        }

        JsonObject matchRequest = new JsonObject()
                .put("$match", match);
        return matchRequest;
    }

    private JsonObject filterReassort(String field, HashMap<String, ArrayList<String>> params) {
        JsonObject match = new JsonObject()
                .put(field + ".reassort", Boolean.parseBoolean(params.get("reassort").get(0)));
        JsonObject matchRequest = new JsonObject()
                .put("$match", match);
        return matchRequest;
    }

    private JsonObject ordersMongo(String type, HashMap<String, ArrayList<String>> params, boolean publicField) {
        String field = "$stats." + type;
        JsonObject id = new JsonObject().put("year", field + ".year");
        JsonObject project = null;
        if (publicField) {
            id.put("public", "$public");
            project = new JsonObject().put("$project", new JsonObject().put("_id", 0).put("total", "$total").put("public", "$_id.public"));
        } else {
            project = new JsonObject().put("$project", new JsonObject().put("_id", 0));
        }
        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$unwind", field),
                                        filterMatch(params),
                                        filterReassort(field.substring(1), params),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", id)
                                                        .put("total", new JsonObject().put("$sum", field + ".total"))
                                                ),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("_id.year", params.get("year").get(0))
                                                ),
                                        project
                                )
                        )
                );
    }

    private JsonObject licencesMongo(HashMap<String, ArrayList<String>> params) {
        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$unwind", "$stats.licences"),
                                        filterMatch(params),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("stats.licences.year", params.get("year").get(0))
                                                ),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject().put("year", "$stats.licences.year"))
                                                        .put("amount", new JsonObject().put("$sum", "$stats.licences.amount"))
                                                        .put("initial_amount", new JsonObject().put("$sum", "$stats.licences.initial_amount"))
                                                ),
                                        new JsonObject().put("$project", new JsonObject()
                                                .put("_id", 0)
                                        )

                                )
                        )
                );
    }

    private JsonObject structuresMongo(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder) {
        JsonObject match = new JsonObject()
                .put("stats.order_year.year", params.get("year").get(0))
                .put("stats.order_year.reassort", Boolean.parseBoolean(params.get("reassort").get(0)));

        if (MoreOneOrder) {
            match.put("stats.order_year.total", new JsonObject().put("$gt", 0));
        }

        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$unwind", "$stats.order_year"),
                                        filterMatch(params),
                                        new JsonObject()
                                                .put("$match", match),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", "$public")
                                                        .put("total", new JsonObject().put("$sum", 1))
                                                ),
                                        new JsonObject().put("$project", new JsonObject()
                                                .put("_id", 0)
                                                .put("public", "$_id")
                                                .put("total", 1))


                                )
                        )
                );
    }

    private JsonObject yearsMongo() {
        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$unwind", "$stats.order_year"),
                                        new JsonObject()
                                                .put("$group", new JsonObject().put("_id", new JsonObject().put("year", "$stats.order_year.year"))
                                                ),
                                        new JsonObject().put("$project", new JsonObject()
                                                .put("_id", 0)
                                                .put("name", "$_id.year")),
                                        new JsonObject().put("$sort", new JsonObject()
                                                .put("name", -1))
                                )
                        )
                );
    }

    private JsonObject statsByStructureMongo(HashMap<String, ArrayList<String>> params) {
        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline",
                        new JsonArray(
                                Arrays.asList(
                                        new JsonObject()
                                                .put("$unwind", "$stats.order_year"),
                                        new JsonObject()
                                                .put("$unwind", "$stats.licences"),
                                        new JsonObject()
                                                .put("$unwind", "$stats.numeric_ressources"),
                                        new JsonObject()
                                                .put("$unwind", "$stats.paper_ressources"),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("stats.licences.year", params.get("year").get(0))
                                                        .put("stats.order_year.year", params.get("year").get(0))
                                                        .put("stats.numeric_ressources.year", params.get("year").get(0))
                                                        .put("stats.paper_ressources.year", params.get("year").get(0))
                                                        .put("stats.order_year.reassort", Boolean.parseBoolean(params.get("reassort").get(0)))
                                                        .put("stats.numeric_ressources.reassort", Boolean.parseBoolean(params.get("reassort").get(0)))
                                                        .put("stats.paper_ressources.reassort", Boolean.parseBoolean(params.get("reassort").get(0)))
                                                ),
                                        new JsonObject()
                                                .put("$group", new JsonObject().put("_id", new JsonObject()
                                                                .put("id_structure", "$id_structure")
                                                                .put("uai", "$uai")
                                                                .put("name", "$name")
                                                                .put("catalog", "$catalog")
                                                                .put("public", "$public")
                                                                .put("licences", "$stats.licences")
                                                                .put("orders", "$stats.order_year.total")
                                                                .put("ressources", new JsonObject().put("$add", new JsonArray().add("$stats.numeric_ressources.total").add("$stats.paper_ressources.total")))
                                                        )
                                                ),
                                        new JsonObject()
                                                .put("$project", new JsonObject()
                                                        .put("_id", 0)
                                                        .put("name", "$_id.name")
                                                        .put("uai", "$_id.uai")
                                                        .put("catalog", "$_id.catalog")
                                                        .put("public", "$_id.public")
                                                        .put("id_structure", "$_id.id")
                                                        .put("licences", "$_id.licences")
                                                        .put("orders", "$_id.orders")
                                                        .put("ressources", "$_id.ressources")
                                                )
                                )
                        ));
    }
}
