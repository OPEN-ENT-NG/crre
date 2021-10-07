package fr.openent.crre.service.impl;

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


public class DefaultStatisticsService extends SqlCrudService implements StatisticsService {

    public DefaultStatisticsService(String table) {
        super(table);
    }

    @Override
    public void exportMongo(JsonObject jsonObject, Handler<Either<String, JsonObject>> handler) {
        MongoDb.getInstance().insert("crre.statistics", jsonObject, MongoDbResult.validResultHandler(handler));
    }

    @Override
    public void getFreeLicences(String id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT sum(total_free) as total_free, EXTRACT(YEAR FROM creation_date)::character varying as year " +
                "FROM crre.\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year;";
        Sql.getInstance().prepared(query, new JsonArray().add(id), SqlResult.validResultHandler(handler));

    }

    @Override
    public void getTotalRessources(String id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT sum(equipment_price * amount) as total, EXTRACT(YEAR FROM creation_date)::character varying as year " +
        "FROM crre.\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year;";
        Sql.getInstance().prepared(query, new JsonArray().add(id), SqlResult.validResultHandler(handler));

    }

    @Override
    public void getRessources(String id_structure, String type, Handler<Either<String, JsonArray>> handlerJsonArray) {
        JsonArray params = new JsonArray();
        String query = "SELECT sum(amount) as nb_ressources, EXTRACT(YEAR FROM creation_date)::character varying as year FROM crre.\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? ";
        params.add(id_structure);
        if(type != null) {
            query += "and equipment_format = ? ";
            params.add(type);
        }
        query += "group by year;";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handlerJsonArray));

    }

    @Override
    public void getOrdersByYear(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT count(*) as nb_orders, EXTRACT(YEAR FROM creation_date)::character varying as year FROM crre.\"order-region-equipment-old\" where owner_id != 'renew2021-2022' and id_structure = ? group by year;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getOrdersByCampaign(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT c.name, c.id, count(*) as nb_commande FROM crre.\"order-region-equipment-old\" left join crre.campaign c on (c.id = id_campaign) where owner_id != 'renew2021-2022' and id_structure = ? group by c.id, c.name;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }

    @Override
    public void getOrdersReassort(String id_structure, Handler<Either<String, JsonArray>> handlerJsonArray) {
        String query = "SELECT count(*) as nb_commande FROM crre.\"order-region-equipment-old\" where owner_id != 'renew2021-2022' AND reassort = true and id_structure = ?;";
        Sql.getInstance().prepared(query, new JsonArray().add(id_structure), SqlResult.validResultHandler(handlerJsonArray));
    }
}
