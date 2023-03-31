package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.Campaign;
import fr.openent.crre.model.ProjectModel;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.CampaignService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.formula.functions.T;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

public class DefaultCampaignService extends SqlCrudService implements CampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignService.class);

    public DefaultCampaignService(String schema, String table) {
        super(schema, table);
    }

    public void listCampaigns(Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> campaignFuture = Future.future();
        Future<JsonArray> purseFuture = Future.future();
        Future<JsonArray> orderFuture = Future.future();


        CompositeFuture.all(campaignFuture, purseFuture, orderFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignFuture.result();
                JsonArray purses = purseFuture.result();
                JsonArray orders = orderFuture.result();


                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    object = campaigns.getJsonObject(i);
                    object.put("nb_orders_waiting", 0).put("nb_orders_valid", 0).put("nb_orders_sent", 0);
                    campaignMap.put(object.getInteger(Field.ID).toString(), object);
                }

                for (int i = 0; i < purses.size(); i++) {
                    object = purses.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("purse_amount", object.getDouble("amount"));
                    }catch (NullPointerException e){
                        //LOGGER.warn("A purse is present on this structure but the structure is not linked to the campaign");
                    }
                }

                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_order", object.getLong("nb_order"));
                        campaign.put("nb_order_waiting", object.getInteger("nb_order_waiting"));
                    }catch (NullPointerException e){
                        //LOGGER.info("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }
                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }
                handler.handle(new Either.Right<>(campaignList));

            } else {
                handler.handle(new Either.Left<>("An error occurred when retrieving campaigns"));
            }
        });
        getCampaignsInfo(null,handlerJsonArray(campaignFuture));
        getCampaignsPurses(handlerJsonArray(purseFuture));
        getCampaignOrderStatusCount(null, handlerJsonArray(orderFuture));
    }

    private void getCampaignsPurses(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT ROUND(SUM(amount)::numeric,2)::double precision as purse " +
                "FROM " + Crre.crreSchema + ".purse " +
                "GROUP BY id_structure;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsPurses(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT ROUND(amount::numeric,2)::double precision AS amount, ROUND(initial_amount::numeric,2)::double precision AS initial_amount, " +
                "ROUND(consumable_amount::numeric,2)::double precision AS consumable_amount, ROUND(consumable_initial_amount::numeric,2)::double precision AS consumable_initial_amount, " +
                "id_structure " +
                "FROM " + Crre.crreSchema + ".purse " +
                "WHERE id_structure = ?";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsLicences(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT seconde, premiere, terminale, secondetechno, premieretechno, terminaletechno, secondepro, premierepro, terminalepro, cap1, cap2, cap3, " +
                "bma1, bma2, pro, l.* " +
                "FROM " + Crre.crreSchema + ".licences l " +
                "FULL JOIN " + Crre.crreSchema + ".students s ON (s.id_structure = l.id_structure) "+
                "WHERE l.id_structure = ?";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String sub_query_waiting_order = "WITH count_order_waiting AS (" +
                "SELECT COUNT(order_client_equipment.id) as nb_order_waiting, campaign.id as id_campaign " +
                "FROM " + Crre.crreSchema +".order_client_equipment " +
                "INNER JOIN " + Crre.crreSchema +".campaign ON (order_client_equipment.id_campaign = campaign.id) " +
                "WHERE ";
        if(idStructure != null){
            sub_query_waiting_order += " id_structure = ? AND ";
            values.add(idStructure);
        }
        sub_query_waiting_order += "status = 'WAITING' GROUP BY campaign.id) ";

        String query = sub_query_waiting_order +
                "SELECT bo.id_campaign, id_user as user_id, COUNT(bo.id) as nb_order, cow.nb_order_waiting " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN count_order_waiting cow ON bo.id_campaign = cow.id_campaign ";
        if(idStructure != null){
            query += "WHERE id_structure = ? ";
            values.add(idStructure);
        }
        query += "GROUP BY bo.id_campaign, id_user, nb_order_waiting;";

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT campaign.*, count(DISTINCT rel_group_structure.id_structure) as nb_structures, " +
                "tc.name as type_name, jsonb(array_to_json(array_agg(distinct groupe))) as groups " +
                "FROM " + Crre.crreSchema + ".campaign " +
                "LEFT JOIN " +
                    "(SELECT rel_group_campaign.id_campaign, structure_group.* as tags " +
                    "FROM " + Crre.crreSchema + ".structure_group " +
                    "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON structure_group.id = rel_group_campaign.id_structure_group " +
                    "GROUP BY rel_group_campaign.id_campaign, structure_group.id) AS groupe ON groupe.id_campaign = campaign.id " +
                "LEFT JOIN " + Crre.crreSchema + ".type_campaign tc ON (tc.id = campaign.id_type) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) ";
        JsonArray values = new JsonArray();
        if(idStructure != null){
            query += "WHERE rel_group_structure.id_structure = ? ";
            values.add(idStructure);
        }
        query += "GROUP BY campaign.id, tc.name ORDER BY campaign.id DESC;";

        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void listCampaigns(String idStructure,  Handler<Either<String, JsonArray>> handler, UserInfos user) {
        Future<JsonArray> campaignFuture = Future.future();
        Future<JsonArray> purseFuture = Future.future();
        Future<JsonArray> basketFuture = Future.future();
        Future<JsonArray> orderFuture = Future.future();
        Future<JsonArray> licenceFuture = Future.future();

        CompositeFuture.all(campaignFuture, purseFuture, basketFuture, orderFuture, licenceFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignFuture.result();
                JsonArray baskets = basketFuture.result();
                JsonArray purses = purseFuture.result();
                JsonArray orders = orderFuture.result();
                JsonArray licences = licenceFuture.result();

                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    campaign = campaigns.getJsonObject(i);
                    campaign.put("nb_licences_total", 0);
                    campaign.put("nb_licences_available", 0);
                    campaign.put("nb_licences_consumable_total", 0);
                    campaign.put("nb_licences_consumable_available", 0);
                    campaign.put("nb_licences_2de", 0);
                    campaign.put("nb_licences_1ere", 0);
                    campaign.put("nb_licences_Tale", 0);
                    campaign.put("nb_licences_2depro", 0);
                    campaign.put("nb_licences_1erepro", 0);
                    campaign.put("nb_licences_Talepro", 0);
                    campaign.put("nb_licences_cap1", 0);
                    campaign.put("nb_licences_cap2", 0);
                    campaign.put("nb_licences_cap3", 0);
                    campaign.put("nb_licences_bma1", 0);
                    campaign.put("nb_licences_bma2", 0);
                    if(purses.size() > 0) {
                        object = purses.getJsonObject(0);
                        try {
                            campaign.put("purse_amount", object.getDouble("amount", 0.0));
                            campaign.put("initial_purse_amount", object.getDouble("initial_amount", 0.0));
                            campaign.put("consumable_purse_amount", object.getDouble("consumable_amount", 0.0));
                            campaign.put("consumable_initial_purse_amount", object.getDouble("consumable_initial_amount", 0.0));
                        } catch (NullPointerException e) {
                            //LOGGER.warn("A purse is present on this structure but the structure is not linked to the campaign");
                        }
                    }
                    campaignMap.put(campaign.getInteger(Field.ID).toString(), campaign);
                }

                for (int i = 0; i < baskets.size(); i++) {
                    object = baskets.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_panier", object.getLong("nb_panier"));
                    }catch (NullPointerException e){
                        LOGGER.info("A basket is present on this structure but the structure is not linked to the campaign");
                    }
                }
                for (int i = 0; i < licences.size(); i++) {
                    object = licences.getJsonObject(i);
                    try {

                        int nb_total = object.getInteger("initial_amount",0);
                        int nb_total_available = object.getInteger("amount",0);
                        int nb_total_consumable = object.getInteger("consumable_initial_amount",0);
                        int nb_total_available_consumable = object.getInteger("consumable_amount",0);
                        for (int s = 0; s < campaigns.size(); s++) {
                            campaign = campaignMap.getJsonObject(campaigns.getJsonObject(s).getInteger(Field.ID).toString());
                            campaign.put("nb_licences_total", nb_total);
                            campaign.put("nb_licences_available", nb_total_available);
                            campaign.put("nb_licences_consumable_total", nb_total_consumable);
                            campaign.put("nb_licences_consumable_available", nb_total_available_consumable);
                            campaign.put("nb_licences_2de", object.getInteger("seconde",0) * 9);
                            campaign.put("nb_licences_1ere", object.getInteger("premiere",0) * 8);
                            campaign.put("nb_licences_Tale", object.getInteger("terminale",0) * 7);
                            campaign.put("nb_licences_2depro", object.getInteger("secondepro",0) * 3);
                            campaign.put("nb_licences_1erepro", object.getInteger("premierepro",0) * 3);
                            campaign.put("nb_licences_Talepro", object.getInteger("terminalepro",0) * 3);
                            campaign.put("nb_licences_cap1", object.getInteger("cap1",0) * 3);
                            campaign.put("nb_licences_cap2", object.getInteger("cap2",0) * 3);
                            campaign.put("nb_licences_cap3", object.getInteger("cap3",0) * 3);
                            campaign.put("nb_licences_bma1", object.getInteger("bma1",0) * 3);
                            campaign.put("nb_licences_bma2", object.getInteger("bma2",0) * 3);
                        }
                    } catch (NullPointerException e){
                        LOGGER.info("A licence is present on this structure but the structure is not linked to the campaign");
                    }
                }
                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        if(user.getUserId().equals(object.getString("user_id")))
                            campaign.put("nb_order", object.getLong("nb_order"));
                        if(WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString()))
                            campaign.put("nb_order_waiting", object.getInteger("nb_order_waiting"));
                        campaign.put("order_notification",0);
                        campaign.put("historic_etab_notification",0);
                    }catch (NullPointerException e){
                        //LOGGER.info("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }
                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }
                handler.handle(new Either.Right<>(campaignList));
            } else {
                handler.handle(new Either.Left<>("[DefaultCampaignService@listCampaigns] An error occured. CompositeFuture returned failed :" + event.cause()));
            }
        });

        getCampaignsInfo(idStructure, handlerJsonArray(campaignFuture));
        getCampaignsPurses(idStructure, handlerJsonArray(purseFuture));
        getCampaignOrderStatusCount(idStructure, handlerJsonArray(orderFuture));
        getCampaignsLicences(idStructure, handlerJsonArray(licenceFuture));
        getBasketCampaigns(idStructure, handlerJsonArray(basketFuture), user);
    }

    private void getBasketCampaigns(String idStructure, Handler<Either<String, JsonArray>> handler, UserInfos user) {
        String query = "SELECT COUNT(basket_order_item.id) as nb_panier, campaign.id as id_campaign " +
                "FROM " + Crre.crreSchema + ".basket_order_item " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON (campaign.id = basket_order_item.id_campaign) " +
                "WHERE id_structure = ? " +
                "AND basket_order_item.owner_id = ? " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure).add(user.getUserId()), SqlResult.validResultHandler(handler));
    }
    public Future<JsonObject> getCampaign(Integer id) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "  SELECT campaign.*,jsonb(array_to_json(array_agg(groupe))) as  groups "+
                "FROM  " + Crre.crreSchema + ".campaign campaign  "+
                "LEFT JOIN  "+
                "(SELECT rel_group_campaign.id_campaign, structure_group.* as tags " +
                "FROM " + Crre.crreSchema + ".structure_group " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign" +
                " ON structure_group.id = rel_group_campaign.id_structure_group "+
                "WHERE rel_group_campaign.id_campaign = ?  "+
                "GROUP BY (rel_group_campaign.id_campaign, structure_group.id)) as groupe " +
                "ON groupe.id_campaign = campaign.id "+
                "where campaign.id = ?  "+
                "group by (campaign.id);  " ;
        sql.prepared(query, new JsonArray().add(id).add(id),
                SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }
    public Future<Campaign> create(final Campaign campaign, List<StructureGroupModel> groups) {
        Promise<Campaign> promise = Promise.promise();

        SqlHelper.getNextVal("campaign_id_seq")
                .compose(nextVal -> {
                    campaign.setId(nextVal);
                    String query = "INSERT INTO crre.campaign(id, name, description, image, accessible, purse_enabled, priority_enabled," +
                            " priority_field, start_date, end_date, automatic_close, reassort, catalog, use_credit, id_type)" +
                            " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" +
                            " RETURNING *";
                    JsonArray params = new JsonArray()
                            .add(campaign.getId())
                            .add(campaign.getName())
                            .add(campaign.getDescription())
                            .add(campaign.getImage())
                            .add(campaign.isAccessible())
                            .add(campaign.isPurseEnabled())
                            .add(campaign.isPriorityEnabled())
                            .add(campaign.getPriorityField())
                            .add(campaign.getStartDate())
                            .add(campaign.getEndDate())
                            .add(campaign.getAutomaticClose())
                            .add(campaign.getReassort())
                            .add(campaign.getCatalog())
                            .add(campaign.getUseCredit())
                            .add(campaign.getIdType());
                    List<TransactionElement> statements = new ArrayList<>();
                    statements.add(new TransactionElement(query, params));
                    statements.add(getCampaignTagsGroupsRelationshipStatement(campaign.getId(), groups));
                    String errorMessage = String.format("[CRRE@%s::create] Fail to create campaign",
                            this.getClass().getSimpleName());
                    return TransactionHelper.executeTransaction(statements, errorMessage);
                })
                .onSuccess(res -> promise.complete(new Campaign(res.get(0).getResult().getJsonObject(0))))
                .onFailure(promise::fail);

        return promise.future();
    }

    public Future<Campaign> update(Campaign campaign, List<StructureGroupModel> groups){
        Promise<Campaign> promise = Promise.promise();
        String query = "UPDATE crre.campaign set name = ?, description = ?, image = ?, accessible = ?, purse_enabled = ?, priority_enabled = ?," +
                " priority_field = ?, start_date = ?, end_date = ?, automatic_close = ?, reassort = ?, catalog = ?, use_credit = ?, id_type = ?" +
                " WHERE id = ?" +
                " RETURNING *";
        JsonArray params = new JsonArray()
                .add(campaign.getName())
                .add(campaign.getDescription())
                .add(campaign.getImage())
                .add(campaign.isAccessible())
                .add(campaign.isPurseEnabled())
                .add(campaign.isPriorityEnabled())
                .add(campaign.getPriorityField())
                .add(campaign.getStartDate())
                .add(campaign.getEndDate())
                .add(campaign.getAutomaticClose())
                .add(campaign.getReassort())
                .add(campaign.getCatalog())
                .add(campaign.getUseCredit())
                .add(campaign.getIdType())
                .add(campaign.getId());

        List<TransactionElement> statements = new ArrayList<>();
        statements.add(new TransactionElement(query, params));
        statements.add(getCampaignTagGroupRelationshipDeletion(campaign.getId()));
        statements.add(getCampaignTagsGroupsRelationshipStatement(campaign.getId(), groups));
        String errorMessage = String.format("[CRRE@%s::create] Fail to create campaign",
                this.getClass().getSimpleName());
        TransactionHelper.executeTransaction(statements, errorMessage)
                .onSuccess(res -> promise.complete(new Campaign(res.get(0).getResult().getJsonObject(0))))
                .onFailure(promise::fail);
        return promise.future();
    }

    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignsGroupRelationshipDeletion(ids))
                .add(getCampaignsDeletion(ids));

        sql.transaction(statements, event -> handler.handle(getTransactionHandler(event,ids.get(0))));
    }

    @Override
    public void getStructures(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_structure as id " +
                "FROM " + Crre.crreSchema + ".campaign " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "WHERE id = ? " +
                "GROUP BY id_structure";
        JsonArray params = new JsonArray()
                .add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCampaignTypes(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id as id_type, name as name_type, credit, reassort, catalog, automatic_close, structure " +
                "FROM " + Crre.crreSchema + ".type_campaign " +
                "ORDER BY name ASC";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void updateAccessibility(final Integer id,final JsonObject campaign,
                                    final Handler<Either<String, JsonObject>> handler){
        String options = "";
        String finalCondition  = "";
        if(!campaign.getBoolean("automatic_close")) {
            if (campaign.getValue("start_date") == null && campaign.getBoolean("accessible")) {
                options += "start_date = NOW() ,";
            }
            if(!campaign.getBoolean("accessible")){
                options += "end_date = NOW() ,";
            }else {
                options += "end_date = NULL ,";
            }
        }else{
            if(campaign.getBoolean("accessible")){
                options += "start_date = NOW (),";
                finalCondition = " AND start_date > NOW()";
            }else{
                options += "end_date = NOW() ,";
            }
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String query = "UPDATE " + Crre.crreSchema + ".campaign SET " +
                options +
                "accessible= ? " +
                "WHERE id = ? " +
                finalCondition +
                ";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getBoolean("accessible"))
                .add(id);
        statements.add(new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action", "prepared"));
        sql.transaction(statements, event -> handler.handle(getTransactionHandler(event, id)));
    }

    private TransactionElement getCampaignTagsGroupsRelationshipStatement(Number id, List<StructureGroupModel> groups) {
        String insertTagCampaignRelationshipQuery = "INSERT INTO " +
                Crre.crreSchema + ".rel_group_campaign" +
                "(id_campaign, id_structure_group) VALUES ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for(StructureGroupModel structureGroupModel : groups){
            insertTagCampaignRelationshipQuery += "(?, ?), ";
            params.add(id)
                    .add(structureGroupModel.getId());
        }
        insertTagCampaignRelationshipQuery = insertTagCampaignRelationshipQuery.substring(0, insertTagCampaignRelationshipQuery.length() - 2);
        return new TransactionElement(insertTagCampaignRelationshipQuery, params);
    }

    private TransactionElement getCampaignTagGroupRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_campaign " +
                "WHERE id_campaign = ?;";

        return new TransactionElement(query, new JsonArray().add(id));
    }

    private JsonObject getCampaignsGroupRelationshipDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_campaign " +
                " WHERE id_campaign in  " +
                Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", value)
                .put("action", "prepared");
    }

    private JsonObject getCampaignsDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".campaign " +
                " WHERE id in  " + Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", value)
                .put("action", "prepared");
    }
    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey(Field.STATUS) && Field.OK.equals(result.getString(Field.STATUS))) {
            JsonObject returns = new JsonObject()
                    .put(Field.ID, id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching campaign transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

}
