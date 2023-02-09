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

    /**
     * See {@link #executeTransaction(List, String)}
     */
    public static Future<List<TransactionElement>> executeTransaction(List<TransactionElement> transactionElementList) {
        return executeTransaction(transactionElementList, null);
    }

    /**
     * Allows you to execute a set of sql queries in a transaction
     *
     * @param transactionElementList list of queries in the form {@link TransactionElement}
     * @param errorMessage message sent in the logs if the transaction failed
     */
    public static Future<List<TransactionElement>> executeTransaction(List<TransactionElement> transactionElementList, String errorMessage) {
        Promise<List<TransactionElement>> promise = Promise.promise();
        JsonArray statements = new JsonArray();

        statements.addAll(new JsonArray(transactionElementList.stream().map(TransactionElement::toJson).collect(Collectors.toList())));

        Sql.getInstance().transaction(statements, SqlResult.validResultsHandler(res -> {
            if (res.isRight()) {
                for (int i = 0; i < transactionElementList.size(); i++) {
                    transactionElementList.get(i).setResult((JsonArray) res.right().getValue().getList().get(i));
                }
                promise.complete(transactionElementList);
            } else {
                if (errorMessage != null) {
                    log.error(errorMessage + " " + res.left().getValue());
                }
                promise.fail(res.left().getValue());
            }
        }));

        return promise.future();
    }
}
