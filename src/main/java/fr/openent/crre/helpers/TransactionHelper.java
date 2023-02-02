package fr.openent.crre.helpers;

import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.impl.DefaultStructureGroupService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionHelper {
    private static final Logger log = LoggerFactory.getLogger(DefaultStructureGroupService.class);

    private TransactionHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Future<JsonArray> executeTransaction(List<TransactionElement> prepareRequestList) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray statements = new JsonArray();

        statements.addAll(new JsonArray(prepareRequestList.stream().map(TransactionElement::toJson).collect(Collectors.toList())));

        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    public static Future<JsonArray> executeTransaction(List<TransactionElement> prepareRequestList, String errorMessage) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray statements = new JsonArray();

        statements.addAll(new JsonArray(prepareRequestList.stream().map(TransactionElement::toJson).collect(Collectors.toList())));

        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(res -> {
            if (res.isRight()) {
                promise.complete(res.right().getValue());
            } else {
                String message = String.format("%s : %s", errorMessage, res.left().getValue());
                log.error(String.format("[CRRE@%s::executeTransaction] %s", TransactionHelper.class.getSimpleName(), message));
                promise.fail(message);
            }
        }));

        return promise.future();
    }
}
