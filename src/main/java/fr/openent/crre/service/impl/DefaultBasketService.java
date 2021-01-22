package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.BasketService;
import fr.openent.crre.service.NotificationService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.filter_waiting;
import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;
import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultBasketService extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketService.class);

    private static NotificationService notificationService;
    private static JsonObject mail;

    public DefaultBasketService(String schema, String table, Vertx vertx, JsonObject slackConfiguration,JsonObject mail) {
        super(schema, table);
        DefaultBasketService.mail = mail;
        notificationService = new SlackService(
                vertx,
                slackConfiguration.getString("api-uri"),
                slackConfiguration.getString("token"),
                slackConfiguration.getString("bot-username"),
                slackConfiguration.getString("channel")
        );
    }

    public void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler, UserInfos user){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT basket.id, basket.amount, basket.comment, basket.price_proposal::float , basket.processing_date, basket.id_campaign, basket.id_structure, " +
                "array_to_json(array_agg( e.* )) as equipment," +
                "array_to_json(array_agg(DISTINCT ep.*)) as options, array_to_json(array_agg(DISTINCT basket_file.*)) as files " +
                "FROM " + Crre.crreSchema + ".basket_equipment basket " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_option ON basket_option.id_basket_equipment = basket.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_file ON basket.id = basket_file.id_basket_equipment " +
                "LEFT JOIN (" +
                "SELECT equipment_option.amount, equipment_option.required, equipment_option.id, tax.value as tax_amount, equipment_option.id_equipment, equipment.name, equipment.price " +
                "FROM " + Crre.crreSchema + ".equipment_option " +
                "INNER JOIN " + Crre.crreSchema + ".equipment ON equipment_option.id_option = equipment.id " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = equipment.id_tax " +
                ") ep ON basket_option.id_option = ep.id " +
                "INNER JOIN (" +
                "SELECT equipment.*, tax.value tax_amount " +
                "FROM " + Crre.crreSchema + ".equipment " +
                "INNER JOIN " + Crre.crreSchema + ".tax ON tax.id = equipment.id_tax " +
                ") as e ON e.id = basket.id_equipment " +
                "WHERE basket.id_campaign = ? " +
                "AND basket.id_structure = ? " +
                "AND basket.owner_id = ? " +
                "GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure) " +
                "ORDER BY basket.id DESC;";
        values.add(idCampaign).add(idStructure).add(user.getUserId());

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getBasketOrder(Integer idBasketOrder, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id = ?";
        values.add(idBasketOrder);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getBasketsOrders(Integer idCampaign, Handler<Either<String, JsonArray>> handler, UserInfos user) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id_campaign = ? AND id_structure IN (";
        values.add(idCampaign);

        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";
        query += " ORDER BY basket_order.id DESC;";

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getMyBasketOrders(Handler<Either<String, JsonArray>> handler, UserInfos user){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order b WHERE b.id_user = ? ORDER BY b.id DESC";
        values.add(user.getUserId());

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void getStructureHistoryBaskets(Handler<Either<String, JsonArray>> handler, UserInfos user){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_order WHERE id_structure IN (";
        for (String idStruct : user.getStructures()) {
            query += "?,";
            values.add(idStruct);
        }
        query = query.substring(0, query.length() - 1) + ")";

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void create(final JsonObject basket, UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Crre.crreSchema + ".basket_equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                try {
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getBasketEquipmentCreationStatement(id, basket, user));

                    JsonArray options = basket.getJsonArray("options");
                    int i = 0;
                    while (null != options && i < options.size() ) {
                        statements.add(getBasketEquipmentOptionCreationStatement(id, options.getInteger(i)));
                        i++;
                    }

                    sql.transaction(statements, event1 -> handler.handle(SqlQueryUtils.getTransactionHandler(event1, id)));
                } catch (ClassCastException e) {
                    LOGGER.error("An error occurred when casting tags ids", e);
                    handler.handle(new Either.Left<>(""));
                }
            } else {
                LOGGER.error("An error occurred when selecting next val");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }
    public void delete(final Integer idBasket,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getOptionsBasketDeletion(idBasket))
                .add(getEquipmentBasketDeletion(idBasket));
        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event, idBasket)));
    }
    public void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler ) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET  amount = ? " +
                " WHERE id = ?; ";
        values.add(amount).add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler) );
    }

    public void updateComment(Integer idBasket, String comment, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String commentParameter = "".equals(comment.trim()) ? "null" : "?";
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET comment = " + commentParameter +
                " WHERE id = ?; ";
        if (!"".equals(comment.trim())) {
            values.add(comment);
        }
        values.add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public void updateReassort(Integer idBasket, Boolean reassort, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".basket_equipment " +
                " SET reassort = ? WHERE id = ?; ";

        values.add(reassort);
        values.add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public void updatePriceProposal(Integer id, Double price_proposal, Handler<Either<String, JsonObject>> eitherHandler) {
        JsonArray values;
        String query = "UPDATE " + Crre.crreSchema + ".basket_equipment " +
                "Set price_proposal = " + (price_proposal == null ? " null " : " ? ") +
                "Where id = ?;";
        values = price_proposal == null ? new JsonArray().add(id) : new JsonArray().add(price_proposal).add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(eitherHandler));
    }


    public void listebasketItemForOrder(Integer idCampaign, String idStructure, JsonArray baskets,
                                        Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String basketFilter = baskets.size() > 0 ? "AND basket.id IN " + Sql.listPrepared(baskets.getList()) : "";
        String query = "SELECT  basket.id id_basket, (basket.price_proposal * basket.amount) as price_proposal ,basket.amount, " +
                "basket.comment, basket.processing_date, basket.id_campaign, basket.id_structure, basket.reassort," +
                "e.id id_equipment, e.name,e.summary, e.description, e.price, e.image, e.id_contract, e.status, jsonb(e.technical_specs) technical_specs," +
                "e.tax_amount, nextval('" + Crre.crreSchema + ".order_client_equipment_id_seq' ) as id_order, Case Count(ep) " +
                "when 0 " +
                "then ROUND((e.price + ((e.price *  e.tax_amount) /100)), 2) * basket.amount " +
                "else (ROUND(e.price + ((e.price *  e.tax_amount) /100), 2) + SUM(ep.total_option_price)) * basket.amount " +
                "END as total_price, array_to_json(array_agg(DISTINCT ep.*)) as options, " +
                "array_to_json(array_agg(DISTINCT basket_file.*)) as files, campaign.purse_enabled " +
                "FROM  " + Crre.crreSchema + ".basket_equipment basket " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON (basket.id_campaign = campaign.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_option ON basket_option.id_basket_equipment = basket.id " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_file ON basket.id = basket_file.id_basket_equipment " +
                "LEFT JOIN (" +
                "SELECT equipment_option.*, equipment.name, equipment.price, tax.value tax_amount, " +
                "ROUND(equipment.price + ((equipment.price * tax.value)/100), 2) as total_option_price " +
                "FROM " + Crre.crreSchema + ".equipment_option " +
                "INNER JOIN " + Crre.crreSchema + ".equipment ON equipment_option.id_option = equipment.id " +
                "INNER JOIN  " + Crre.crreSchema + ".tax ON tax.id = equipment.id_tax " +
                ") ep ON basket_option.id_option = ep.id  " +
                "INNER JOIN (" +
                "SELECT equipment.*, tax.value tax_amount " +
                "FROM " + Crre.crreSchema + ".equipment " +
                "INNER JOIN " + Crre.crreSchema + ".tax ON tax.id = equipment.id_tax " +
                "WHERE equipment.status = 'AVAILABLE' " +
                ") as e ON e.id = basket.id_equipment " +
                "WHERE basket.id_campaign = ? " +
                "AND basket.id_structure = ? " + basketFilter +
                "GROUP BY (basket.id, basket.amount, basket.processing_date,basket.id_campaign, basket.id_structure, " +
                "e.id, e.price, e.name, e.summary, e.description, " +
                "e.price, e.image, e.id_contract, e.status, jsonb(e.technical_specs),  e.tax_amount, campaign.purse_enabled);";
        values.add(idCampaign).add(idStructure);

        if (baskets.size() > 0) {
            for (int i = 0; i < baskets.size(); i++) {
                values.add(baskets.getInteger(i));
            }
        }

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void addFileToBasket(Integer basketId, String fileId, String fileName, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".basket_file (id, id_basket_equipment, filename) " +
                "VALUES (?, ?, ?)";
        JsonArray params = new JsonArray()
                .add(fileId)
                .add(basketId)
                .add(fileName);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteFileFromBasket(Integer basketId, String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Crre.crreSchema + ".basket_file WHERE id = ? AND id_basket_equipment = ?";

        JsonArray params = new JsonArray()
                .add(fileId)
                .add(basketId);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                          "FROM " + Crre.crreSchema + ".basket_order bo " +
                          "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment oe ON (bo.id = oe.id_basket) " +
                          "WHERE bo.id_campaign = ? AND bo.id_user = ? ";
        values.add(id_campaign);
        values.add(user.getUserId());
        if (query != "") {
            sqlquery += "AND (bo.name ~* ? OR bo.name_user ~* ? OR oe.equipment_key IN (";
            values.add(query);
            values.add(query);
        } else {
            sqlquery += "AND (bo.name ~* bo.name OR bo.name_user ~* bo.name_user OR oe.equipment_key IN (";
        }

        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getInteger("id"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        sqlquery += ") AND bo.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(0).fieldNames().toString().length() -1);
                String value = filters.getJsonObject(i).getString(key);
                sqlquery += "bo." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery += " ORDER BY bo.id DESC;";

        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void searchWithoutEquip(String query, JsonArray filters, UserInfos user, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE bo.id_campaign = ? AND bo.id_user = ? AND (bo.name ~* ? OR bo.name_user ~* ?) AND oe.id_structure IN (";
        values.add(id_campaign);
        values.add(user.getUserId());
        values.add(query);
        values.add(query);
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(0).fieldNames().toString().length() -1);
                String value = filters.getJsonObject(i).getString(key);
                sqlquery += "bo." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery += " ORDER BY bo.id DESC;";

        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void filter(JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE bo.id_campaign = ? AND bo.id_user = ? ";

        values.add(id_campaign);
        values.add(user.getUserId());
        sqlquery += " AND oe.equipment_key IN (";
        for (int i = 0; i < equipTab.size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonObject(i).getInteger("id"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += " AND oe.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(0).fieldNames().toString().length() -1);
                String value = filters.getJsonObject(i).getString(key);
                sqlquery += "bo." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery += " ORDER BY bo.id DESC;";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void searchWithAll(String query, JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT distinct bo.* " +
                "FROM " + Crre.crreSchema + ".order_client_equipment oe " +
                "LEFT JOIN " + Crre.crreSchema + ".basket_order bo ON (bo.id = oe.id_basket) " +
                "WHERE bo.id_campaign = ? AND bo.id_user = ? ";
        values.add(id_campaign);
        values.add(user.getUserId());
        values.add(query);
        values.add(query);
        if(equipTab.getJsonArray(1).isEmpty()){
            sqlquery += "AND (bo.name ~* ? OR bo.name_user ~* ?) ";
        } else {
            sqlquery += "AND (bo.name ~* ? OR bo.name_user ~* ? OR oe.equipment_key IN (";
            for (int i = 0; i < equipTab.getJsonArray(1).size(); i++) {
                sqlquery += "?,";
                values.add(equipTab.getJsonArray(1).getJsonObject(i).getInteger("id"));
            }
            sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";
            sqlquery += ")";
        }

        sqlquery += " AND oe.equipment_key IN (";
        for (int i = 0; i < equipTab.getJsonArray(0).size(); i++) {
            sqlquery += "?,";
            values.add(equipTab.getJsonArray(0).getJsonObject(i).getInteger("id"));
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        sqlquery += " AND bo.id_structure IN ( ";
        for (String idStruct : user.getStructures()) {
            sqlquery += "?,";
            values.add(idStruct);
        }
        sqlquery = sqlquery.substring(0, sqlquery.length() - 1) + ")";

        if(filters != null && filters.size() > 0) {
            sqlquery += " AND ( ";
            for (int i = 0; i < filters.size(); i++) {
                String key = filters.getJsonObject(i).fieldNames().toString().substring(1, filters.getJsonObject(0).fieldNames().toString().length() -1);
                String value = filters.getJsonObject(i).getString(key);
                sqlquery += "bo." + key + " = " + "?";
                values.add(value);
                if(!(i == filters.size() - 1)) {
                    sqlquery += " OR ";
                } else {
                    sqlquery += ")";
                }
            }
        }
        sqlquery += " ORDER BY bo.id DESC;";
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }

    @Override
    public void updateAllAmount(Integer id, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Crre.crreSchema + ".basket_order bo " +
                " SET amount = " +
                " (SELECT SUM(oce.amount)" +
                " FROM " + Crre.crreSchema + ".basket_order bo" +
                " JOIN " + Crre.crreSchema + ".order_client_equipment oce " +
                " ON (bo.id = oce.id_basket) " +
                " WHERE bo.id = ?) " +
                " WHERE id = ? " +
                " RETURNING bo.*;";
        values.add(id).add(id);
        sql.prepared(query, values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getFile(Integer basketId, String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Crre.crreSchema + ".basket_file WHERE id = ? AND id_basket_equipment = ?";
        JsonArray params = new JsonArray()
                .add(fileId)
                .add(basketId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue().getJsonObject(0)));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }

    public void takeOrder(final HttpServerRequest request, final JsonArray baskets, Integer idCampaign, UserInfos user,
                          String idStructure, final String nameStructure, JsonArray baskets_objects, String nameBasket,
                          final Handler<Either<String, JsonObject>> handler) {
        try {
            JsonArray test = new fr.wseduc.webutils.collections.JsonArray();
            JsonObject basket_data;
            int amount = 0;
            double total = 0;
            for (int i = 0; i < baskets.size(); i++) {
                basket_data = baskets.getJsonObject(i);
                total += basket_data.getInteger("amount") * (Double.parseDouble(basket_data.getString("price")) * (1 + Double.parseDouble(basket_data.getString("tax_amount")) / 100));
                amount += basket_data.getInteger("amount") ;
            }
            test.add(getInsertBasketName(user, idStructure, idCampaign, nameBasket, total, amount));

            sql.transaction(test, event_id -> {
                JsonObject results_id = event_id.body().getJsonArray("results")
                        .getJsonObject(event_id.body().getJsonArray("results").size()-1);
                JsonArray objectResultId = results_id.getJsonArray("results").getJsonArray(0);
                int id_basket = objectResultId.getInteger(0);

                JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                JsonObject basket;
                Boolean purse_enabled = baskets.getJsonObject(0).getBoolean("purse_enabled");
                for (int i = 0; i < baskets.size(); i++) {
                    basket = baskets.getJsonObject(i);
/*                    if (purse_enabled) {
                        statements.add(purseService.updatePurseAmountStatement(Double.valueOf(basket.getString("total_price")),
                                idCampaign, idStructure, "-"));
                    } cela n'est pas au bon endroit il faut le faire quand le valideur valide la commande */
                    statements.add(getInsertEquipmentOrderStatement(basket, user.getUserId(), id_basket));
                    if(! "[null]".equals( basket.getString("options"))) {
                        statements.add(getInsertEquipmentOptionsStatement(basket));
                        statements.add(getDeletionBasketsOptionsStatments(basket.getInteger("id_basket")));
                    }
                    if (!"[null]".equals(basket.getString("files"))) {
                        statements.add(getInsertFilesStatement(basket));
                        statements.add(deleteFilesFromBasket(basket.getInteger("id_basket")));
                    }
                }
                statements.add(getDeletionBasketsEquipmentStatments(idCampaign, idStructure, baskets_objects, purse_enabled, user));
                sql.transaction(statements, event -> {
                    JsonObject results = event.body().getJsonArray("results")
                            .getJsonObject(event.body().getJsonArray("results").size()-1);
                    JsonArray objectResult = results.getJsonArray("results").getJsonArray(0);
                    String jsonValue = objectResult.getString(0) == null ? "{}" : objectResult.getString(0);
                    getTransactionHandler(request, nameStructure, getTotalPriceOfBasketList(baskets),
                            event, new JsonObject(jsonValue), handler);
                });
            });


        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting baskets elements", e);
            handler.handle(new Either.Left<>(""));
        }
    }

    private JsonObject getInsertFilesStatement(JsonObject basket) {
        JsonArray files = new JsonArray(basket.getString("files"));
        StringBuilder filesBuilder = new StringBuilder("INSERT INTO " + Crre.crreSchema + ".order_file(id, id_order_client_equipment, filename) VALUES");
        JsonArray params = new JsonArray();
        JsonObject file;
        for (int i = 0; i < files.size(); i++) {
            file = files.getJsonObject(i);
            filesBuilder.append("(?, ?, ?)");
            params.add(file.getString("id"))
                    .add(basket.getInteger("id_order"))
                    .add(file.getString("filename"));
            filesBuilder.append(i == files.size() - 1 ? "; " : ", ");
        }

        return new JsonObject()
                .put("statement", filesBuilder.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getInsertBasketName(UserInfos user, String idStructure, Integer idCampaign, String basket_name, double total, int amount) {

        String insertBasketNameQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_order(" +
                        "name, id_structure, id_campaign, name_user, id_user, total, amount, created)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) returning id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(basket_name)
                .add(idStructure)
                .add(idCampaign)
                .add(user.getUsername())
                .add(user.getUserId())
                .add(total)
                .add(amount);


        return new JsonObject()
                .put("statement", insertBasketNameQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject deleteFilesFromBasket(Integer basketId) {
        String queryEquipmentOrder = " DELETE FROM " + Crre.crreSchema + ".basket_file " +
                "WHERE id_basket_equipment = ? ;";
        return new JsonObject()
                .put("statement", queryEquipmentOrder)
                .put("values", new JsonArray().add(basketId))
                .put("action", "prepared");
    }

    private JsonObject getOptionsBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Crre.crreSchema + ".basket_option " +
                        " WHERE id_basket_equipment = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idBasket);

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getEquipmentBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Crre.crreSchema + ".basket_equipment " +
                        "WHERE id= ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idBasket);

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Returns a basket equipment insert statement
     *
     * @param id    basket Id
     * @param basket basket Object
     * @return basket equipment relationship transaction statement
     */
    private JsonObject getBasketEquipmentCreationStatement(Number id, JsonObject basket, UserInfos user) {


        String insertBasketEquipmentRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_equipment(" +
                        "id, amount, processing_date, id_equipment, id_campaign, id_structure, owner_id, owner_name)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(basket.getInteger("amount"))
                .add(basket.getString("processing_date"))
                .add(basket.getInteger("equipment"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add(user.getUserId())
                .add(user.getUsername());


        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Returns a basket equipment-option insert statement
     * @param id id of equipment-basket
     * @param option id of options
     * @return basket equipment-option relationship transaction statement
     */
    private JsonObject getBasketEquipmentOptionCreationStatement(Number id, Number option) {
        String insertBasketEquipmentOptionRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".basket_option " +
                        "(id_basket_equipment, id_option) " +
                        " VALUES (?, ?);";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(option);
        return new JsonObject()
                .put("statement", insertBasketEquipmentOptionRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }


    /**
     * Basket to order
     * @param basket
     * @param userId
     * @param id_basket
     * @return
     */
    private JsonObject getInsertEquipmentOrderStatement(JsonObject basket, String userId, int id_basket) {
        StringBuilder queryEquipmentOrder;
        JsonArray params;
        try {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, price, tax_amount, amount,  id_campaign, id_structure, name, summary," +
                            " description, image, technical_spec, status, " +
                            " id_contract, equipment_key, comment, price_proposal, rank, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?, ?, ?, ?, ?, ").append(getTheLastRankOrder()).append(", ?, ?, ?); ");
            params = getObjects(basket);
            params.add(Double.valueOf(basket.getString("price_proposal")))
                    .add(basket.getInteger("id_campaign"))
                    .add(basket.getString("id_structure"))
                    .add(userId)
                    .add(id_basket)
                    .add(basket.getBoolean("reassort"));

        } catch (java.lang.NullPointerException e) {
            queryEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_equipment ")
                    .append(" (id, price, tax_amount, amount,  id_campaign, id_structure, name, summary," +
                            " description, image, technical_spec, status, " +
                            " id_contract, equipment_key, comment, price_proposal, rank, user_id, id_basket, reassort ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?, ?, ?, ?, null, ")
                    .append(getTheLastRankOrder())
                    .append( ", ?, ?, ?); ");
            params = getObjects(basket);
            params.add(basket.getInteger("id_campaign"))
                    .add(basket.getString("id_structure"))
                    .add(userId)
                    .add(id_basket)
                    .add(basket.getBoolean("reassort"));
        }
        return new JsonObject()
                .put("statement", queryEquipmentOrder.toString())
                .put("values", params)
                .put("action", "prepared");

    }

    private JsonArray getObjects(JsonObject basket) {
        JsonArray params;
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(basket.getInteger("id_order"))
                .add(Double.valueOf(basket.getString("price")))
                .add(Double.valueOf(basket.getString("tax_amount")))
                .add(basket.getInteger("amount"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add(basket.getString("name"))
                .add(basket.getString("summary"))
                .add(basket.getString("description"))
                .add(basket.getString("image"))
                .add(basket.getString("technical_specs"))
                .add("WAITING")
                .add(basket.getInteger("id_contract"))
                .add(basket.getInteger("id_equipment"))
                .add(basket.getString("comment"));
        return params;
    }

    private String getTheLastRankOrder () {
        return "(select " +
                "case " +
                "       WHEN max(oce.rank) is null THEN 0 " +
                "       ELSE max(oce.rank + 1) " +
                "END " +
                "from " + Crre.crreSchema + ".order_client_equipment as oce  " +
                "where oce.id_campaign = ? and oce.id_structure = ? )";
    }
    private static JsonObject getInsertEquipmentOptionsStatement (JsonObject basket){
        JsonArray options = new fr.wseduc.webutils.collections.JsonArray(basket.getString("options"))  ;
        StringBuilder queryEOptionEquipmentOrder = new StringBuilder().append(" INSERT INTO ").append(Crre.crreSchema).append(".order_client_options ")
                .append(" ( tax_amount, price, id_order_client_equipment, name, amount, required) VALUES ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (int i=0; i<options.size(); i++){
            queryEOptionEquipmentOrder.append("( ?, ?, ?, ?, ?, ?, ?)");
            queryEOptionEquipmentOrder.append( i == options.size()-1 ? "; " : ", ");
            JsonObject option = options.getJsonObject(i);
            params.add( option.getDouble("tax_amount"))
                    .add(option.getDouble("price"))
                    .add(basket.getInteger("id_order"))
                    .add(option.getString("name"))
                    .add(option.getInteger("amount"))
                    .add(option.getBoolean("required"));
        }
        return new JsonObject()
                .put("statement", queryEOptionEquipmentOrder.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getDeletionBasketsOptionsStatments (Number idBasketEquipment){
        String queryEquipmentOrder = " DELETE FROM " + Crre.crreSchema + ".basket_option " +
                "WHERE id_basket_equipment = ? ;";
        return new JsonObject()
                .put("statement", queryEquipmentOrder)
                .put("values",  new fr.wseduc.webutils.collections.JsonArray().add(idBasketEquipment))
                .put("action", "prepared");
    }

    private static JsonObject getDeletionBasketsEquipmentStatments(Integer idCampaign, String idStructure, JsonArray baskets,
                                                                   Boolean purse_enabled, UserInfos user) {
        String basketFilter = baskets.size() > 0 ? "AND basket_equipment.id IN " + Sql.listPrepared(baskets.getList()) : "";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);
        for (int i = 0; i < baskets.size(); i++) {
            params.add(baskets.getInteger(i));
        }
        params.add(idCampaign).add(idStructure);
        if (purse_enabled) {
            params.add(idCampaign).add(idStructure);
        }
        params.add(user.getUserId());
        String queryEquipmentOrder = " DELETE FROM " + Crre.crreSchema + ".basket_equipment " +
                " WHERE id_campaign = ? AND id_structure = ? " + basketFilter + " RETURNING " +
                getReturningQueryOfTakeOrder(purse_enabled);
        return new JsonObject()
                .put("statement", queryEquipmentOrder)
                .put("values", params )
                .put("action", "prepared");
    }

    private static String getReturningQueryOfTakeOrder(Boolean purse_enabled) {
        if (purse_enabled) {
            return "( SELECT row_to_json(row(p.amount, count(o.id ) )) " +
                    " FROM " + Crre.crreSchema + ".purse p, " + Crre.crreSchema + ".order_client_equipment o " +
                    " where p.id_campaign = ? " +
                    " AND p.id_structure = ? " +
                    " AND  o.id_campaign = ? " +
                    " AND o.id_structure = ? AND o.status != 'VALID' AND o.user_id = ? " +
                    " GROUP BY(p.amount) )";
        } else {
            return "(SELECT row_to_json(row(count(o.id)))\n" +
                    "FROM " + Crre.crreSchema + ".order_client_equipment o " +
                    "WHERE  o.id_campaign = ? " +
                    "  AND o.id_structure = ? " +
                    "  AND o.status != 'VALID'" +
                    "  AND o.user_id = ?)";
        }
    }
    /**
     * Returns the amount of purse from an order transactions.
     *
     * @param event PostgreSQL event
     * @param basicBDObject
     * @return Transaction handler
     */
    private static void getTransactionHandler(HttpServerRequest request, String nameStructure, Double totalPrice,
                                              Message<JsonObject> event, JsonObject basicBDObject,
                                              Handler<Either<String, JsonObject>> handler) {
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("nb_order", basicBDObject.getInteger(basicBDObject.containsKey("f2") ? "f2" : "f1"));
            if (basicBDObject.containsKey("f2")) {
               try{
                   returns.put("amount", basicBDObject.getDouble("f1"));
               }catch (Exception e){
                   returns.put("amount",basicBDObject.getInteger("f1"));
               }
            }
            DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            final double cons = 100.0;
            Number total = Math.round(totalPrice * cons) / cons;
            handler.handle(new Either.Right<>(returns));
            if(mail.containsKey("enableMail")){
                if (!mail.getBoolean("enableMail")) {
                    notificationService.sendMessage(
                            I18n.getInstance().translate("the.structure",
                                    getHost(request), I18n.acceptLanguage(request))
                                    + " " + nameStructure + " " +
                                    I18n.getInstance().translate("crre.slack.order.message1",
                                            getHost(request), I18n.acceptLanguage(request))
                                    + " " + total.toString() + " " +
                                    I18n.getInstance().translate("money.symbol",
                                            getHost(request), I18n.acceptLanguage(request))
                                    + " " +
                                    I18n.getInstance().translate("determiner.male",
                                            getHost(request), I18n.acceptLanguage(request))
                                    + " " + format.format(new Date()) + " ");
                } else {
                    LOGGER.info("Sending mails is enable in conf.json.template");
                }
            } else {
                LOGGER.info("EnableMail doesn't exist in object mail contents conf.json.template");
            }
        } else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<>(""));
        }
    }

    private static Double getTotalPriceOfBasketList(JsonArray baskets) {
        double total = 0;
        for(int i = 0; i < baskets.size(); i++) {
            total += Double.parseDouble((baskets.getJsonObject(i)).getString("total_price"));
        }
        return total;
    }

    public void searchName(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearchName(word, handler);
    }

    @Override
    public void filterGrade(List<String> filter, String query, Handler<Either<String, JsonArray>> handler) {
        if(query == "") {
            filter_waiting(filter, null, handler);
        } else {
            filter_waiting(filter, query, handler);
        }
    }


}
