package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.LogService;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultLogService implements LogService {

    private static final int NB_OCCURRENCES_PAGE = 100;

    public void list(Integer page, Handler<Either<String, JsonArray>> handler)  {
        String query = "SELECT id, date, action, context , CASE " +
                " WHEN value is null THEN "+
                "'{}' " +
                " ELSE value END, " +
                " id_user, username, item "+
                "FROM " + Crre.crreSchema + ".logs LOGGER ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        if (page != null) {
            query += " ORDER BY date DESC LIMIT 100 OFFSET ?";
            params.add(NB_OCCURRENCES_PAGE * page);
        }
        Sql.getInstance().prepared(query, params,new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getLogsNumber(Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(id) as number_logs FROM " + Crre.crreSchema + ".logs";

        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validUniqueResultHandler(handler));
    }
}
