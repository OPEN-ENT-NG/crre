package fr.openent.crre.logging;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Logging {

    private static final int BAD_REQUEST_STATUS = 400;
    private static final int OK_STATUS = 200;

    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);


    private Logging() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * @deprecated Use {@link #insert(UserInfos, String, String, String, JsonObject)}
     */
    @Deprecated
    public static JsonObject add(final String context, final String action, final String item, final JsonObject object,
                                 UserInfos user) {
        final JsonObject statement = new JsonObject();
        StringBuilder query = new StringBuilder("INSERT INTO ")
                .append(Crre.crreSchema)
                .append(".logs(id_user, username, action, context, item" );
        if (object != null) {
            query.append(", value");
        }
        query.append(") VALUES (?, ?, ?, ?, ?");
        if (object != null) {
            query.append(", to_json(?::text)");
        }
        query.append(");");

        JsonArray params = addParams(context, action, item, object, user);
        statement.put("statement", query.toString())
                .put("values",params)
                .put("action", "prepared");

        return statement;
    }

    public static TransactionElement addTransaction(final String context, final String action, final String item, final JsonObject object,
                                         UserInfos user) {
        StringBuilder query = new StringBuilder("INSERT INTO ")
                .append(Crre.crreSchema)
                .append(".logs(id_user, username, action, context, item" );
        if (object != null) {
            query.append(", value");
        }
        query.append(") VALUES (?, ?, ?, ?, ?");
        if (object != null) {
            query.append(", to_json(?::text)");
        }
        query.append(");");

        JsonArray params = addParams(context, action, item, object, user);

        return new TransactionElement(query.toString(), params);
    }

    public static void defaultResponseFuture(final EventBus eb, final HttpServerRequest request, final String context, final String action,
                                             final String item, final JsonObject object, Future<JsonObject> future) {
        Handler<Either<String, JsonObject>> handler = defaultResponseHandler(eb, request, context, action, item, object);
        future.onSuccess(res -> handler.handle(new Either.Right<>(res)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static void defaultResponsesFuture(final EventBus eb, final HttpServerRequest request, final String context, final String action,
                                              final List<String> item, final JsonObject object, Future<JsonObject> future) {
        Handler<Either<String, JsonObject>> handler = defaultResponsesHandler(eb, request, context, action, item, object);
        future.onSuccess(res -> handler.handle(new Either.Right<>(res)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static Handler<Either<String, JsonObject>> defaultResponseHandler(final EventBus eb,
                      final HttpServerRequest request, final String context, final String action,
                      final String item, final JsonObject object) {
        return event -> {
            if (event.isRight()) {
                UserUtils.getUserInfos(eb, request, user -> {
                    Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                    JsonObject statement = add(context, action,
                            item == null ? event.right().getValue().getInteger(Field.ID).toString() : item, object, user);
                    Sql.getInstance().prepared(statement.getString("statement"), statement.getJsonArray("values"),
                            response -> {
                        if (!Field.OK.equals(response.body().getString(Field.STATUS))) {
                            log(context, action);
                        }
                    });
                });
            } else {
                JsonObject error = new JsonObject()
                        .put(Field.ERROR, event.left().getValue());
                Renders.renderJson(request, error, BAD_REQUEST_STATUS);
            }
        };
    }
    public static Handler<Either<String, JsonObject>> defaultResponsesHandler
            (final EventBus eb, final HttpServerRequest request, final String context, final String action,
             final  List<String> items, final JsonObject object) {
        return event -> {
            if (event.isRight()) {
                UserUtils.getUserInfos(eb, request, user -> {
                    Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                    JsonArray statements = new JsonArray();
                    for (String item : items) {
                        statements.add(add(context, action, item, object, user));
                    }
                    Sql.getInstance().transaction(statements, response -> {
                        if (!Field.OK.equals(response.body().getString(Field.STATUS))) {
                            log(context, action);
                        }
                    });
                });
            } else {
                JsonObject error = new JsonObject()
                        .put(Field.ERROR, event.left().getValue());
                Renders.renderJson(request, error, BAD_REQUEST_STATUS);
            }
        };
    }

    public static Handler<Either<String, JsonObject>> defaultCreateResponsesHandler
            (final EventBus eb, final HttpServerRequest request,
             final String context, final String action,final String item, final JsonArray objects) {
        return event -> {
            if (event.isRight()) {
                UserUtils.getUserInfos(eb, request, user -> {
                    JsonObject object;
                    Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                    JsonArray statements = new JsonArray();
                    for(int i=0; i<objects.size(); i++){
                        object = objects.getJsonObject(i);
                        statements.add( add(context, action,
                                object.getInteger(item).toString(), object, user));
                    }
                    Sql.getInstance().transaction(statements, response -> {
                        if (!Field.OK.equals(response.body().getString(Field.STATUS))) {
                            log(context, action);
                        }
                    });
                });
            } else {
                JsonObject error = new JsonObject()
                        .put(Field.ERROR, event.left().getValue());
                Renders.renderJson(request, error, BAD_REQUEST_STATUS);
            }
        };
    }

    /**
     * @deprecated Use {@link #insert(UserInfos, String, String, String, JsonObject)}
     */
    @Deprecated
    public static void insert(EventBus eb, HttpServerRequest request, final String context,
                               final String action, final String item, final JsonObject object) {
        UserUtils.getUserInfos(eb, request, user -> {
            String query = "INSERT INTO " + Crre.crreSchema + ".logs" +
                    "(id_user, username, action, context, item, value) " +
                    "VALUES (?, ?, ?, ?, ?, ";
            if (object != null) {
                query += "to_json(?::text)";
            } else {
                query += "null";
            }
            query += ")";
            JsonArray params = addParams(context, action, item, object, user);
            Sql.getInstance().prepared(query, params, response -> {
                if (!Field.OK.equals(response.body().getString(Field.STATUS))) {
                    log(context, action);
                }
            });
        });
    }

    public static void insert(UserInfos userInfos, final String context,
                              final String action, final String item, final JsonArray object) {
        final List<TransactionElement> statements = object.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(jsonObject -> addTransaction(context, action, item, jsonObject, userInfos))
                .collect(Collectors.toList());

        TransactionHelper.executeTransaction(statements)
                .onFailure(error -> log(context, action));
    }

    public static void insert(UserInfos userInfos, final String context,
                              final String action, final String item, final JsonObject object) {
        final List<TransactionElement> statements = Collections.singletonList(addTransaction(context, action, item, object, userInfos));

        TransactionHelper.executeTransaction(statements)
                .onFailure(error -> log(context, action));
    }

    public static void insert(UserInfos userInfos, final String context,
                              final String action, final List<String> items, final JsonObject object) {
        final List<TransactionElement> statements = items.stream()
                .map(item -> addTransaction(context, action, item, object, userInfos))
                .collect(Collectors.toList());

        TransactionHelper.executeTransaction(statements)
                .onFailure(error -> log(context, action));
    }

    private static JsonArray addParams(String context, String action, String item, JsonObject object, UserInfos user) {
        JsonArray params = new JsonArray()
                .add(user.getUserId())
                .add(user.getUsername())
                .add(action)
                .add(context)
                .add(item.contains("id = ") ? item : ("id = " + item));
        if (object != null) {
            params.add(object);
        }
        return params;
    }

    private static void log(String context, String action) {
        LOGGER.error("An error occurred when logging state for " + context + " - " + action);
    }
}
