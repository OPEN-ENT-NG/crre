package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.exception.ImportPurseException;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.Purse;
import fr.openent.crre.model.PurseImport;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DefaultPurseService implements PurseService {
    @Override
    public Future<PurseImport> launchImport(JsonObject statementsValues, List<String> uaiErrorList) {
        Promise<PurseImport> promise = Promise.promise();
        List<TransactionElement> statements = new ArrayList<>();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        for (String field : fields) {
            JsonObject values = statementsValues.getJsonObject(field);
            Purse purse = values.getJsonObject(Field.PURSES) != null ? IModelHelper.toModel(values.getJsonObject(Field.PURSES), Purse.class) : new Purse();
            statements.add(getImportStatementStudent(field,
                    values.getInteger("second"), values.getInteger("premiere"), values.getInteger("terminale"),
                    values.getBoolean("pro")));
            if (purse != null) {
                statements.addAll(getImportStatementAmount(purse));
            }
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

    @Override
    public void getPursesStudentsAndLicences(List<String> ids, Handler<Either<String, JsonArray>> handler) {
        String query = getQueryPursesAndLicences();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        if (ids.size() > 0) {
            query += "AND licences.id_structure IN " + Sql.listPrepared(ids.toArray());
            for (String id : ids) {
                params.add(id);
            }
        }

        query += " ORDER BY name ";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private String getQueryPursesAndLicences() {
        return "SELECT DISTINCT COALESCE(licences.id_structure, purse.id_structure, students.id_structure) AS id_structure, structure.name, " +
                "ROUND(purse.amount::numeric,2)::double precision AS amount, ROUND(purse.initial_amount::numeric,2)::double precision AS initial_amount, " +
                "ROUND(purse.consumable_amount::numeric,2)::double precision AS consumable_amount, ROUND(purse.consumable_initial_amount::numeric,2)::double precision AS consumable_initial_amount, " +
                "seconde, premiere, terminale, pro, " +
                "licences.amount as licence_amount, licences.initial_amount as licence_initial_amount, " +
                "licences.consumable_amount as consumable_licence_amount, " +
                "licences.consumable_initial_amount as consumable_licence_initial_amount " +
                "FROM " + Crre.crreSchema + ".purse " +
                "FULL OUTER JOIN " + Crre.crreSchema + ".students ON students.id_structure = purse.id_structure " +
                "FULL OUTER JOIN " + Crre.crreSchema + ".licences ON purse.id_structure = licences.id_structure " +
                "JOIN " + Crre.crreSchema + ".structure ON structure.id_structure = licences.id_structure " +
                "OR structure.id_structure = purse.id_structure " +
                "OR structure.id_structure = students.id_structure " +
                "WHERE (purse.initial_amount IS NOT NULL OR purse.consumable_initial_amount IS NOT NULL OR " +
                "licences.initial_amount <> 0 OR licences.consumable_initial_amount <> 0) ";
    }

    @Override
    public void getPursesStudentsAndLicences(Integer page, JsonArray idStructures, Handler<Either<String, JsonArray>> handler) {
        String query = getQueryPursesAndLicences();

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        if (idStructures != null && !idStructures.isEmpty()) {
            query += "AND (purse.id_structure IN " + Sql.listPrepared(idStructures) + " OR licences.id_structure IN " + Sql.listPrepared(idStructures) + ") ";
            for (int i = 0; i < idStructures.size(); i++) {
                params.add(idStructures.getString(i));
                params.add(idStructures.getString(i));
            }
        }

        query += " ORDER BY name ";

        if (page != null) {
            query += " OFFSET ? LIMIT ? ";
            Integer PAGE_SIZE = 30;
            params.add(PAGE_SIZE * page);
            params.add(PAGE_SIZE);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String id_structure, JsonObject purse, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(getImportStatementAmount(id_structure, purse.getDouble("initial_amount"), "purse",
                false, false, true));
        statements.add(getImportStatementAmount(id_structure, purse.getDouble("consumable_initial_amount"), "purse",
                true, false, true));
        statements.add(getImportStatementAmount(id_structure, purse.getInteger("licence_initial_amount"), "licences",
                false, false, true));
        statements.add(getImportStatementAmount(id_structure, purse.getInteger("consumable_licence_initial_amount"), "licences",
                true, false, true));
        handleSQLStatements(handler, statements);
    }

    private void handleSQLStatements(Handler<Either<String, JsonObject>> handler, JsonArray statements) {
        Sql.getInstance().transaction(statements, message -> {
            if (message.body().containsKey(Field.STATUS) &&
                    Field.OK.equals(message.body().getString(Field.STATUS))) {
                handler.handle(new Either.Right<>(
                        new JsonObject().put(Field.STATUS, Field.OK)));
            } else {
                handler.handle(new Either.Left<>
                        ("crre.statements.error"));
            }
        });
    }

    private List<TransactionElement> getImportStatementAmount(Purse purse) {
        List<TransactionElement> statements = new ArrayList<>();
        if (!purse.getCreditsConsumable().isEmpty()) {
            statements.add(this.insertAmount(purse, true));
        }

        if (!purse.getCredits().isEmpty()) {
            statements.add(this.insertAmount(purse, false));
        }
        return statements;
    }

    private TransactionElement insertAmount(Purse purse, Boolean consumable) {
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
                        "consumable_amount = " + Field.PURSE + ".consumable_amount + ? + MIN(?, " + Field.PURSE + ".consumable_initial_amount - " +
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
                statement += "SET consumable_amount = " + Field.PURSE + ".consumable_amount + ? ";
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
                statement += "SET amount = " + Field.PURSE + ".amount + ? ";
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
