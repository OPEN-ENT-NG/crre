package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.exception.ImportPurseException;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.*;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultPurseService implements PurseService {
    private static final Logger log = LoggerFactory.getLogger(DefaultPurseService.class);
    private final StructureService structureService;

    public DefaultPurseService(ServiceFactory serviceFactory) {
        this.structureService = serviceFactory.getStructureService();
    }

    @Override
    public Future<PurseImport> launchImport(JsonObject statementsValues, List<String> uaiErrorList) {
        Promise<PurseImport> promise = Promise.promise();
        List<TransactionElement> statements = new ArrayList<>();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        for (String field : fields) {
            JsonObject values = statementsValues.getJsonObject(field);
            Optional<PurseImportElement> purse = IModelHelper.toModel(values.getJsonObject(Field.PURSES), PurseImportElement.class);
            statements.add(getImportStatementStudent(field,
                    values.getInteger("second"), values.getInteger("premiere"), values.getInteger("terminale"),
                    values.getBoolean("pro")));
            purse.ifPresent(value -> statements.addAll(getImportStatementAmount(value)));
            if (values.getInteger("licence") != -1) {
                statements.add(getImportStatementAmount(field,
                        values.getInteger("licence"), "licences", false, true, false));
            }
            if (values.getInteger("consumable_licence") != -1) {
                statements.add(getImportStatementAmount(field,
                        values.getInteger("consumable_licence"), "licences", true, true, false));
            }
        }

        if (statements.size() > 0) {
            String errorMessage = String.format("[CRRE@%s::launchImport] Failed to import purse by CSV", this.getClass().getSimpleName());
            TransactionHelper.executeTransaction(statements, errorMessage)
                    .onSuccess(importResult -> promise.complete())
                    .onFailure(promise::fail);
        } else {
            promise.fail(new ImportPurseException("crre.error.message.statements.empty"));
        }
        return promise.future();
    }

    private Future<List<PurseModel>> getPursesFromSql(Integer page, List<String> idStructureList) {
        Promise<List<PurseModel>> promise = Promise.promise();

        StringBuilder query = new StringBuilder().append("SELECT * FROM ").append(Crre.crreSchema).append(".purse")
                .append(" INNER JOIN ").append(Crre.crreSchema).append(".structure ON structure.id_structure = purse.id_structure");


        JsonArray params = new JsonArray();

        if (idStructureList != null && !idStructureList.isEmpty()) {
            query.append(" WHERE purse.id_structure IN ").append(Sql.listPrepared(idStructureList));
            params.addAll(new JsonArray(new ArrayList<>(idStructureList)));
        }

        if (page != null) {
            query.append(" OFFSET ? LIMIT ? ");
            Integer PAGE_SIZE = 30;
            params.add(PAGE_SIZE * page);
            params.add(PAGE_SIZE);
        }

        String messageError = String.format("[CRRE@%s::getPursesStudentsAndLicences] Fail to get purse", this.getClass().getSimpleName());

        Sql.getInstance().prepared(String.valueOf(query), params, SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, PurseModel.class, messageError)));

        return promise.future();
    }

    @Override
    public Future<List<PurseModel>> searchPursesByUAI(Integer page, String query) {
        Promise<List<PurseModel>> promise = Promise.promise();

        Map<String, Object> params = new HashMap<>();
        this.structureService.searchStructureByNameUai(query)
                .compose(structureNeo4jModelList -> {
                    params.put(Field.STRUCTURES, structureNeo4jModelList);
                    List<String> idStructureList = structureNeo4jModelList.stream()
                            .map(StructureNeo4jModel::getId)
                            .collect(Collectors.toList());
                    return this.getPursesFromSql(page, idStructureList);
                })
                .onSuccess(purseModelList -> {
                    List<StructureNeo4jModel> structureNeo4jModelList = (List<StructureNeo4jModel>) params.getOrDefault(Field.STRUCTURES, null);
                    purseModelList.forEach(purseModel -> structureNeo4jModelList.stream()
                            .filter(structureNeo4jModel -> structureNeo4jModel.getId().equals(purseModel.getIdStructure()))
                            .findFirst()
                            .ifPresent(purseModel::setStructureNeo4jModel));

                    promise.complete(purseModelList.stream()
                            .filter(purseModel -> purseModel.getStructureNeo4jModel() != null)
                            .collect(Collectors.toList()));
                })
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::searchPursesByUAI] Fail to get purse %s:%s", this.getClass().getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    @Override
    public Future<List<PurseModel>> getPurses(Integer page, List<String> idStructureList) {
        Promise<List<PurseModel>> promise = Promise.promise();

        Map<String, Object> params = new HashMap<>();
        this.getPursesFromSql(page, idStructureList)
                .compose(purseModelList -> {
                    params.put(Field.PURSES, purseModelList);
                    List<String> searchStructureIdList = purseModelList.stream()
                            .map(PurseModel::getIdStructure)
                            .distinct()
                            .collect(Collectors.toList());
                    return this.structureService.getStructureNeo4jById(searchStructureIdList);
                })
                .onSuccess(structureNeo4jModelList -> {
                    List<PurseModel> purseModelList = (List<PurseModel>) params.get(Field.PURSES);
                    purseModelList.forEach(purseModel -> structureNeo4jModelList.stream()
                            .filter(structureNeo4jModel -> structureNeo4jModel.getId().equals(purseModel.getIdStructure()))
                            .findFirst()
                            .ifPresent(purseModel::setStructureNeo4jModel));

                    promise.complete(purseModelList.stream()
                            .filter(purseModel -> purseModel.getStructureNeo4jModel() != null)
                            .collect(Collectors.toList()));
                })
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::getPursesStudentsAndLicences] Fail to get purse %s:%s", this.getClass().getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    @Override
    public Future<Void> update(String idStructure, JsonObject purse) {
        Promise<Void> promise = Promise.promise();

        List<TransactionElement> statements = new ArrayList<>();
        statements.add(incrementAddedInitialAmountFromNewValue(false, purse.getDouble(Field.INITIAL_AMOUNT), idStructure));
        statements.add(incrementAddedInitialAmountFromNewValue(true, purse.getDouble(Field.CONSUMABLE_INITIAL_AMOUNT), idStructure));

        statements.add(getImportStatementAmount(idStructure, purse.getDouble(Field.INITIAL_AMOUNT), Field.PURSE,
                false, false, true));
        statements.add(getImportStatementAmount(idStructure, purse.getDouble(Field.CONSUMABLE_INITIAL_AMOUNT), Field.PURSE,
                true, false, true));
        String errorMessage = "[CRRE@%s::update] Fail to update purse:";
        TransactionHelper.executeTransaction(statements, errorMessage)
                .onSuccess(result -> promise.complete())
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public TransactionElement incrementAddedInitialAmountFromNewValue(boolean consumable, Double newValue, String structureId) {
        StringBuilder query = new StringBuilder().append("UPDATE crre.purse SET ")
                .append(consumable ? "added_consumable_initial_amount" : "added_initial_amount")
                .append(" =  ")
                .append(consumable ? "added_consumable_initial_amount" : "added_initial_amount")
                .append(" + ? - ")
                .append(consumable ? "consumable_initial_amount" : "initial_amount")
                .append(" WHERE id_structure = ?");
        JsonArray params = new JsonArray()
                .add(newValue)
                .add(structureId);

        return new TransactionElement(query.toString(), params);
    }

    private List<TransactionElement> getImportStatementAmount(PurseImportElement purse) {
        List<TransactionElement> statements = new ArrayList<>();
        if (!purse.getCreditsConsumable().isEmpty()) {
            if (purse.getCreditsConsumable().getNewValue() != null) {
                statements.add(this.setAddedInitialAmount(purse.getIdStructure(), true, 0));
            } else if (purse.getCreditsConsumable().getAddValue() != null) {
                statements.add(this.addAddedInitialAmount(purse.getIdStructure(), true, purse.getCreditsConsumable().getAddValue()));
            }
            statements.add(this.insertAmount(purse, true));
        }

        if (!purse.getCredits().isEmpty()) {
            if (purse.getCredits().getNewValue() != null) {
                statements.add(this.setAddedInitialAmount(purse.getIdStructure(), false, 0));
            } else if (purse.getCredits().getAddValue() != null) {
                statements.add(this.addAddedInitialAmount(purse.getIdStructure(), false, purse.getCredits().getAddValue()));
            }
            statements.add(this.insertAmount(purse, false));
        }
        return statements;
    }

    private TransactionElement setAddedInitialAmount(String idStructure, boolean consumable, double amount) {
        String query = "UPDATE " + Crre.crreSchema + ".purse SET " +
                (consumable ? "added_consumable_initial_amount" : "added_initial_amount") +
                " = ? WHERE id_structure = ?";
        JsonArray params = new JsonArray()
                .add(amount)
                .add(idStructure);

        return new TransactionElement(query, params);
    }

    private TransactionElement addAddedInitialAmount(String idStructure, boolean consumable, double amount) {
        StringBuilder query = new StringBuilder()
                .append("UPDATE ").append(Crre.crreSchema).append(".purse SET ")
                .append(consumable ? "added_consumable_initial_amount" : "added_initial_amount")
                .append(" = ? + ").append(consumable ? "added_consumable_initial_amount" : "added_initial_amount")
                .append(" WHERE id_structure = ?");
        JsonArray params = new JsonArray()
                .add(amount)
                .add(idStructure);

        return new TransactionElement(query.toString(), params);
    }

    private TransactionElement insertAmount(PurseImportElement purse, Boolean consumable) {
        String statement = "INSERT INTO " + Crre.crreSchema + "." + Field.PURSE + " (id_structure, ";
        JsonArray params = new JsonArray();
        if (consumable) {
            statement += "consumable_amount, consumable_initial_amount) " +
                    "VALUES (?,?,?) " +
                    "ON CONFLICT (id_structure) DO UPDATE ";
            params.add(purse.getIdStructure())
                    .add(purse.getCreditsConsumable().getDefaultValue())
                    .add(purse.getCreditsConsumable().getDefaultValue());
            if (purse.getCreditsConsumable().getAddValue() != null && purse.getCreditsConsumable().getRefundValue() != null &&
                    purse.getCreditsConsumable().getNewValue() != null) {
                statement += "SET consumable_initial_amount = ?, " +
                        "consumable_amount = ? ";
                Double newValue = purse.getCreditsConsumable().getNewValue() + purse.getCreditsConsumable().getAddValue();
                params.add(newValue).add(newValue);
            } else if (purse.getCreditsConsumable().getAddValue() != null && purse.getCreditsConsumable().getRefundValue() != null) {
                statement += "SET consumable_initial_amount = " + Field.PURSE + ".consumable_initial_amount + ?, " +
                        "consumable_amount = " + Field.PURSE + ".consumable_amount + ? + LEAST(?, " + Field.PURSE + ".consumable_initial_amount - " +
                        Field.PURSE + ".consumable_amount) ";
                params.add(purse.getCreditsConsumable().getAddValue())
                        .add(purse.getCreditsConsumable().getAddValue())
                        .add(purse.getCreditsConsumable().getRefundValue());
            } else if (purse.getCreditsConsumable().getNewValue() != null) {
                statement += "SET consumable_initial_amount = ?, " +
                        "consumable_amount = ? ";
                Double newValue = purse.getCreditsConsumable().getNewValue();
                if (purse.getCreditsConsumable().getAddValue() != null) {
                    newValue += purse.getCreditsConsumable().getAddValue();
                }
                params.add(newValue).add(newValue);
            } else if (purse.getCreditsConsumable().getRefundValue() != null) {
                statement += "SET consumable_amount = " + Field.PURSE + ".consumable_amount + LEAST(?, " + Field.PURSE + ".consumable_initial_amount - " +
                        Field.PURSE + ".consumable_amount) ";
                params.add(purse.getCreditsConsumable().getRefundValue());
            } else if (purse.getCreditsConsumable().getAddValue() != null) {
                statement += "SET consumable_amount = " + Field.PURSE + ".consumable_amount + ?, " +
                        "consumable_initial_amount = " + Field.PURSE + ".consumable_initial_amount + ? ";
                params.add(purse.getCreditsConsumable().getAddValue()).add(purse.getCreditsConsumable().getAddValue());
            }
        } else {
            statement += "amount, initial_amount) " +
                    "VALUES (?,?,?) " +
                    "ON CONFLICT (id_structure) DO UPDATE ";
            params.add(purse.getIdStructure())
                    .add(purse.getCredits().getDefaultValue())
                    .add(purse.getCredits().getDefaultValue());
            if (purse.getCredits().getAddValue() != null && purse.getCredits().getRefundValue() != null &&
                    purse.getCredits().getNewValue() != null) {
                statement += "SET initial_amount = ?, " +
                        "amount = ? ";
                Double newValue = purse.getCredits().getNewValue() + purse.getCredits().getAddValue();
                params.add(newValue).add(newValue);
            } else if (purse.getCredits().getAddValue() != null && purse.getCredits().getRefundValue() != null) {
                statement += "SET initial_amount = " + Field.PURSE + ".initial_amount + ?, " +
                        "amount = " + Field.PURSE + ".amount + ? + LEAST(?, " + Field.PURSE + ".initial_amount - " +
                        Field.PURSE + ".amount) ";
                params.add(purse.getCredits().getAddValue())
                        .add(purse.getCredits().getAddValue())
                        .add(purse.getCredits().getRefundValue());
            } else if (purse.getCredits().getNewValue() != null) {
                statement += "SET initial_amount = ?, " +
                        "amount = ? ";
                Double newValue = purse.getCredits().getNewValue();
                if (purse.getCredits().getAddValue() != null) {
                    newValue += purse.getCredits().getAddValue();
                }
                params.add(newValue).add(newValue);
            } else if (purse.getCredits().getRefundValue() != null) {
                statement += "SET amount = " + Field.PURSE + ".amount + LEAST(?, " + Field.PURSE + ".initial_amount - " +
                        Field.PURSE + ".amount) ";
                params.add(purse.getCredits().getRefundValue());
            } else if (purse.getCredits().getAddValue() != null) {
                statement += "SET amount = " + Field.PURSE + ".amount + ?, " +
                        "initial_amount = " + Field.PURSE + ".initial_amount + ? ";
                params.add(purse.getCredits().getAddValue()).add(purse.getCredits().getAddValue());
            }
        }
        statement += "WHERE " + Field.PURSE + ".id_structure = ?;";
        params.add(purse.getIdStructure());
        return new TransactionElement(statement, params);
    }

    private TransactionElement getImportStatementAmount(String structureId, double amount, String table, Boolean consumable,
                                                        Boolean isOverrideAmount, Boolean update) {
        String statement = "INSERT INTO " + Crre.crreSchema + "." + table + " (id_structure, ";
        if (consumable) {
            statement += "consumable_amount, consumable_initial_amount) " +
                    "VALUES (?,?,?) " +
                    "ON CONFLICT (id_structure) DO UPDATE ";
            if (update) {
                statement += "SET consumable_amount = " + table + ".consumable_amount + ( ? - " + table + ".consumable_initial_amount), " +
                        "consumable_initial_amount = ? ";
            } else if (!isOverrideAmount) {
                statement += "SET consumable_initial_amount = " + table + ".consumable_initial_amount + ?, " +
                        "consumable_amount = " + table + ".consumable_amount + ? ";
            } else {
                statement += "SET consumable_initial_amount = ?, " +
                        "consumable_amount = ? ";
            }
        } else {
            statement += "amount, initial_amount) " +
                    "VALUES (?,?,?) " +
                    "ON CONFLICT (id_structure) DO UPDATE ";
            if (update) {
                statement += "SET amount = " + table + ".amount + ( ? - " + table + ".initial_amount), initial_amount = ? ";
            } else if (!isOverrideAmount) {
                statement += "SET initial_amount = " + table + ".initial_amount + ?, " +
                        "amount = " + table + ".amount + ? ";
            } else {
                statement += "SET initial_amount = ?, " +
                        "amount = ? ";
            }
        }
        statement += "WHERE " + table + ".id_structure = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(structureId);
        if (table.equals("licences")) {
            params.add((int) amount)
                    .add((int) amount)
                    .add((int) amount)
                    .add((int) amount);
        } else {
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
            DecimalFormat df2 = new DecimalFormat("#.##", dfs);
            double amountToInsert = Double.parseDouble(df2.format(amount));
            params.add(amountToInsert)
                    .add(amountToInsert)
                    .add(amountToInsert)
                    .add(amountToInsert);
        }
        params.add(structureId);
        return new TransactionElement(statement, params);
    }

    private TransactionElement getImportStatementStudent(String structureId, int second, int premiere, int terminale, boolean pro) {

        String statement = "INSERT INTO " + Crre.crreSchema + ".students (id_structure, seconde, premiere, terminale, pro) " +
                "VALUES (?,?,?,?,?) " +
                "ON CONFLICT (id_structure) DO UPDATE " +
                "SET seconde = ?, " +
                "premiere = ?, " +
                "terminale = ?, " +
                "pro = ? " +
                "WHERE students.id_structure = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(structureId)
                .add(second)
                .add(premiere)
                .add(terminale)
                .add(pro)
                .add(second)
                .add(premiere)
                .add(terminale)
                .add(pro)
                .add(structureId);
        return new TransactionElement(statement, params);
    }

    @Override
    public void updatePurseAmount(Double price, String idStructure, String operation, Boolean consumable,
                                  Handler<Either<String, JsonObject>> handler) {
        TransactionElement transactionElement = this.getTransactionUpdatePurseAmount(price, idStructure, operation, consumable);

        Sql.getInstance().prepared(transactionElement.getQuery(), transactionElement.getParams(),
                new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public TransactionElement getTransactionUpdatePurseAmount(Double price, String idStructure, String operation, Boolean consumable) {
        final double cons = 100.0;
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".purse " +
                "SET " + (consumable ? "consumable_" : "") + "amount = ROUND((" +
                (consumable ? "consumable_" : "") + "amount " + operation + " ? )::numeric ,2)::double precision " +
                "WHERE id_structure = ? ;";

        JsonArray params = new JsonArray()
                .add(Math.round(price * cons) / cons)
                .add(idStructure);

        return new TransactionElement(updateQuery, params);
    }
}
