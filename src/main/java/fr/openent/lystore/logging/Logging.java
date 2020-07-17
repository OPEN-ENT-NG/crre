package fr.openent.lystore.logging;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.List;

public final class Logging {

    private static final int BAD_REQUEST_STATUS = 400;
    private static final int OK_STATUS = 200;

    private static final Logger LOGGER = LoggerFactory.getLogger(Logging.class);


    private Logging() {
        throw new IllegalAccessError("Utility class");
    }

    public static JsonObject add(EventBus eb, HttpServerRequest request, final String context,
                                 final String action, final String item, final JsonObject object, UserInfos user) {
        final JsonObject statement = new JsonObject();
        StringBuilder query = new StringBuilder("INSERT INTO ")
                .append(Lystore.lystoreSchema)
                .append(".logs(id_user, username, action, context, item" );
        if (object != null) {
            query.append(", value");
        }
        query.append(") VALUES (?, ?, ?, ?, ?");
        if (object != null) {
            query.append(", to_json(?::text)");
        }
        query.append(");");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(user.getUserId())
                .add(user.getUsername())
                .add(action)
                .add(context)
                .add(item.contains("id = ") ? item : ("id = " + item));
        if (object != null) {
            params.add(object);
        }
        statement.put("statement", query.toString())
                .put("values",params)
                .put("action", "prepared");

        return statement;
    }

    public static Handler<Either<String, JsonObject>> defaultResponseHandler (final EventBus eb,
                      final HttpServerRequest request, final String context, final String action,
                      final String item, final JsonObject object) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> event) {
                if (event.isRight()) {
                    UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                        @Override
                        public void handle(UserInfos user) {
                            Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                            JsonObject statement = add(eb, request, context, action,
                                    item == null ? event.right().getValue().getInteger("id").toString() : item, object, user);
                            Sql.getInstance().prepared(statement.getString("statement"), statement.getJsonArray("values"), new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> response) {
                                    if (!"ok".equals(response.body().getString("status"))) {
                                        log(context, action);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    JsonObject error = new JsonObject()
                            .put("error", event.left().getValue());
                    Renders.renderJson(request, error, BAD_REQUEST_STATUS);
                }
            }
        };
    }
    public static Handler<Either<String, JsonObject>> defaultResponsesHandler
            (final EventBus eb, final HttpServerRequest request, final String context, final String action,
             final  List<String> items, final JsonObject object) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> event) {
                if (event.isRight()) {
                    UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                        @Override
                        public void handle(UserInfos user) {
                            Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                            for(int i=0; i<items.size(); i++){
                                statements.add( add(eb, request, context, action,items.get(i), object, user));
                            }
                            Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> response) {
                                    if (!"ok".equals(response.body().getString("status"))) {
                                        log(context, action);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    JsonObject error = new JsonObject()
                            .put("error", event.left().getValue());
                    Renders.renderJson(request, error, BAD_REQUEST_STATUS);
                }
            }
        };
    }

    public static Handler<Either<String, JsonObject>> defaultCreateResponsesHandler
            (final EventBus eb, final HttpServerRequest request,
             final String context, final String action,final String item, final JsonArray objects) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> event) {
                if (event.isRight()) {
                    UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                        @Override
                        public void handle(UserInfos user) {
                            JsonObject object;
                            Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                            for(int i=0; i<objects.size(); i++){
                                object = objects.getJsonObject(i);
                                statements.add( add(eb, request, context, action,
                                        object.getInteger(item).toString(), object, user));
                            }
                            Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> response) {
                                    if (!"ok".equals(response.body().getString("status"))) {
                                        log(context, action);
                                    }
                                }
                            });
                        }
                    });
                } else {
                    JsonObject error = new JsonObject()
                            .put("error", event.left().getValue());
                    Renders.renderJson(request, error, BAD_REQUEST_STATUS);
                }
            }
        };
    }
    public static void insert (EventBus eb, HttpServerRequest request, final String context,
                               final String action, final String item, final JsonObject object) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                String query = "INSERT INTO " + Lystore.lystoreSchema + ".logs" +
                        "(id_user, username, action, context, item, value) " +
                        "VALUES (?, ?, ?, ?, ?, ";

                if (object != null) {
                    query += "to_json(?::text)";
                } else {
                    query += "null";
                }

                query += ")";

                JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                        .add(user.getUserId())
                        .add(user.getUsername())
                        .add(action)
                        .add(context)
                        .add(item.contains("id = ") ? item : ("id = " + item));
                if (object != null) {
                    params.add(object);
                }

                Sql.getInstance().prepared(query, params, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> response) {
                        if (!"ok".equals(response.body().getString("status"))) {
                            log(context, action);
                        }
                    }
                });
            }
        });
    }

    private static void log(String context, String action) {
        LOGGER.error("An error occurred when logging state for " + context + " - " + action);
    }

}
