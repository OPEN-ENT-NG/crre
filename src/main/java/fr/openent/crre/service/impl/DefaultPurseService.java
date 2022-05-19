package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultPurseService implements PurseService {
    private Boolean invalidDatas = false;

    @Override
    public void launchImport(JsonObject statementsValues, boolean invalidDatasPreviously,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        invalidDatas = invalidDatasPreviously;
        if (!invalidDatas) {
            for (String field : fields) {
                JsonObject values = statementsValues.getJsonObject(field);
                statements.add(getImportStatementStudent(field,
                        values.getInteger("second"), values.getInteger("premiere"), values.getInteger("terminale"),
                        values.getBoolean("pro")));
                if(values.getJsonObject("amount").getDouble("value") != -1) {
                    statements.add(getImportStatementAmount(field,
                            values.getJsonObject("amount").getDouble("value"), "purse",
                            false, values.getJsonObject("amount").getBoolean("isOverrideAmount"),false));
                }
                if(values.getJsonObject("consumable_amount").getDouble("value") != -1) {
                    statements.add(getImportStatementAmount(field,
                            values.getJsonObject("consumable_amount").getDouble("value"), "purse",
                            true, values.getJsonObject("consumable_amount").getBoolean("isOverrideAmount"),false));
                }
                if(values.getInteger("licence") != -1) {
                    statements.add(getImportStatementAmount(field,
                            values.getInteger("licence"), "licences", false, true));
                }
               if(values.getInteger("consumable_licence") != -1) {
                   statements.add(getImportStatementAmount(field,
                           values.getInteger("consumable_licence"), "licences", true, true));
               }
            }
        }
        if (invalidDatas) {
            handler.handle(new Either.Left<>
                    ("crre.invalid.data.to.insert"));
        } else if (statements.size() > 0) {
            Sql.getInstance().transaction(statements, message -> {
                if (message.body().containsKey("status") &&
                        "ok".equals(message.body().getString("status"))) {
                    handler.handle(new Either.Right<>(
                            new JsonObject().put("status", "ok")));
                } else {
                    handler.handle(new Either.Left<>
                            ("crre.statements.error"));
                }
            });
        } else {
            handler.handle(new Either.Left<>
                    ("crre.statements.empty"));
        }
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
                "purse.amount, purse.initial_amount, seconde, premiere, terminale, pro, " +
                "licences.amount as licence_amount, licences.initial_amount as licence_initial_amount, " +
                "licences.consumable_amount as consumable_licence_amount, " +
                "licences.consumable_initial_amount as consumable_licence_initial_amount " +
                "FROM " + Crre.crreSchema + ".licences " +
                "FULL OUTER JOIN " + Crre.crreSchema + ".students ON students.id_structure = licences.id_structure " +
                "FULL OUTER JOIN " + Crre.crreSchema + ".purse ON purse.id_structure = licences.id_structure " +
                "JOIN " + Crre.crreSchema + ".structure ON structure.id_structure = licences.id_structure " +
                "OR structure.id_structure = purse.id_structure " +
                "OR structure.id_structure = students.id_structure " +
                "WHERE (purse.initial_amount IS NOT NULL OR licences.initial_amount <> 0 OR licences.consumable_initial_amount <> 0) ";
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
            Integer PAGE_SIZE = 15;
            params.add(PAGE_SIZE * page);
            params.add(PAGE_SIZE);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void update(String id_structure, JsonObject purse, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".purse " +
                "SET initial_amount = ?, amount = amount + ( ? - initial_amount) " +
                "WHERE id_structure = ? ;" +
                "UPDATE " + Crre.crreSchema + ".licences " +
                "SET initial_amount = ?, amount = amount + ( ? - initial_amount) " +
                "WHERE id_structure = ? ;" +
                "UPDATE " + Crre.crreSchema + ".licences " +
                "SET consumable_initial_amount = ?, consumable_amount = consumable_amount + ( ? - consumable_initial_amount) " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(purse.getDouble("initial_amount")).add(purse.getDouble("initial_amount")).add(id_structure)
                .add(purse.getInteger("licence_initial_amount")).add(purse.getInteger("licence_initial_amount")).add(id_structure)
                .add(purse.getInteger("consumable_licence_initial_amount")).add(purse.getInteger("consumable_licence_initial_amount")).add(id_structure);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonObject getImportStatementAmount(String structureId, double amount, String table, Boolean consumable_licences, Boolean isOverrideAmount) {
        String statement = "INSERT INTO " + Crre.crreSchema + "." + table + " (id_structure, ";
            if (consumable_licences) {
                statement += "consumable_amount, consumable_initial_amount) " +
                        "VALUES (?,?,?) " +
                        "ON CONFLICT (id_structure) DO UPDATE " +
                        "SET consumable_initial_amount = ?, " +
                        "consumable_amount = (" +
                        "  CASE " +
                        "    WHEN " + table + ".consumable_amount IS NOT NULL THEN " + table + ".consumable_initial_amount - " + table + ".consumable_amount + ? " +
                        "    ELSE ?" +
                        "  END" +
                        ") " +
                        "WHERE " + table + ".id_structure = ? ;";
            } else {
                statement += "amount, initial_amount) " +
                        "VALUES (?,?,?) " +
                        "ON CONFLICT (id_structure) DO UPDATE ";
                if(isOverrideAmount == false) {
                    statement += "SET initial_amount = " + table + ".initial_amount + ?, " +
                            "amount = " + table + ".amount + ? ";
                } else {
                    statement += "SET initial_amount = ?, " +
                            "amount = ? ";
                }
                statement += "WHERE " + table + ".id_structure = ? ;";
            }
            JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
            try {
                params.add(structureId);
                if (table.equals("licences")) {
                    params.add((int) amount)
                            .add((int)amount)
                            .add((int)amount)
                            .add((int)amount)
                            .add((int)amount);
                } else {
                    params.add(amount)
                            .add(amount)
                            .add(amount)
                            .add(amount);
                }
                params.add(structureId);

            } catch (NumberFormatException e) {
                invalidDatas = true;
            }
            return new JsonObject()
                    .put("statement", statement)
                    .put("values", params)
                    .put("action", "prepared");
    }

    private JsonObject getImportStatementStudent(String structureId, int second, int premiere, int terminale, boolean pro) {

        String statement = "INSERT INTO " + Crre.crreSchema + ".students (id_structure, seconde, premiere, terminale, pro) " +
                "VALUES (?,?,?,?,?) " +
                "ON CONFLICT (id_structure) DO UPDATE " +
                "SET seconde = ?, " +
                "premiere = ?, " +
                "terminale = ?, " +
                "pro = ? " +
                "WHERE students.id_structure = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        try {
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

        } catch (NumberFormatException e) {
            invalidDatas = true;
        }
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void updatePurseAmount(Double price, String idStructure, String operation, Handler<Either<String, JsonObject>> handler) {
        final double cons = 100.0;
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".purse " +
                "SET amount = amount " + operation + " ?  " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round(price * cons) / cons)
                .add(idStructure);

        Sql.getInstance().prepared(updateQuery, params, SqlResult.validUniqueResultHandler(handler));
    }
}
