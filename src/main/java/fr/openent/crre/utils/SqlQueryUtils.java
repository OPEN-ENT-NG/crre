package fr.openent.crre.utils;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class SqlQueryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlQueryUtils.class);

    private SqlQueryUtils() {
        throw new IllegalAccessError("SqlQueryUtils Utility class");
    }

    public static List<Integer> getIntegerIds (List<String> params) {
        List<Integer> ids = new ArrayList<>();
        for (String param : params) {
            ids.add(Integer.parseInt(param));
        }
        return ids;
    }

    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    public static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("id", id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching transaction");
            either = new Either.Left<>("");
        }
        return either;
    }
}
