package fr.openent.crre.helpers;

import fr.openent.crre.model.TransactionElement;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionHelper {
    public static Future<JsonArray> executeTransaction(List<TransactionElement> prepareRequestList) {
        Promise<JsonArray> promise = Promise.promise();
        JsonArray statements = new JsonArray();

        statements.addAll(new JsonArray(prepareRequestList.stream().map(TransactionElement::toJson).collect(Collectors.toList())));

        Sql.getInstance().transaction(statements, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }
}
