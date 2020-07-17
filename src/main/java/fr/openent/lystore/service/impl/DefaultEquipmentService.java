package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.EquipmentService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);
    private static final String STATEMENT = "statement" ;
    private static final String VALUES = "values" ;
    private static final String ACTION = "action" ;
    private static final String PREPARED = "prepared" ;

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
        String filter = "", q;
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                q = filters.get(i);
                if (i > 0) {
                    filter += "AND ";
                }

                filter += "(LOWER(equip.name) ~ LOWER(?) OR LOWER(equip.reference) ~ LOWER(?) OR LOWER(supplier.name) ~ LOWER(?) OR LOWER(contract.name) ~ LOWER(?)) ";
            }
        }

        return filter;
    }

    public void listEquipments(Integer page, String order, Boolean reverse, List<String> filters, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();

        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter);
            }
        }

        String query = "SELECT equip.reference, equip.name, equip.id, equip.status,  equip.price, supplier.name as supplier_name, contract.name as contract_name, tax.value as tax_amount " +
                "FROM " + Lystore.lystoreSchema + ".equipment equip " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax ON tax.id = equip.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (contract.id = equip.id_contract) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                getTextFilter(filters) +
                "ORDER by " + getSqlOrderValue(order) + " " + getSqlReverseString(reverse);

        if (page != null) {
            query += " LIMIT " + Lystore.PAGE_SIZE + " OFFSET ?";
            params.add(Lystore.PAGE_SIZE * page);
        }
        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }
    public void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT equip.*, supplier.name as supplier_name, contract.name as contract_name, tax.value as tax_amount, equipment_type.name as nametype, array_to_json( " +
                "(SELECT array_agg(tag.*) " +
                "FROM " + Lystore.lystoreSchema + ".equipment " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".rel_equipment_tag ON (equipment.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tag ON rel_equipment_tag.id_tag = tag.id" +
                " WHERE equip.id = rel_equipment_tag.id_equipment)) as tags, " +
                "array_to_json(array_agg(opts.*)) as options " +
                "FROM  " + Lystore.lystoreSchema + ".equipment equip    " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".equipment_type ON (equipment_type.id = equip.id_type) " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".equipment_option ON (equip.id = equipment_option.id_equipment) " +
                "LEFT JOIN ( " +
                "SELECT equipment.id as id_equipment, equipment.reference, equipment.id_type, equipment_option.id, equipment_option.id_option, equipment.name, equipment.price, equipment_option.amount, " +
                "equipment_option.required, tax.value as tax_amount, equipment_option.id_equipment as master_equipment, equipment_type.name as nametype " +
                "FROM " + Lystore.lystoreSchema + ".equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax  ON (equipment.id_tax = tax.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_option  ON (equipment_option.id_option = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_type ON (equipment_type.id = equipment.id_type) " +
                ") opts ON (equipment_option.id_option = opts.id_equipment AND opts.master_equipment = equip.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax ON tax.id = equip.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (contract.id = equip.id_contract) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                "WHERE equip.id = ? " +
                "GROUP BY (equip.id, tax.id,equipment_type.name, supplier.name, contract.name, tax.value) " +
                "ORDER by equip.name ASC " +
                "LIMIT 50 OFFSET 0";


        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idEquipment), SqlResult.validResultHandler(handler));
    }

    @Override
    public void listAllEquipments(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT e.*,contract_type.name as contract_type_name, equipment_type.name as nametype, tax.value tax_amount, array_to_json(array_agg(DISTINCT opts)) as options, contract.name as contract_name, array_to_json(array_agg(DISTINCT  rel_equipment_tag.id_tag)) tags " +
                "FROM " + Lystore.lystoreSchema + ".equipment e LEFT JOIN ( " +
                "SELECT option.*, equipment.name, equipment.price, tax.value tax_amount " +
                "FROM " + Lystore.lystoreSchema + ".equipment_option option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON (option.id_option = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = equipment.id_tax " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (e.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +

                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_type on equipment_type.id = e.id_type " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (e.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (" +
                "rel_group_campaign.id_tag = rel_equipment_tag.id_tag " +
                "AND rel_group_campaign.id_campaign = ? " +
                ((idStructure != null) ?
                        " AND rel_group_campaign.id_structure_group IN (  " +
                                " SELECT structure_group.id FROM  " + Lystore.lystoreSchema + ".structure_group  " +
                                " INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON rel_group_structure.id_structure_group = structure_group.id  " +
                                " WHERE rel_group_structure.id_structure = ? )"
                        : ""
                ) + ")and e.catalog_enabled = true AND e.status != 'OUT_OF_STOCK' " +
                "GROUP BY (e.id, tax.id , nametype, contract.name,contract_type.name )";

        JsonArray values = new JsonArray().add(idCampaign);
        if (idStructure != null)
            values.add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }


    public void listEquipments(Integer idCampaign, String idStructure, Integer page, List<String> filters,
                               Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray().add(idCampaign).add(idStructure);
        String queryFilter = "";
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                queryFilter += "AND (lower(e.name) ~ lower(?) OR lower(contract.name) ~ lower(?))";
                values.add(filter).add(filter);
            }
        }
        String query = "SELECT e.*,contract_type.name as contract_type_name, equipment_type.name as nametype, tax.value tax_amount, array_to_json(array_agg(DISTINCT opts)) as options, contract.name as contract_name, array_to_json(array_agg(DISTINCT  rel_equipment_tag.id_tag)) tags " +
                "FROM " + Lystore.lystoreSchema + ".equipment e LEFT JOIN ( " +
                "SELECT option.*, equipment.name, equipment.price, tax.value tax_amount " +
                "FROM " + Lystore.lystoreSchema + ".equipment_option option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON (option.id_option = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = equipment.id_tax " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (e.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +

                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_type on equipment_type.id = e.id_type " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (e.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (" +
                "rel_group_campaign.id_tag = rel_equipment_tag.id_tag " +
                "AND rel_group_campaign.id_campaign = ? " +
                "AND rel_group_campaign.id_structure_group IN (" +
                "SELECT structure_group.id FROM " + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON rel_group_structure.id_structure_group = structure_group.id " +
                "WHERE rel_group_structure.id_structure = ?)) and e.catalog_enabled = true AND e.status != 'OUT_OF_STOCK' " + queryFilter +
                "GROUP BY (e.id, tax.id , nametype, contract.name,contract_type.name )";


        if (page != null) {
            query += " LIMIT " + Lystore.PAGE_SIZE + " OFFSET ?";
            values.add(Lystore.PAGE_SIZE * page);
        }

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void create(final JsonObject equipment, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getEquipmentCreationStatement(id, equipment));

                        JsonArray tags = equipment.getJsonArray("tags");
                        for (int i = 0; i < tags.size(); i++) {
                            statements.add(getEquipmentTagRelationshipStatement(id, tags.getInteger(i)));
                        }
                        JsonArray options = equipment.getJsonArray("optionsCreate");
                        for (int j = 0; j < options.size(); j++) {
                            statements.add(getEquipmentOptionRelationshipStatement(id, (JsonObject) options.getJsonObject(j)));
                        }
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                            }
                        });
                    } catch (ClassCastException e) {
                        LOGGER.error("An error occurred when casting tags ids", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    LOGGER.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }

    private String getImportTagFilter(String[] tags) {
        StringBuilder tagFilter = new StringBuilder("(");
        for (int j = 0; j < tags.length; j++) {
            tagFilter.append((j == tags.length - 1) ? "lower(?)" : "lower(?),");
        }
        tagFilter.append(")");

        return tagFilter.toString();
    }

    private JsonObject getInsertImportStatement(JsonObject equipment) {
        JsonArray params = new JsonArray();
        Double price = Double.parseDouble(equipment.getValue("price").toString().replace(",", "."));
        Double tax = Double.parseDouble(equipment.getValue("id_tax").toString().replace(",", "."));

        boolean referenceValue = !"".equals(equipment.getString("reference"));
        String[] tags = equipment.getString("name_tag").split(",");

        String query = "WITH equipmentId_rows AS (" +
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment(" + (referenceValue ? " reference, " : "") + "name, price, id_tax, warranty, catalog_enabled, id_contract, status, id_type, price_editable) " +
                "VALUES (" + (referenceValue ? "?," : "") + "?, ?, (SELECT id FROM " + Lystore.lystoreSchema + ".tax WHERE value = ? LIMIT 1), ?, ?, ?, ?, " +
                "(SELECT id FROM " + Lystore.lystoreSchema + ".equipment_type WHERE name = ?), ?) RETURNING id)" +
                "INSERT INTO " + Lystore.lystoreSchema + ".rel_equipment_tag (id_equipment, id_tag) " +
                "SELECT equipmentId_rows.id, tag.id FROM " + Lystore.lystoreSchema + ".tag, equipmentId_rows WHERE lower(name) IN " + getImportTagFilter(tags);

        if (referenceValue) {
            params.add(equipment.getString("reference"));
        }
        params.add(equipment.getString("name"));
        params.add(price);
        params.add(tax);
        params.add(equipment.getInteger("warranty"));
        params.add(equipment.getBoolean("catalog_enabled"));
        params.add(equipment.getInteger("id_contract"));
        params.add(equipment.getString("status"));
        params.add(equipment.getString("type"));
        params.add(equipment.getBoolean("price_editable"));
        for (String tag : tags) {
            params.add(tag.trim());
        }

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    private JsonObject getDeleteTagImportStatement(String reference) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                "USING " + Lystore.lystoreSchema + ".equipment " +
                "WHERE equipment.id = rel_equipment_tag.id_equipment " +
                "AND equipment.reference = ?;";
        JsonArray params = new JsonArray().add(reference);
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    private JsonObject getUpdateImportStatement(JsonObject equipment) {
        String[] tags = equipment.getString("name_tag").split(",");
        Double price = Double.parseDouble(equipment.getValue("price").toString().replace(",", "."));
        Double tax = Double.parseDouble(equipment.getValue("id_tax").toString().replace(",", "."));
        JsonArray params = new JsonArray();
        String query = "WITH equipmentId_rows AS (UPDATE " + Lystore.lystoreSchema + ".equipment " +
                "SET name = ?, price = ?, id_tax = (SELECT id FROM " + Lystore.lystoreSchema + ".tax WHERE value = ? LIMIT 1), warranty = ?," +
                "id_type = (SELECT id FROM " + Lystore.lystoreSchema + ".equipment_type WHERE name = ? LIMIT 1), catalog_enabled = ?, price_editable = ?, status = ?" +
                "WHERE equipment.reference = ? RETURNING id)" +
                "INSERT INTO " + Lystore.lystoreSchema + ".rel_equipment_tag (id_equipment, id_tag) " +
                "SELECT equipmentId_rows.id, tag.id FROM " + Lystore.lystoreSchema + ".tag, equipmentId_rows WHERE lower(name) IN " + getImportTagFilter(tags);

        params.add(equipment.getString("name"))
                .add(price)
                .add(tax)
                .add(equipment.getInteger("warranty"))
                .add(equipment.getString("type"))
                .add(equipment.getBoolean("catalog_enabled"))
                .add(equipment.getBoolean("price_editable"))
                .add(equipment.getString("status"))
                .add(equipment.getString("reference"));

        for (String tag : tags) {
            params.add(tag.trim());
        }

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    public void importEquipments(final JsonArray equipments, JsonArray referencesToUpdate, Handler<Either<String, JsonObject>> handler) {
        if (referencesToUpdate.size() > 0) {
            String matchReferencesQuery = "SELECT reference FROM " + Lystore.lystoreSchema + ".equipment  WHERE reference IN " + Sql.listPrepared(referencesToUpdate.getList());
            Sql.getInstance().prepared(matchReferencesQuery, referencesToUpdate, SqlResult.validResultHandler(event -> {
                if (event.isRight()) {
                    JsonArray values = event.right().getValue();
                    JsonObject o;
                    JsonArray refs = new JsonArray();
                    for (int h = 0; h < values.size(); h++) {
                        o = values.getJsonObject(h);
                        refs.add(o.getString("reference"));
                    }

                    launchImport(equipments, refs, handler);
                } else {
                    String message = "An error occurred when matching references to update";
                    LOGGER.error(message);
                    handler.handle(new Either.Left<String, JsonObject>(message));
                }
            }));
        } else {
            launchImport(equipments, new JsonArray(), handler);
        }
    }

    private void launchImport(JsonArray equipments, JsonArray references, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        JsonObject statementTag;
        JsonObject statementTax;

        for (int i = 0; i < equipments.size(); i++) {
            JsonObject equipment = equipments.getJsonObject(i);

            String[] tags = equipment.getString("name_tag").split(",");

            for (String tag : tags) {
                statementTag = new JsonObject();
                String insertNotExistentTagQuery = "INSERT INTO " + Lystore.lystoreSchema + ".tag (name, color) SELECT ?, '#0033cc' WHERE NOT EXISTS (SELECT 1 FROM " + Lystore.lystoreSchema + ".tag WHERE lower(name) = lower(?))";
                JsonArray tagValues = new JsonArray();
                tagValues.add(tag.trim()).add(tag.trim());

                statementTag.put(STATEMENT, insertNotExistentTagQuery);
                statementTag.put(VALUES, tagValues);
                statementTag.put(ACTION, PREPARED);

                statements.add(statementTag);
            }

            Double tax = Double.parseDouble(equipment.getValue("id_tax").toString().replace(",", "."));

            statementTax = new JsonObject();
            String insertNotExistentTaxQuery = "INSERT INTO " + Lystore.lystoreSchema + ".tax (value, name) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM " + Lystore.lystoreSchema + ".tax WHERE value = ?)";
            JsonArray taxValues = new JsonArray();
            taxValues.add(tax).add("Taxe " + tax + "%").add(tax);

            statementTax.put(STATEMENT, insertNotExistentTaxQuery);
            statementTax.put(VALUES, taxValues);
            statementTax.put(ACTION, PREPARED);

            statements.add(statementTax);

            if (references.contains(equipment.getString("reference"))) {
                statements.add(getDeleteTagImportStatement(equipment.getString("reference")));
                statements.add(getUpdateImportStatement(equipment));
            } else {
                statements.add(getInsertImportStatement(equipment));
            }
        }
        if (statements.size() > 0) {
            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    if (event.body().containsKey("status") && "ok".equals(event.body().getString("status"))) {
                        handler.handle(new Either.Right<>(new JsonObject().put("message", "Imported")));
                    } else {
                        String message = "An error occurred when handling equipment transaction";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<String, JsonObject>(message));
                    }
                }
            });
        } else {
            String message = "An error occurred when assembling the transaction";
            LOGGER.error(message);
            handler.handle(new Either.Left<String, JsonObject>(message));
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
                "FROM " + Lystore.lystoreSchema + ".equipment equip " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (contract.id = equip.id_contract) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                getTextFilter(filters);
        Sql.getInstance().prepared(query, params, event -> {
            if (!"ok".equals(event.body().getString("status"))) {
                handler.handle(new Either.Left<>("An error occurred when collecting equipment count"));
                return;
            }

            handler.handle(new Either.Right<>(new JsonObject().put("count", calculPagesNumber(event.body().getJsonArray("results").getJsonArray(0).getInteger(0)))));
        });
    }

    @Override
    public void getNumberPages(Integer idCampaign, String idStructure, List<String> filters, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray().add(idCampaign).add(idStructure);
        String queryFilter = "WHERE equipment.status != 'OUT_OF_STOCK' ";
        if (!filters.isEmpty()) {
            for (int i = 0; i < filters.size(); i++) {
                queryFilter += "AND lower(equipment.name) ~ lower(?) ";
                values.add(filters.get(i));
            }
        }
        String query = "SELECT count(equipment.id) " +
                "FROM " + Lystore.lystoreSchema + ".equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (equipment.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (rel_group_campaign.id_tag = rel_equipment_tag.id_tag) " +
                "AND rel_group_campaign.id_campaign = ? " +
                "AND rel_group_campaign.id_structure_group IN ( " +
                "SELECT structure_group.id " +
                "FROM " + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_structure.id_structure_group = structure_group.id) " +
                "WHERE rel_group_structure.id_structure = ? " +
                ")" + queryFilter;

        Sql.getInstance().prepared(query, values, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (!"ok".equals(event.body().getString("status"))) {
                    handler.handle(new Either.Left<>("An error occurred when collecting equipment count"));
                    return;
                }

                handler.handle(new Either.Right<>(new JsonObject().put("count", calculPagesNumber(event.body().getJsonArray("results").getJsonArray(0).getInteger(0)))));
            }
        });
    }


    private Integer calculPagesNumber(Integer count) {
        Integer pageCount = count / Lystore.PAGE_SIZE;
        pageCount += ((count % Lystore.PAGE_SIZE) != 0 ? 1 : 0);
        return pageCount;
    }

    @Override
    public void updateEquipment(final Integer id, JsonObject equipment,
                                final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentUpdateStatement(id, equipment))
                .add(getEquipmentTagRelationshipDeletion(id));
        JsonArray tags = equipment.getJsonArray("tags");

        for (int i = 0; i < tags.size(); i++) {
            statements.add(getEquipmentTagRelationshipStatement(id, tags.getInteger(i)));
        }

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
            }
        });
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
            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                }
            });
        } else {
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("id", id)));
        }
    }
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentsOptionsRelationshipDeletion(ids))
                .add(getEquipmentsDeletion(ids));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0)));
            }
        });
    }

    @Override
    public void setStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".equipment SET status = ? " +
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

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event));
            }
        });
    }

    @Override
    public void search(String query,  List<String> listFields, Handler<Either<String, JsonArray>> handler) {

        String sqlQuery = "SELECT e.id, e.name, e.summary, e.description, CAST(e.price AS NUMERIC) as price, t.value as tax_amount, t.id as id_tax, e.image, e.reference, e.warranty, " +
                                    "e.id_type, e.option_enabled, et.name as nameType "+
                "FROM " + Lystore.lystoreSchema + ".equipment as e " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax as t ON (e.id_tax = t.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_type as et ON (e.id_type = et.id) " +
                "WHERE e.status = 'AVAILABLE' AND e.option_enabled = true ";

        String fieldName=listFields.get(0);
        if (listFields.size()==1) {
            sqlQuery = sqlQuery + "AND LOWER(e." + fieldName + ") IS NOT NULL AND LOWER(e." + fieldName + ") ~ LOWER(?);";
        }

        JsonArray params = new JsonArray().add(query);

        Sql.getInstance().prepared(sqlQuery, params, SqlResult.validResultHandler(handler));
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
                "      FROM lystore.basket_equipment " +
                "    where id_equipment = ? ; ";
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put(ACTION, PREPARED);
    }
    private static JsonObject getOptionsSequences(int i) {
        String query = "select nextval('lystore.equipment_option_id_seq') as id"+i;
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
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment(id, name, summary, description, price, id_tax," +
                        " image, id_contract, status, technical_specs, warranty, reference, id_type, option_enabled, price_editable) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?, ?, ?, ?, ?) RETURNING id;";
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
                .add(equipment.getInteger("warranty"))
                .add(equipment.getString("reference"))
                .add(equipment.getInteger("id_type"))
                .add(equipment.getBoolean("option_enabled"))
                .add(equipment.getBoolean("price_editable"));

        return new JsonObject()
                .put(STATEMENT, insertEquipmentQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Returns an equipment tag relationship transaction statement
     *
     * @param id    equipment Id
     * @param tagId tag id
     * @return equipment tag relationship transaction statement
     */
    private JsonObject getEquipmentTagRelationshipStatement(Number id, Number tagId) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".rel_equipment_tag(id_equipment, id_tag) " +
                        "VALUES (?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(tagId);

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
    private JsonObject createEquipmentOptionRelationshipStatement(Number id, JsonObject option, Number optionId) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment_option" +
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
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment_option" +
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
                "UPDATE lystore.equipment_option " +
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
        StringBuilder insertQuery = new StringBuilder()
                .append("INSERT INTO lystore.basket_option( ")
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
        String query = "UPDATE " + Lystore.lystoreSchema + ".equipment SET " +
                "name = ?, summary = ?, description = ?, price = ?, id_tax = ?, image = ?, " +
                "id_contract = ?, status = ?, technical_specs = to_json(?::text), " +
                "id_type = ?, catalog_enabled = ?, option_enabled = ?, price_editable = ?, reference = ? " +
                "WHERE id = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(equipment.getString("name"))
                .add(equipment.getString("summary"))
                .add(equipment.getString("description"))
                .add(equipment.getDouble("price"))
                .add(equipment.getInteger("id_tax"))
                .add(equipment.getString("image"))
                .add(equipment.getInteger("id_contract"))
                .add(equipment.getString("status"))
                .add(equipment.getJsonArray("technical_specs"))
                .add(equipment.getInteger("id_type"))
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
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".equipment_option " +
                " WHERE id in "+ Sql.listPrepared(deletedOptions.getList());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.add((deletedOptions.getJsonObject(i)).getInteger("id"));
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }
    /**
     * Delete options of an equipment from baskets
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsBasketRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".basket_option " +
                " WHERE id_option in "+ Sql.listPrepared(deletedOptions.getList());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.add((deletedOptions.getJsonObject(i)).getInteger("id") );
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
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment_option ")
                .append(" WHERE id_equipment in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put(STATEMENT, query.toString())
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment ")
                .append(" WHERE id in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put(STATEMENT, query.toString())
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }
    private JsonObject getEquipmentTagRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                " WHERE id_equipment = ?;";

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put(ACTION, PREPARED);
    }
}
