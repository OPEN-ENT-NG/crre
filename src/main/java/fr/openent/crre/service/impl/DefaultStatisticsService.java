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
    public void getFreeLicences(String id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT sum(total_free) as total, reassort, CAST(CASE WHEN EXTRACT(MONTH FROM creation_date) < 5 THEN  (EXTRACT(YEAR FROM creation_date)) ELSE  (EXTRACT(YEAR FROM creation_date) + 1) END as character varying) as year " +
                "FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year, reassort;";
        Sql.getInstance().prepared(query, new JsonArray().add(id), SqlResult.validResultHandler(handler));

    }

    @Override
    public void getTotalRessources(String id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT sum(equipment_price * amount) as total, reassort, CAST(CASE WHEN EXTRACT(MONTH FROM creation_date) < 5 THEN  (EXTRACT(YEAR FROM creation_date)) ELSE  (EXTRACT(YEAR FROM creation_date) + 1) END as character varying) as year " +
                "FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year, reassort;";
        Sql.getInstance().prepared(query, new JsonArray().add(id), SqlResult.validResultHandler(handler));

    }

    @Override
    public void getRessources(String id_structure, String type, Handler<Either<String, JsonArray>> handlerJsonArray) {
        JsonArray params = new JsonArray();
        String query = "SELECT sum(amount) as total, reassort, CAST(CASE WHEN EXTRACT(MONTH FROM creation_date) < 5 THEN  (EXTRACT(YEAR FROM creation_date)) ELSE  (EXTRACT(YEAR FROM creation_date) + 1) END as character varying) as year FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? ";
        params.add(id_structure);
        if (type != null) {
            query += "and equipment_format = ? ";
            params.add(type);
        }
        query += "group by year, reassort;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handlerJsonArray));

    }

    @Override
    public void getOrdersByYear(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT count(*) as total, reassort, CAST(CASE WHEN EXTRACT(MONTH FROM creation_date) < 5 THEN  (EXTRACT(YEAR FROM creation_date)) ELSE  (EXTRACT(YEAR FROM creation_date) + 1) END as character varying) as year FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year, reassort;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getOrdersByCampaign(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT c.name, c.id, count(*) as total FROM " + Crre.crreSchema + ".\"order-region-equipment-old\" left join " + Crre.crreSchema + ".campaign c on (c.id = id_campaign) where owner_id != 'renew2021-2022' and id_structure = ? group by c.id, c.name;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getLicences(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT amount, initial_amount FROM " + Crre.crreSchema + ".licences where id_structure = ?;";
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
}
