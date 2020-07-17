package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ProjectService;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;


public class DefaultProjectService extends SqlCrudService implements ProjectService {
    private PurseService purseService;
    private Logger log = LoggerFactory.getLogger(DefaultProjectService.class);
    public DefaultProjectService(String schema, String table) {
        super(schema, table);
        purseService = new DefaultPurseService();
    }

    /**
     * List all the projects
     *
     * @param handler
     */
    @Override
    public void getProjects(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT  project.id,project.description, project.id_title, project.id_grade, project.building, project.stair, project.room, project.site, project.preference, " +
                "array_to_json( array_agg( tt.* )) as title , array_to_json ( array_agg ( gr.*)) as grade " +
                "FROM " + Lystore.lystoreSchema + ".project " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".title tt ON tt.id = project.id_title " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".grade gr ON gr.id = project.id_grade " +
                "GROUP BY project.preference, tt.name, project.id ,gr.name ;";

        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


    @Override
    public void createProject(JsonObject project, Integer idCampaign, String idStructure, Handler<Either<String, JsonObject>> handler) {


        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".project (" +
                "id_title, id_grade, description, building, stair, room, site, preference) " +
                "Values ( ?, ?, ?, ?, ?, ?, ?, "+ getTheLastPreferenceProject()+") RETURNING id ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(project.getInteger("id_title"))
                .add(project.getInteger("id_grade"))
                .add(project.getString("description"))
                .add(project.getString("building"))
                .add(project.getInteger("stair"))
                .add(project.getString("room"))
                .add(project.getString("site"))
                .add(idCampaign)
                .add(idStructure);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    private String getTheLastPreferenceProject() {
        return "(select case  WHEN max(p.preference) is null THEN 0 " +
                "ELSE max(p.preference + 1) " +
                "END " +
                "from "+ Lystore.lystoreSchema+".order_client_equipment as oce " +
                "inner join "+Lystore.lystoreSchema+".project p ON p.id = oce.id_project "+
                "where oce.id_campaign = ? and oce.id_structure = ? ) ";
    }

    @Override
    public void getProject(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT project.* " +
                "FROM " + Lystore.lystoreSchema + ".project " +
                "Where project.id = ? ;";

        JsonArray params = new JsonArray().add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void revertOrderAndDeleteProject(JsonArray orders, Integer id, Integer idCampaign, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String getTotalToUpdate = " WITH values as ( SELECT  " +
                "                CASE count(opts) " +
                "                WHEN 0 THEN ROUND((oe.price + (oe.tax_amount * oe.price)/100), 2) * oe.amount " +
                "                ELSE (price_all_options +(ROUND(oe.price + (oe.tax_amount * oe.price)/100, 2))) * oe.amount " +
                "                END as price_total_equipment " +
                "                FROM " + Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "                LEFT JOIN (SELECT SUM((ROUND(price +(tax_amount * price)/100,2))) as price_all_options, " +
                "                id_order_client_equipment FROM " + Lystore.lystoreSchema + ".order_client_options " +
                "                GROUP BY id_order_client_equipment) opts ON oe.id = opts.id_order_client_equipment " +
                "                Where id_campaign= "+ idCampaign +
                "                GROUP BY oe.id, price_all_options) " +
                "     " +
                "    SELECT sum (values.price_total_equipment) as total " +
                "    from values " +
                "    ";

        sql.raw(getTotalToUpdate,
                SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                                                       @Override
                                                       public void handle(Either<String, JsonObject> event) {
                                                           if(event.isRight()){
                                                               JsonObject order;
                                                               JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();

                                                               Double totalToAdd =Double.parseDouble( event.right().getValue().getString("total"));
                                                               statements.add(getUpdatePurse(idCampaign,idStructure,totalToAdd));
                                                               //Query
                                                               for (int i = 0; i < orders.size(); i++) {
                                                                   order = orders.getJsonObject(i);
                                                                   statements.add(getInsertBasketEquipmentOrderStatement(order));
                                                                   if (order.containsKey("options")) {
                                                                       try {
                                                                           statements.add(getInsertBasketOptionOrderStatement(order));
                                                                       } catch (NullPointerException e) {

                                                                       }
                                                                   }
                                                               }

                                                               statements.add(getNewNbBasket(idCampaign, idStructure));
                                                               statements.add(deleteProject(id));
                                                               statements.add(getNewNbORDER(idCampaign, idStructure));
                                                               statements.add(getPurse(idCampaign, idStructure));

                                                               sql.transaction(statements, new Handler<Message<JsonObject>>() {
                                                                   @Override
                                                                   public void handle(Message<JsonObject> event) {
                                                                       if (!"ok".equals(event.body().getString("status"))) {
                                                                           String message = "An error occurred when deleting project " + id;
                                                                           log.error(message);
                                                                           handler.handle(new Either.Left<>(message));
                                                                       } else {
                                                                           JsonArray results = event.body().getJsonArray("results");
                                                                           Double purse_amount = Double.parseDouble(results.getJsonObject(results.size()-1).getJsonArray("results").getJsonArray(0).getString(0));
                                                                           JsonObject res;
                                                                           Integer nb_order = -1;
                                                                           Integer nb_basket = -1;
                                                                           JsonArray fields, nb_order_array, nb_basket_array;
                                                                           for (int i = 0; i < results.size(); i++) {
                                                                               res = results.getJsonObject(i);
                                                                               fields = res.getJsonArray("fields");
                                                                               if (fields.size() != 0) {
                                                                                   String type = fields.getString(0);

                                                                                   if (type.equals("nb_order")) {
                                                                                       nb_order_array = res.getJsonArray("results");
                                                                                       nb_order_array = nb_order_array.getJsonArray(0);
                                                                                       nb_order = nb_order_array.getInteger(0);
                                                                                   }
                                                                                   if (type.equals("nb_basket")) {
                                                                                       nb_basket_array = res.getJsonArray("results");
                                                                                       nb_basket_array = nb_basket_array.getJsonArray(0);
                                                                                       nb_basket = nb_basket_array.getInteger(0);
                                                                                   }
                                                                               }
                                                                           }
                                                                           JsonObject resultfinal = new JsonObject().put("status", "ok");
                                                                           if (nb_basket >= 0 && nb_order >= 0) {
                                                                               resultfinal.put("nb_order", nb_order).put("nb_basket", nb_basket).put("purse_amount",purse_amount);
                                                                           }
                                                                           handler.handle(new Either.Right<>(resultfinal));
                                                                       }
                                                                   }
                                                               });
                                                           }
                                                       }
                                                   }
                ));
    }

    private JsonObject getPurse(Integer idCampaign, String idStructure) {
        String query = "SELECT amount from "+Lystore.lystoreSchema+".purse " +
                "where id_structure = ? AND id_campaign = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
             .add(idStructure).add(idCampaign);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getUpdatePurse(Integer idCampaign, String idStructure,Double price) {
        return purseService.updatePurseAmountStatement(price, idCampaign, idStructure, "+");
    }

    private JsonObject getNewNbORDER(Integer idCampaign, String idStructure) {
        String query = "SELECT count(id) as nb_order FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? AND status != 'VALID';";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getNewNbBasket(Integer idCampaign, String idStructure) {
        String query = "SELECT count(id) as nb_basket FROM " + Lystore.lystoreSchema + ".basket_equipment " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    public void selectOrdersToBaskets(Integer id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oe.equipment_key as id_equipment, array_to_json(array_agg(DISTINCT bo.*)) as options, oe.price as price_equipment, oe.comment as comment, oe.id_campaign as id_campaign," +
                "oe.id_structure as id_structure , nextval( '" + Lystore.lystoreSchema + ".basket_equipment_id_seq') as id_basket_equipment, " +
                "oe.price_proposal as price_proposal, oe.amount as amount  " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment as oe " +
                "LEFT JOIN ( " +
                "SELECT  equipment_option.id_equipment as id,equipment_option.id as id_option " +
                "FROM " + Lystore.lystoreSchema + ".equipment_option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON equipment_option.id_equipment= equipment.id " +
                ") bo ON oe.equipment_key = bo.id " +
                "WHERE oe.id_project = ? " +
                "GROUP BY oe.id ;";
        JsonArray params = new JsonArray().add(id);


        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateProject(JsonObject project, Handler<Either<String, JsonObject>> handler, Integer id) {
        String query;
        JsonArray params = new JsonArray();
        params.add(project.getInteger("id_title"));
        if (project.containsKey("id_grade"))
            params.add(project.getInteger("id_grade"));
        if (project.containsKey("description"))
            params.add(project.getString("description"));
        if (project.containsKey("building"))
            params.add(project.getString("building"));
        if (project.containsKey("stair"))
            params.add(project.getInteger("stair"));
        if (project.containsKey("room"))
            params.add(project.getString("room"));
        if (project.containsKey("site"))
            params.add(project.getString("site"));

        query = "UPDATE " + Lystore.lystoreSchema + ".project " +
                "SET id_title = ?, " +
                "id_grade = " + (project.containsKey("id_grade") ? "?," : "null, ") +
                "description = " + (project.containsKey("description") ? "?," : "null, ") +
                "building =  " + (project.containsKey("building") ? "?," : "null, ") +
                "stair =  " + (project.containsKey("stair") ? "?," : "null, ") +
                "room =  " + (project.containsKey("room") ? "?," : "null, ") +
                "site =  " + (project.containsKey("site") ? "? " : "null ") +
                "WHERE id = ? ;";

        params.add(id);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deletableProject(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(project.id) as count" +
                " FROM " + Lystore.lystoreSchema + ".project " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "ON project.id = oce.id_project " +
                "WHERE oce.status != 'WAITING' AND project.id = ? ;";
        JsonArray params = new JsonArray().add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonObject deleteProject(Integer id) {
        JsonArray params = new JsonArray();
        String queryDeletionProject = " DELETE FROM " + Lystore.lystoreSchema + ".project " +
                "WHERE project.id = ? ;";

        params.add(id);
        return new JsonObject()
                .put("statement", queryDeletionProject)
                .put("values", params)
                .put("action", "prepared");

    }

    private static JsonObject getInsertBasketEquipmentOrderStatement(JsonObject order) {
        StringBuilder queryBasketEquipment;
        JsonArray params;

        try {
            queryBasketEquipment = new StringBuilder()
                    .append(" INSERT INTO lystore.basket_equipment ")
                    .append(" ( id, amount,  id_equipment, id_campaign, id_structure, comment, price_proposal ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?); ");
            params = new fr.wseduc.webutils.collections.JsonArray();

            params.add(order.getInteger("id_basket_equipment"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("id_equipment"))
                    .add(order.getInteger("id_campaign"))
                    .add(order.getString("id_structure"))
                    .add(order.getString("comment"))
                    .add(Double.valueOf(order.getString("price_proposal")));

        } catch (NullPointerException e) {
            queryBasketEquipment = new StringBuilder()
                    .append(" INSERT INTO lystore.basket_equipment ")
                    .append(" ( id, amount,  id_equipment, id_campaign, id_structure, comment, price_proposal ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, null); ");
            params = new fr.wseduc.webutils.collections.JsonArray();

            params.add(order.getInteger("id_basket_equipment"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("id_equipment"))
                    .add(order.getInteger("id_campaign"))
                    .add(order.getString("id_structure"))
                    .add(order.getString("comment"));


        }
        return new JsonObject()
                .put("statement", queryBasketEquipment.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getInsertBasketOptionOrderStatement(JsonObject order) {

        JsonObject option;
        JsonArray options = new JsonArray(order.getString("options"));
        String buffer = "";
        for (int i = 0; i < options.size(); i++) {
            buffer += " (?, ?),";

        }
        buffer = buffer.substring(0, buffer.length() - 1);

        String queryBasketOption = "INSERT INTO " + Lystore.lystoreSchema + ".basket_option " +
                " ( id_option, id_basket_equipment)" +
                "VALUES " + buffer + ";";

        JsonArray params = new JsonArray();

        for (int i = 0; i < options.size(); i++) {
            option = options.getJsonObject(i);
            params.add(option.getInteger("id_option")).add(order.getInteger("id_basket_equipment"));
        }
        return new JsonObject()
                .put("statement", queryBasketOption.toString())
                .put("values", params)
                .put("action", "prepared");


    }


}
