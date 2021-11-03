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
        if (type.equals("orders")) {
            query += "SELECT count(*)::integer AS total, ";
        }
        if (type.equals("ressources")) {
            query += "SELECT sum(equipment_price * amount)::double precision AS total, ";
        }
        if (type.equals("free")) {
            query += "SELECT sum(total_free)::integer AS total, ";
        }
        if (type.equals("articlenumerique") || type.equals("articlepapier")) {
            query += "SELECT sum(amount)::integer AS total, ";
        }
        if (type.equals("all_ressources")) {
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

        if (type.equals("articlenumerique") || type.equals("articlepapier")) {
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
    public void getOrdersCompute(String type, HashMap<String, ArrayList<String>> params, boolean publicField,
                                 boolean isReassort, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(ordersMongo(type, params, publicField, isReassort).toString(),
                MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getLicencesCompute(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(licencesMongo(params).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getStructureCompute(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder,
                                    boolean isReassort, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(structuresMongo(params, MoreOneOrder, isReassort).toString(),
                MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getAllYears(Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(yearsMongo().toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void getStatsByStructure(HashMap<String, ArrayList<String>> params, Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().command(statsByStructureMongo(params).toString(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    @Override
    public void deleteStatsDay(Handler<Either<String, JsonObject>> handlerJsonObject) {
        MongoDb.getInstance().delete("crre.statistics", deleteStatsDay(), MongoDbResult.validResultHandler(handlerJsonObject));
    }

    private JsonObject filterMatch(HashMap<String, ArrayList<String>> params) {
        JsonObject match = new JsonObject()
                .put("date", LocalDate.now().toString());

        Set<Map.Entry<String, ArrayList<String>>> set = params.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            if (!me.getKey().equals("year") && !me.getKey().equals("reassort") &&
                    !me.getKey().equals("orientation") && !me.getKey().equals("query")) {
                JsonObject parameter = new JsonObject().put("$in", me.getValue());
                match.put(me.getKey(), parameter);
            } else if (me.getKey().equals("orientation")) {
                if (me.getValue().get(0).equals("LG") && me.getValue().size() == 1) {
                    match.put("general", true);
                } else if (me.getValue().get(0).equals("LP") && me.getValue().size() == 1) {
                    match.put("technical", true);
                } else {
                    match.put("technical", true);
                    match.put("general", true);
                }
            }
        }

        return new JsonObject()
                .put("$match", match);
    }

    private JsonObject filterReassort(String field, HashMap<String, ArrayList<String>> params) {
        JsonObject match = new JsonObject()
                .put(field + ".reassort", Boolean.parseBoolean(params.get("reassort").get(0)));
        return new JsonObject()
                .put("$match", match);
    }

    private JsonObject searchMatch(String query) {
        JsonObject match = new JsonObject().put("$or",
                Arrays.asList(
                        new JsonObject().put("uai", new JsonObject().put("$regex", query).put("$options", "i")),
                        new JsonObject().put("name", new JsonObject().put("$regex", query).put("$options", "i"))
                ));
        return new JsonObject()
                .put("$match", match);
    }



    private JsonObject ordersMongo(String type, HashMap<String, ArrayList<String>> params, boolean publicField, boolean isReassort) {
        String field = "$stats." + type;
        JsonObject id = new JsonObject().put("year", field + ".year");
        JsonObject project;
        if (publicField) {
            id.put("public", "$public");
            project = new JsonObject().put("$project", new JsonObject().put("_id", 0).put("total", "$total").put("public", "$_id.public"));
        } else {
            project = new JsonObject().put("$project", new JsonObject().put("_id", 0));
        }
        ArrayList<JsonObject> listParameter = new ArrayList<>(Arrays.asList(
                new JsonObject()
                        .put("$unwind", field),
                filterMatch(params)));
        if (isReassort) {
            listParameter.addAll(Arrays.asList(filterReassort(field.substring(1), params)));
        }
        listParameter.addAll(Arrays.asList(
                new JsonObject()
                        .put("$group", new JsonObject()
                                .put("_id", id)
                                .put("total", new JsonObject().put("$sum", field + ".total"))
                        ),
                new JsonObject()
                        .put("$match", new JsonObject()
                                .put("_id.year", params.get("year").get(0))
                        ),
                project));
        JsonArray pipeline = new JsonArray();
        return new JsonObject()
                .put("aggregate", "crre.statistics")
                .put("allowDiskUse", true)
                .put("cursor",
                        new JsonObject().put("batchSize", 2147483647)
                )
                .put("pipeline", listParameter);
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

    private JsonObject structuresMongo(HashMap<String, ArrayList<String>> params, boolean MoreOneOrder, boolean isReassort) {
        JsonObject match = new JsonObject();
        JsonObject unwind = new JsonObject().put("path", "$stats.order_year");

        if (isReassort) {
            match.put("stats.order_year.reassort", Boolean.parseBoolean(params.get("reassort").get(0)));
        }
        if (MoreOneOrder) {
            match.put("stats.order_year.year", params.get("year").get(0))
                    .put("stats.order_year.total", new JsonObject().put("$gt", 0));
        } else {
            unwind.put("preserveNullAndEmptyArrays", true);
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
                                                .put("$unwind", unwind),
                                        new JsonObject().put("$match", filterMatch(params).getJsonObject("$match").mergeIn(match)),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", new JsonObject()
                                                                .put("id", "$public")
                                                                .put("id_structure", "$_id"))
                                                ),
                                        new JsonObject()
                                                .put("$group", new JsonObject()
                                                        .put("_id", "$_id.id")
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
                                        new JsonObject().put("$match", new JsonObject()
                                                .put("name", new JsonObject("{\"$ne\": null}"))),
                                        new JsonObject().put("$sort", new JsonObject()
                                                .put("name", -1))
                                )
                        )
                );
    }

    private JsonObject statsByStructureMongo(HashMap<String, ArrayList<String>> params) {
        String query = params.containsKey("query") ? params.get("query").get(0) : "";
        JsonObject cond = new JsonObject();
        cond.put("if", new JsonObject().put("$eq", new JsonArray().add("$_id.licences.exist").add(false)));
        cond.put("then", new JsonObject().put("amount", 0).put("initial_amount", 0).put("year", params.get("year").get(0)).put("percentage", 0));
        cond.put("else", "$_id.licences");
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
                                                .put("$unwind", "$stats.all_ressources"),
                                        searchMatch(query),
                                        filterMatch(params),
                                        new JsonObject()
                                                .put("$match", new JsonObject()
                                                        .put("$expr", new JsonObject()
                                                                .put("$and", Arrays.asList(
                                                                        new JsonObject().put("$or",
                                                                                Arrays.asList(
                                                                                        new JsonObject().put("$eq",
                                                                                                new JsonArray().add("$stats.all_ressources.exist").add(false)
                                                                                        ),
                                                                                        new JsonObject().put("$and",
                                                                                                new JsonArray().add(new JsonObject().put("$eq",
                                                                                                        new JsonArray().add("$stats.all_ressources.year").add(params.get("year").get(0)))
                                                                                                )
                                                                                        )
                                                                                )

                                                                        ),
                                                                        new JsonObject().put("$or",
                                                                                Arrays.asList(
                                                                                        new JsonObject().put("$eq",
                                                                                                new JsonArray().add("$stats.order_year.exist").add(false)
                                                                                        ),
                                                                                        new JsonObject().put("$and",
                                                                                                new JsonArray().add(new JsonObject().put("$eq",
                                                                                                        new JsonArray().add("$stats.order_year.year").add(params.get("year").get(0)))
                                                                                                )
                                                                                        )
                                                                                )

                                                                        ),
                                                                        new JsonObject().put("$or",
                                                                                Arrays.asList(
                                                                                        new JsonObject().put("$eq",
                                                                                                new JsonArray().add("$stats.licences.exist").add(false)
                                                                                        ),
                                                                                        new JsonObject().put("$and",
                                                                                                new JsonArray().add(new JsonObject().put("$eq",
                                                                                                        new JsonArray().add("$stats.licences.year").add(params.get("year").get(0)))
                                                                                                )
                                                                                        )
                                                                                )

                                                                        ))
                                                                )
                                                        )
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
                                                                .put("ressources", "$stats.all_ressources.total")
                                                        )
                                                ),
                                        new JsonObject()
                                                .put("$group", new JsonObject().put("_id", new JsonObject()
                                                                        .put("id_structure", "$_id.id_structure")
                                                                        .put("uai", "$_id.uai")
                                                                        .put("name", "$_id.name")
                                                                        .put("catalog", "$_id.catalog")
                                                                        .put("public", "$_id.public")
                                                                        .put("licences", "$_id.licences")
                                                                )
                                                                .put("orders", new JsonObject().put("$sum", "$_id.orders"))
                                                                .put("ressources", new JsonObject().put("$sum", "$_id.ressources"))
                                                ),
                                        new JsonObject()
                                                .put("$project", new JsonObject()
                                                        .put("_id", 0)
                                                        .put("name", "$_id.name")
                                                        .put("uai", "$_id.uai")
                                                        .put("catalog", "$_id.catalog")
                                                        .put("public", "$_id.public")
                                                        .put("id_structure", "$_id.id_structure")
                                                        .put("licences", new JsonObject().put("$cond", cond))
                                                        .put("orders", "$orders")
                                                        .put("ressources", "$ressources")
                                                )
                                )
                        ));
    }

    private JsonObject deleteStatsDay() {
        return new JsonObject()
                .put("date", LocalDate.now().toString());
    }
}
