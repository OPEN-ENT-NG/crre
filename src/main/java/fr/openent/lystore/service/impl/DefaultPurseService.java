package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ImportCSVHelper;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultPurseService implements PurseService {
    private Boolean invalidDatas= false;
    private static final Logger log = LoggerFactory.getLogger(PurseService.class);

    @Override
    public void launchImport(Integer campaignId, JsonObject statementsValues,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        invalidDatas = false;
        for (String field : fields) {
            statements.add(getImportStatement(campaignId, field,
                    statementsValues.getString(field)));

        }
        if(invalidDatas){
            handler.handle(new Either.Left<String, JsonObject>
                    ("lystore.invalid.data.to.insert"));
        }else  if (statements.size() > 0) {
            Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    if (message.body().containsKey("status") &&
                            "ok".equals(message.body().getString("status"))) {
                        handler.handle(new Either.Right<String, JsonObject>(
                                new JsonObject().put("status", "ok")));
                    } else {
                        handler.handle(new Either.Left<String, JsonObject>
                                ("lystore.statements.error"));
                    }
                }
            });
        } else {
            handler.handle(new Either.Left<String, JsonObject>
                    ("lystore.statements.empty"));
        }
    }

    @Override
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".purse" +
                " WHERE id_campaign = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject getImportStatement(Integer campaignId, String structureId, String amount) {
        String statement = "INSERT INTO " + Lystore.lystoreSchema + ".purse(id_structure, amount, id_campaign, initial_amount) " +
                "VALUES (?, ?, ?,?) " +
                "ON CONFLICT (id_structure, id_campaign) DO UPDATE " +
                "SET amount = ?, " +
                " initial_amount = ? " +
                "WHERE purse.id_structure = ? " +
                "AND purse.id_campaign = ?;";
        JsonArray params =  new fr.wseduc.webutils.collections.JsonArray();
        try {
            params.add(structureId)
                    .add(Double.parseDouble(amount))
                    .add(campaignId)
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(structureId)
                    .add(campaignId);

        }catch (NumberFormatException e){
            invalidDatas = true;
        }
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void update(Integer id, JsonObject purse, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE   " +
                "  " + Lystore.lystoreSchema + ".purse   " +
                "SET   " +
                "  amount = (  " +
                "    SELECT   " +
                "      CASE WHEN SUM(total) IS NULL THEN ? ELSE ? - SUM(total) END   " +
                "    FROM   " +
                "      (  " +
                "        SELECT   " +
                "          Round(  " +
                "            (  " +
                "              (  " +
                "                SELECT   " +
                "                  CASE WHEN orders.price_proposal IS NOT NULL THEN 0 WHEN orders.override_region IS NULL THEN 0 WHEN Sum(  " +
                "                    oco.price + (  " +
                "                      (oco.price * oco.tax_amount) / 100  " +
                "                    ) * oco.amount  " +
                "                  ) IS NULL THEN 0 ELSE Sum(  " +
                "                    oco.price + (  " +
                "                      (oco.price * oco.tax_amount) / 100  " +
                "                    ) * oco.amount  " +
                "                  ) END   " +
                "                FROM   " +
                "                  " + Lystore.lystoreSchema + ".order_client_options oco   " +
                "                WHERE   " +
                "                  oco.id_order_client_equipment = orders.id  " +
                "              ) + orders.\"price TTC\"  " +
                "            ) * orders.amount,   " +
                "            2  " +
                "          ) AS total   " +
                "        FROM   " +
                "          (  " +
                "            (  " +
                "              select   " +
                "                ore.id,   " +
                "                ore.price as \"price TTC\",   " +
                "                ore.amount,   " +
                "                ore.id_campaign,   " +
                "                ore.id_structure,   " +
                "                null as price_proposal,   " +
                "                ore.id_order_client_equipment,   " +
                "                ore.id_operation,   " +
                "                null as override_region   " +
                "              from   " +
                "                " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore  " +
                "            )   " +
                "            UNION   " +
                "              (  " +
                "                select   " +
                "                  oce.id,   " +
                "                  CASE WHEN price_proposal is null then price + (price * tax_amount / 100) else price_proposal end as \"price TTC\",   " +
                "                  amount,   " +
                "                  id_campaign,   " +
                "                  id_structure,   " +
                "                  price_proposal,   " +
                "                  null as id_order_client_equipment,   " +
                "                  id_operation,   " +
                "                  override_region   " +
                "                from   " +
                "                  " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "              )  " +
                "          ) as orders   " +
                "        WHERE   " +
                "          orders.id_structure = ?   " +
                "          AND orders.id_campaign = ?  " +
                "      ) as totalOrders  " +
                "  ),   " +
                "  initial_amount = ?   " +
                "WHERE   " +
                "  id = ? RETURNING *  ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(purse.getDouble("amount"))
                .add(purse.getDouble("amount"))
                .add(purse.getString("id_structure"))
                .add(purse.getInteger("id_campaign"))
                .add(purse.getDouble("amount"))
                .add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
//        Sql.getInstance().prepared(query, params, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> event) {
//                log.info(event.body());
//                String status = event.body().getString("status");
//
//                if(status.equals("ok")){
//                    handler
//                }else{
//                    String message = event.body().getString("message");
//                    if(message.contains("Check_amount_positive"))
//                        log.info("Pas de chance");
//                    else{
//                        log.info("plop");
//                    }
//                }
//            }
//        });
    }

    @Override
    public JsonObject updatePurseAmountStatement(Double price, Integer idCampaign, String idStructure,String operation) {
        final double cons = 100.0;
        String updateQuery = "UPDATE  " + Lystore.lystoreSchema + ".purse " +
                "SET amount = amount " +  operation + " ?  " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round(price * cons)/cons)
                .add(idCampaign)
                .add(idStructure);

        return new JsonObject()
                .put("statement", updateQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void checkPurses(Integer id, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH values  " +
                "     AS (SELECT orders.id,  " +
                "                orders.id_campaign,  " +
                "                orders.\"price TTC\",  " +
                "                Round(( (SELECT CASE  " +
                "                                  WHEN orders.price_proposal IS NOT NULL THEN 0  " +
                "                                  WHEN orders.override_region IS NULL THEN 0  " +
                "                                  WHEN Sum(oco.price + ( ( oco.price *  " +
                "                                                         oco.tax_amount )  " +
                "                                                         / 100 )  " +
                "                                                       *  " +
                "                                                       oco.amount) IS  " +
                "                                       NULL THEN 0  " +
                "                                  ELSE Sum(oco.price + ( ( oco.price *  " +
                "                                                         oco.tax_amount )  " +
                "                                                         / 100 )  " +
                "                                                       *  " +
                "                                                       oco.amount)  " +
                "                                END  " +
                "                         FROM    " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "                         WHERE  oco.id_order_client_equipment = orders.id)  " +
                "                        + orders.\"price TTC\" ) * orders.amount, 2) AS total,  " +
                "                orders.id_structure,  " +
                "                orders.id_operation                                AS  " +
                "                id_operation  " +
                "         FROM   ( " +
                "                 (SELECT oce.id,  " +
                "                         false AS isregion,  " +
                "                         CASE  " +
                "                           WHEN price_proposal IS NULL THEN price +  " +
                "                           ( price * tax_amount  " +
                "                             / 100 )  " +
                "                           ELSE price_proposal  " +
                "                         END   AS \"price TTC\",  " +
                "                         amount,  " +
                "                         creation_date,  " +
                "                         NULL  AS modification_date,  " +
                "                         NAME,  " +
                "                         summary,  " +
                "                         description,  " +
                "                         image,  " +
                "                         status,  " +
                "                         id_contract,  " +
                "                         equipment_key,  " +
                "                         id_campaign,  " +
                "                         id_structure,  " +
                "                         cause_status,  " +
                "                         number_validation,  " +
                "                         id_order,  " +
                "                         comment,  " +
                "                         rank  AS \"prio\",  " +
                "                         price_proposal,  " +
                "                         id_project,  " +
                "                         NULL  AS id_order_client_equipment,  " +
                "                         program,  " +
                "                         action,  " +
                "                         id_operation,  " +
                "                         override_region  " +
                "                  FROM    " + Lystore.lystoreSchema + ".order_client_equipment oce)) AS orders  " +
                "         WHERE  orders.id_campaign = ?)  " +
                "SELECT Array_to_json(Array_agg(values.*))                               AS  " +
                "       orders,  " +
                "       ( purse.initial_amount - ( Sum(values.total ) + purse.amount ) ) AS substraction  " +
                "       ,  " +
                "       purse.id_structure,  " +
                "       purse.id_campaign  " +
                "FROM    " + Lystore.lystoreSchema + ".purse  " +
                "       LEFT JOIN values  " +
                "              ON purse.id_campaign = values.id_campaign  " +
                "                 AND purse.id_structure = values.id_structure  " +
                "WHERE  purse.id_campaign = ? " +
                "GROUP  BY purse.id_structure,  " +
                "          purse.initial_amount,  " +
                "          purse.amount,  " +
                "          purse.id_campaign ";
        Sql.getInstance().prepared(query,new JsonArray().add(id).add(id), new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L),SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight())
                {
                    JsonArray results = event.right().getValue();
                    for(int i =0;i<results.size();i++){
                       JsonObject result = results.getJsonObject(i);
                       try {
                           result.put("substraction",  Double.parseDouble(result.getString("substraction")));
                       }catch (NullPointerException e){
                            result.put("substraction",0.d);
                       }
                    }
                    handler.handle(new Either.Right<>(results));
                }else {
                    handler.handle(new Either.Left<>("Error in SQL when checking purse"));
                }
            }
        }));
    }
}
