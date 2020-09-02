package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.EquipmentService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.elasticsearch.BulkRequest;
import org.entcore.common.elasticsearch.ElasticSearch;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;


public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);
    private static final String STATEMENT = "statement" ;
    private static final String VALUES = "values" ;
    private static final String ACTION = "action" ;
    private static final String PREPARED = "prepared" ;
    public static final String EVENTS = "equipment";

    public DefaultEquipmentService(String schema, String table) {
        super(schema, table);
    }


    private String getSqlOrderValue(String field) {
        String typeField;
        switch (field) {
            case "supplier": {
                field = "name";
                typeField = "supplier";
                break;
            }
            case "contract": {
                field = "name";
                typeField = "contract";
                break;
            }
            case "status":
            case "reference":
            case "name":
            case "price":
            default:
                typeField = "equip";
        }

        return typeField + "." + field;
    }

    private String getSqlReverseString(Boolean reverse) {
        return reverse ? "DESC" : "ASC";
    }

    private String getTextFilter(List<String> filters) {
        StringBuilder filter = new StringBuilder();
        String q;
        if (filters.size() > 0) {
            filter = new StringBuilder("WHERE ");
            for (int i = 0; i < filters.size(); i++) {
                q = filters.get(i);
                if (i > 0) {
                    filter.append("AND ");
                }

                filter.append("(LOWER(equip.name) ~ LOWER(?) OR LOWER(equip.reference) ~ LOWER(?) OR LOWER(supplier.name) ~ LOWER(?) OR LOWER(contract.name) ~ LOWER(?)) ");
            }
        }

        return filter.toString();
    }

    public void searchWord(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearch(word, handler);
    }

    public void searchAll(Handler<Either<String, JsonArray>> handler) {
        search_All(handler);
    }


    public void syncES() {
        String query = "SELECT e.id, e.name, e.summary, e.description, e.author, e.price, e.id_tax, e.image, " +
                "e.id_editor, e.status, e.technical_specs, to_char(parution_date, 'month yyyy') parution_date, e.option_enabled, " +
                "e.reference,e.price_editable, e.ean, e.offer, e.duration, to_char(end_availability, 'dd/MM/yyyy ') end_availability, " +
                "tax.value tax_amount, editor.name as editor_name, STRING_AGG ( DISTINCT grade.name,', ') grade_name, " +
                "STRING_AGG ( DISTINCT subject.name,', ') subject_name, array_to_json(array_agg(DISTINCT opts)) as options " +
                "FROM " + Crre.crreSchema + ".equipment e " +
                "LEFT JOIN ( " +
                "SELECT option.*, equipment.name, equipment.price, tax.value tax_amount " +
                "FROM " + Crre.crreSchema + ".equipment_option option " +
                "INNER JOIN " + Crre.crreSchema + ".equipment ON (option.id_option = equipment.id) " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = equipment.id_tax " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = e.id_tax " +
                "LEFT JOIN " + Crre.crreSchema + ".rel_equipment_grade ON (rel_equipment_grade.id_equipment = e.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".editor ON (editor.id = e.id_editor) " +
                "LEFT JOIN " + Crre.crreSchema + ".grade ON (grade.id = rel_equipment_grade.id_grade) " +
                "LEFT JOIN " + Crre.crreSchema + ".rel_equipment_subject ON (rel_equipment_subject.id_equipment = e.id) " +
                "LEFT JOIN " + Crre.crreSchema + ".subject ON (subject.id = rel_equipment_subject.id_subject) " +
                "GROUP BY (e.id, tax.id, editor.id)";
        sql.raw(query, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                final JsonArray results = event.right().getValue();
                final int resultsSize = results.size();
                final JsonArray eventsIds = new JsonArray();
                ElasticSearch es = ElasticSearch.getInstance();
                BulkRequest bulkRequest = es.bulk(EVENTS, ar -> {
                    if (ar.succeeded()) {
                        JsonArray items = ar.result().getJsonArray("items");
                        if (items.size() != resultsSize) {
                            LOGGER.error("Error different sync length. Expected : " + resultsSize +
                                    " - Found : " + items.size());
                            return;
                        }
                        int countWarningItems = 0;
                        for (Object o : items) {
                            final int itemStatus = ((JsonObject) o).getJsonObject("index").getInteger("status");
                            if (itemStatus != 201) {
                                if (itemStatus == 200) {
                                    countWarningItems++;
                                    //log.warn("Update event in ES : " + ((JsonObject) o).encode());
                                } else {
                                    LOGGER.error("Error persisting event in ES : " + ((JsonObject) o).encode());
                                    return;
                                }
                            }
                        }
                        if (countWarningItems > 0) {
                            LOGGER.warn("Update " + countWarningItems + " events in ES.");
                        }

                    } else {
                        LOGGER.error("Error sending events to elasticsearch", ar.cause());
                    }
                });
                for (Object o : results) {
                    JsonObject j = (JsonObject) o;
                    j.put("_id", j.getInteger("id"));
                    eventsIds.add(j.getInteger("id"));
                    bulkRequest.index(j, null);
                }
                bulkRequest.end();
            }
        }));
    }

    public void filterWord(HashMap<String, ArrayList<String>> test, Handler<Either<String, JsonArray>> handler) {
        filter(test, handler);
    }

    public void listEquipments(Integer page, String order, Boolean reverse, List<String> filters, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();

        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter);
            }
        }

        String query = "SELECT equip.reference, equip.name, equip.id, equip.status, equip.price, tax.value as tax_amount " +
                "FROM " + Crre.crreSchema + ".equipment equip " +
                "INNER JOIN " + Crre.crreSchema + ".tax ON tax.id = equip.id_tax " +
                getTextFilter(filters) +
                "ORDER by " + getSqlOrderValue(order) + " " + getSqlReverseString(reverse);

        if (page != null) {
            query += " LIMIT " + Crre.PAGE_SIZE + " OFFSET ?";
            params.add(Crre.PAGE_SIZE * page);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void listEquipments(Integer page, List<String> filters, Handler<Either<String, JsonArray>> handler) {

    }

    public void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT equip.*, tax.value as tax_amount, " +
                "array_to_json(array_agg(opts.*)) as options " +
                "FROM  " + Crre.crreSchema + ".equipment equip    " +
                "LEFT JOIN " + Crre.crreSchema + ".equipment_option ON (equip.id = equipment_option.id_equipment) " +
                "LEFT JOIN ( " +
                "SELECT equipment.id as id_equipment, equipment.reference, equipment_option.id, " +
                "equipment_option.id_option, equipment.name, equipment.price, equipment_option.amount, " +
                "equipment_option.required, tax.value as tax_amount, equipment_option.id_equipment as master_equipment " +
                "FROM " + Crre.crreSchema + ".equipment " +
                "INNER JOIN " + Crre.crreSchema + ".tax  ON (equipment.id_tax = tax.id) " +
                "INNER JOIN " + Crre.crreSchema + ".equipment_option  ON (equipment_option.id_option = equipment.id) " +
                ") opts ON (equipment_option.id_option = opts.id_equipment AND opts.master_equipment = equip.id) " +
                "INNER JOIN " + Crre.crreSchema + ".tax ON tax.id = equip.id_tax " +
                "WHERE equip.id = ? " +
                "GROUP BY (equip.id, tax.id, tax.value) " +
                "ORDER by equip.name ASC " +
                "LIMIT 50 OFFSET 0";


        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idEquipment), SqlResult.validResultHandler(handler));
    }



    @Override
    public void listAllEquipments(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT e.*, tax.value tax_amount, grade.name as grade_name, subject.name as subject_name, array_to_json(array_agg(DISTINCT opts)) as options " +
                "FROM " + Crre.crreSchema + ".equipment e LEFT JOIN ( " +
                "SELECT option.*, equipment.name, equipment.price, tax.value tax_amount " +
                "FROM " + Crre.crreSchema + ".equipment_option option " +
                "INNER JOIN " + Crre.crreSchema + ".equipment ON (option.id_option = equipment.id) " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = equipment.id_tax " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Crre.crreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Crre.crreSchema + ".rel_equipment_grade ON (rel_equipment_grade.id_equipment = e.id) " +
                "INNER JOIN " + Crre.crreSchema + ".grade ON (grade.id = rel_equipment_grade.id_grade) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_equipment_subject ON (rel_equipment_subject.id_equipment = e.id) " +
                "INNER JOIN " + Crre.crreSchema + ".subject ON (subject.id = rel_equipment_subject.id_subject) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (" +
                " rel_group_campaign.id_campaign = ? " +
                ((idStructure != null) ?
                        " AND rel_group_campaign.id_structure_group IN (  " +
                                " SELECT structure_group.id FROM  " + Crre.crreSchema + ".structure_group  " +
                                " INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON rel_group_structure.id_structure_group = structure_group.id  " +
                                " WHERE rel_group_structure.id_structure = ? )"
                        : ""
                ) + ")and e.catalog_enabled = true AND e.status != 'OUT_OF_STOCK' " +
                "GROUP BY (e.id, tax.id , grade.id, subject.id nametype )";

        JsonArray values = new JsonArray().add(idCampaign);
        if (idStructure != null)
            values.add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }


    public void create(final JsonObject equipment, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Crre.crreSchema + ".equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                try {
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getEquipmentCreationStatement(id, equipment));

                    JsonArray options = equipment.getJsonArray("optionsCreate");
                    for (int j = 0; j < options.size(); j++) {
                        statements.add(getEquipmentOptionRelationshipStatement(id, options.getJsonObject(j)));
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

    public void importEquipments(final JsonArray equipments, JsonArray referencesToUpdate, Handler<Either<String, JsonObject>> handler) {
        if (referencesToUpdate.size() > 0) {
            String matchReferencesQuery = "SELECT reference FROM " + Crre.crreSchema + ".equipment  WHERE reference IN " + Sql.listPrepared(referencesToUpdate.getList());
            Sql.getInstance().prepared(matchReferencesQuery, referencesToUpdate, SqlResult.validResultHandler(event -> {
                if (event.isRight()) {
                    launchImport(equipments, handler);
                } else {
                    String message = "An error occurred when matching references to update";
                    LOGGER.error(message);
                    handler.handle(new Either.Left<>(message));
                }
            }));
        } else {
            launchImport(equipments, handler);
        }
    }

    private void launchImport(JsonArray equipments, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        JsonObject statementTax;

        for (int i = 0; i < equipments.size(); i++) {
            JsonObject equipment = equipments.getJsonObject(i);

            Double tax = Double.parseDouble(equipment.getValue("id_tax").toString().replace(",", "."));

            statementTax = new JsonObject();
            String insertNotExistentTaxQuery = "INSERT INTO " + Crre.crreSchema + ".tax (value, name) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM " + Crre.crreSchema + ".tax WHERE value = ?)";
            JsonArray taxValues = new JsonArray();
            taxValues.add(tax).add("Taxe " + tax + "%").add(tax);

            statementTax.put(STATEMENT, insertNotExistentTaxQuery);
            statementTax.put(VALUES, taxValues);
            statementTax.put(ACTION, PREPARED);

            statements.add(statementTax);
        }
        if (statements.size() > 0) {
            sql.transaction(statements, event -> {
                if (event.body().containsKey("status") && "ok".equals(event.body().getString("status"))) {
                    handler.handle(new Either.Right<>(new JsonObject().put("message", "Imported")));
                } else {
                    String message = "An error occurred when handling equipment transaction";
                    LOGGER.error(message);
                    handler.handle(new Either.Left<>(message));
                }
            });
        } else {
            String message = "An error occurred when assembling the transaction";
            LOGGER.error(message);
            handler.handle(new Either.Left<>(message));
        }
    }

    @Override
    public void getNumberPages(List<String> filters, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();

        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter);
            }
        }

        String query = "SELECT count(equip.id)" +
                "FROM " + Crre.crreSchema + ".equipment equip " +
                "INNER JOIN " + Crre.crreSchema + ".contract ON (contract.id = equip.id_contract) " +
                "INNER JOIN " + Crre.crreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                getTextFilter(filters);
        returnSQLHandler(handler, params, query);
    }

    private void returnSQLHandler(Handler<Either<String, JsonObject>> handler, JsonArray params, String query) {
        Sql.getInstance().prepared(query, params, event -> {
            if (!"ok".equals(event.body().getString("status"))) {
                handler.handle(new Either.Left<>("An error occurred when collecting equipment count"));
                return;
            }
            int count_result = event.body().getJsonArray("results").getJsonArray(0).getInteger(0);
            if (count_result == 0) {
                count_result = 1;
            }
            handler.handle(new Either.Right<>(new JsonObject().put("count", calculPagesNumber(count_result))));
        });
    }

    @Override
    public void getNumberPagesCatalog(List<String> filters, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        StringBuilder queryFilter = new StringBuilder(" WHERE equipment.status != 'OUT_OF_STOCK' ");
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                queryFilter.append(" AND lower(equipment.name) ~ lower(?) ");
                values.add(filter);
            }
        }
        String query = "SELECT count(equipment.id) " +
                "FROM " + Crre.crreSchema + ".equipment " +
                ")" + queryFilter;

        returnSQLHandler(handler, values, query);
    }


    private Integer calculPagesNumber(Integer count) {
        int pageCount = count / Crre.PAGE_SIZE;
        pageCount += ((count % Crre.PAGE_SIZE) != 0 ? 1 : 0);
        return pageCount;
    }

    @Override
    public void updateEquipment(final Integer id, JsonObject equipment,
                                final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentUpdateStatement(id, equipment));

        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event, id)));
    }
    @Override
    public void updateOptions(final Number id, JsonObject equipment,  JsonObject  resultsObject,
                              final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray deletedOptions  = equipment.getJsonArray("deletedOptions");
        JsonArray optionsCreate =  equipment.getJsonArray("optionsCreate");
        JsonArray optionsUpdate =  equipment.getJsonArray("optionsUpdate");

        if( null != deletedOptions && deletedOptions.size() != 0 ) {
            statements.add(getEquipmentOptionsBasketRelationshipDeletion( deletedOptions));
            statements.add(getEquipmentOptionsRelationshipDeletion(deletedOptions));
        }

        for (int j = 0; j < optionsCreate.size(); j++) {
            JsonObject option = optionsCreate.getJsonObject(j);
            statements.add(createEquipmentOptionRelationshipStatement(id, option, resultsObject.getInteger("id"+j) ));
            if ( option.getBoolean("required") &&
                    resultsObject.getJsonArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getJsonArray("id_basket_equipments"),
                        resultsObject.getInteger("id"+j)));
            }
        }
        for (int i = 0; i < optionsUpdate.size(); i++) {
            JsonObject option = optionsUpdate.getJsonObject(i);
            statements.add(updateEquipmentOptionRelationshipStatement(option));
            if ( option.getBoolean("required") &&
                    resultsObject.getJsonArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getJsonArray("id_basket_equipments"),
                        option.getInteger("id")
                ));
            }
        }
        if (statements.size() > 0) {
            sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event, id)));
        } else {
            handler.handle(new Either.Right<>(new JsonObject().put("id", id)));
        }
    }
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentsOptionsRelationshipDeletion(ids))
                .add(getEquipmentsDeletion(ids));

        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0))));
    }

    @Override
    public void setStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".equipment SET status = ? " +
                "WHERE equipment.id IN " + Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(status);

        for (Integer id: ids) {
            params.add(id);
        }

        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void prepareUpdateOptions (Number optionCreate, Number idEquipment,
                                      final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(getBasketIds(idEquipment));
        for(int i = 0 ; i < (int) optionCreate ; i++) {
            statements.add(getOptionsSequences(i));
        }

        sql.transaction(statements, event -> handler.handle(getTransactionHandler(event)));
    }

    @Override
    public void search(String query,  List<String> listFields, Handler<Either<String, JsonArray>> handler) {

        String sqlQuery = "SELECT e.id, e.name, e.summary, e.description, CAST(e.price AS NUMERIC) as price, t.value as tax_amount, t.id as id_tax, e.image, e.reference, " +
                " e.option_enabled "+
                "FROM " + Crre.crreSchema + ".equipment as e " +
                "INNER JOIN " + Crre.crreSchema + ".tax as t ON (e.id_tax = t.id) " +
                "WHERE e.status = 'AVAILABLE' AND e.option_enabled = true ";

        String fieldName=listFields.get(0);
        if (listFields.size()==1) {
            sqlQuery = sqlQuery + "AND LOWER(e." + fieldName + ") IS NOT NULL AND LOWER(e." + fieldName + ") ~ LOWER(?);";
        }

        JsonArray params = new JsonArray().add(query);

        Sql.getInstance().prepared(sqlQuery, params, SqlResult.validResultHandler(handler));
    }

    public void getNextPageItems(String scroll_id, Handler<Either<String, JsonObject>> handler) {
        getPageItems(scroll_id, handler);
    }



    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @return id_basket_equipment : ids of baskets who contains the equipment
     *         a sequence allocation for each option to create
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            either = new Either.Right<>(formatResults (event.body().getJsonArray("results")));
        } else {
            LOGGER.error("An error occurred when launching transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

    private static JsonObject formatResults(JsonArray result){
        JsonObject returns = new JsonObject();
        for (int i=0; i<result.size() ; i++) {
            JsonObject object = result.getJsonObject(i);
            String fields = object.getJsonArray("fields").getString(0);
            if ("id_basket_equipments".equals(fields)) {
                returns.put(fields, object.getJsonArray("results"));
            } else {
                returns.put(fields, (Number) ((object.getJsonArray("results").getJsonArray(0))).getInteger(0));
            }
        }
        return returns;
    }
    private static JsonObject getBasketIds(Number id) {
        String query = "SELECT id id_basket_equipments " +
                "      FROM " + Crre.crreSchema + ".basket_equipment " +
                "    where id_equipment = ? ; ";
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put(ACTION, PREPARED);
    }
    private static JsonObject getOptionsSequences(int i) {
        String query = "select nextval('" + Crre.crreSchema + ".equipment_option_id_seq') as id"+i;
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray())
                .put(ACTION, PREPARED);
    }
    /**
     * Returns an equipment creation statement
     *
     * @param id        equipment id
     * @param equipment equipment to create
     * @return equipment creation statement
     */
    private JsonObject getEquipmentCreationStatement(Number id, JsonObject equipment) {
        String insertEquipmentQuery =
                "INSERT INTO " + Crre.crreSchema + ".equipment(id, name, summary, description, price, id_tax," +
                        " image, id_contract, status, technical_specs, reference, option_enabled, price_editable) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?, ?, ?) RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(equipment.getString("name"))
                .add(equipment.getString("summary"))
                .add(equipment.getString("description"))
                .add(equipment.getDouble("price"))
                .add(equipment.getInteger("id_tax"))
                .add(equipment.getString("image"))
                .add(equipment.getInteger("id_contract"))
                .add(equipment.getString("status"))
                .add(equipment.getJsonArray("technical_specs"))
                .add(equipment.getString("reference"))
                .add(equipment.getBoolean("option_enabled"))
                .add(equipment.getBoolean("price_editable"));

        return new JsonObject()
                .put(STATEMENT, insertEquipmentQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Create an option for an equipment
     * @param id of the equipment
     * @param option
     * @return Insert statement
     */
    private JsonObject createEquipmentOptionRelationshipStatement(Number id, JsonObject option, Number optionId) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".equipment_option" +
                        "(id, amount, required, id_equipment, id_option) " +
                        "VALUES (?, ?, ?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(optionId)
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(id)
                .add(option.getInteger("id_option"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }
    /**
     * Create an option for an equipment
     * @param id of the equipment
     * @param option
     * @return Insert statement
     */
    private JsonObject getEquipmentOptionRelationshipStatement(Number id, JsonObject option) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Crre.crreSchema + ".equipment_option" +
                        "( amount, required, id_equipment, id_option) " +
                        "VALUES (?, ?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(id)
                .add(option.getInteger("id_option"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }


    /**
     * Update an option for an equipment
     * @param option the option
     * @return Insert statement
     */
    private static JsonObject updateEquipmentOptionRelationshipStatement( JsonObject option) {
        String insertTagEquipmentRelationshipQuery =
                "UPDATE " + Crre.crreSchema + ".equipment_option " +
                        "SET amount=?, required=?, id_option = ?" +
                        "WHERE id=?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(option.getInteger("id_option"))
                .add(option.getInteger("id"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * add a required option for all equipments in a basket
     * @return Insert statement
     */
    private static JsonObject addRequiredOptionToBasketStatement(JsonArray idsBasket, Number optionId) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder insertQuery = new StringBuilder().append("INSERT INTO ").append(Crre.crreSchema).append(".basket_option( ")
                .append(" id_basket_equipment, id_option) " )
                .append( " VALUES ");
        for (int i=0; i<idsBasket.size(); i++){
            insertQuery.append("( ?, ?)");
            insertQuery.append( i == idsBasket.size()-1 ? "; " : ", ");

            params.add((idsBasket.getJsonArray(i)).getInteger(0))
                    .add( optionId );
        }



        return new JsonObject()
                .put(STATEMENT, insertQuery.toString())
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }
    /**
     * Returns the update statement.
     *
     * @param id        resource Id
     * @param equipment equipment to update
     * @return Update statement
     */
    private JsonObject getEquipmentUpdateStatement(Number id, JsonObject equipment) {
        String query = "UPDATE " + Crre.crreSchema + ".equipment SET " +
                "name = ?, summary = ?, description = ?, price = ?, id_tax = ?, image = ?, " +
                "status = ?, technical_specs = to_json(?::text), " +
                "catalog_enabled = ?, option_enabled = ?, price_editable = ?, reference = ? " +
                "WHERE id = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(equipment.getString("name"))
                .add(equipment.getString("summary"))
                .add(equipment.getString("description"))
                .add(equipment.getDouble("price"))
                .add(equipment.getInteger("id_tax"))
                .add(equipment.getString("image"))
                .add(equipment.getString("status"))
                .add(equipment.getJsonArray("technical_specs"))
                .add(equipment.getBoolean("catalog_enabled"))
                .add(equipment.getBoolean("option_enabled"))
                .add(equipment.getBoolean("price_editable"))
                .add(equipment.getString("reference"))
                .add(id);

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete options of an equipment
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + ".equipment_option " +
                " WHERE id in "+ Sql.listPrepared(deletedOptions.getList());
        return addParamsDeletion(deletedOptions, value, query);
    }
    /**
     * Delete options of an equipment from baskets
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsBasketRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + ".basket_option " +
                " WHERE id_option in "+ Sql.listPrepared(deletedOptions.getList());
        return addParamsDeletion(deletedOptions, value, query);
    }

    private JsonObject addParamsDeletion(JsonArray deletedOptions, JsonArray value, String query) {
        for (int i = 0; i < deletedOptions.size(); i++) {
            value.add((deletedOptions.getJsonObject(i)).getInteger("id"));
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete options of  equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsOptionsRelationshipDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".equipment_option " +
                " WHERE id_equipment in " +
                Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".equipment " +
                " WHERE id in " +
                Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }


    public void listSubjects(final Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT name " +
                "FROM " + Crre.crreSchema + ".subject";
        sql.raw(query, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                handler.handle(event);
            }
        }));
    }


    public void listGrades(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT name " +
                "FROM " + Crre.crreSchema + ".grade";
        sql.raw(query, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                handler.handle(event);
            }
        }));
    }

}

