package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

public final class SqlUtils {

    private SqlUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void deleteIds(String table, List<Integer> ids,
                                 io.vertx.core.Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Crre.crreSchema + "." + table + " WHERE " +
                SqlQueryUtils.prepareMultipleIds(ids);
        for (Integer id : ids) {
            params.add(id);
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
