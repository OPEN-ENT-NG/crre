package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultPurseService implements PurseService {
    private Boolean invalidDatas= false;

    @Override
    public void launchImport(Integer campaignId, JsonObject statementsValues,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        invalidDatas = false;
        for (String field : fields) {
            statements.add(getImportStatement(field,
                    statementsValues.getString(field)));

        }
        if(invalidDatas){
            handler.handle(new Either.Left<>
                    ("crre.invalid.data.to.insert"));
        }else  if (statements.size() > 0) {
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
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Crre.crreSchema + ".purse" +
                " WHERE id_structure IN " +
                "( SELECT id_structure FROM " + Crre.crreSchema + ".rel_group_structure WHERE id_structure_group IN " +
                "( SELECT id_structure_group FROM " + Crre.crreSchema + ".rel_group_campaign WHERE id_campaign = ? ) );";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject getImportStatement(String structureId, String amount) {
        String statement = "INSERT INTO " + Crre.crreSchema + ".purse(id_structure, amount, initial_amount) " +
                "VALUES (?, ?,?) " +
                "ON CONFLICT (id_structure) DO UPDATE " +
                "SET amount = ?, " +
                "initial_amount = ? " +
                "WHERE purse.id_structure = ? ;";
        JsonArray params =  new fr.wseduc.webutils.collections.JsonArray();
        try {
            params.add(structureId)
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(structureId);

        }catch (NumberFormatException e){
            invalidDatas = true;
        }
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void updatePurseAmount(Double price, String idStructure,String operation, Handler<Either<String, JsonObject>> handler) {
        final double cons = 100.0;
        String updateQuery = "UPDATE  " + Crre.crreSchema + ".purse " +
                "SET amount = amount " +  operation + " ?  " +
                "WHERE id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round(price * cons)/cons)
                .add(idStructure);

        Sql.getInstance().prepared(updateQuery, params, SqlResult.validUniqueResultHandler(handler));
    }
}
