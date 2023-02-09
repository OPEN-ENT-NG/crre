package fr.openent.crre.helpers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class SqlHelper {
    private static final Logger log = LoggerFactory.getLogger(SqlHelper.class);

    private SqlHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Get the next value for a given sequences name in sql
     * @param sequencesName the sequences name
     * @return a future succeed with the next value
     */
    public static Future<Integer> getNextVal(String sequencesName) {
        Promise<Integer> promise = Promise.promise();

        String getIdQuery = "Select nextval('" + Crre.crreSchema + "." + sequencesName + "') as id";
        Sql.getInstance().raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue().getInteger(Field.ID));
            } else {
                log.error(String.format("[CRRE@%s::getNextVal] Fail to get next value id for sequences %s : %s",
                        SqlHelper.class.getSimpleName(), sequencesName, event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        }));

        return promise.future();
    }
}
