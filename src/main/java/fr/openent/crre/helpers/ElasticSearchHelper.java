package fr.openent.crre.helpers;


import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.elasticsearch.ElasticSearch;

import java.util.*;

public class ElasticSearchHelper {
    private static final String REGEXP_FORMAT = ".*%s.*";
    private static final Integer PAGE_SIZE = 30;
    private static final String RESOURCE_TYPE_NAME = "equipment";
    private static final  List<String> PLAIN_TEXT_FIELDS = Arrays.asList("id", "name", "ean", "editor_name", "grade_name", "subject_name", "author");

    private ElasticSearchHelper() {
        throw new IllegalStateException("Utility class");
    }

    private static String formatRegexp(String query) {
        return String.format(REGEXP_FORMAT, query.toLowerCase());
    }

    private static void search(JsonObject query, Handler<Either<String, JsonArray>> handler) {
        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                JsonArray result = new JsonArray();
                for (Object article:ar.result().getJsonArray("hits")) {
                    result.add(((JsonObject)article).getJsonObject("_source"));
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }

    public static void search_All(Handler<Either<String, JsonArray>> handler) {
        JsonObject query = new JsonObject()
                .put("from", 0)
                .put("size", PAGE_SIZE)
                .put("query", new JsonObject().put("match_all", new JsonObject()));

        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                JsonArray result = new JsonArray();
                for (Object article:ar.result().getJsonArray("hits")) {
                    result.add(((JsonObject)article).getJsonObject("_source"));
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }

    public static void plainTextSearch(String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = new JsonObject()
                    .put(field, formatRegexp(query));
            should.add(new JsonObject().put("regexp", regexp));
        }

        JsonObject regexpBool = new JsonObject()
                .put("should", should);
        JsonArray must = new JsonArray()
        .add(new JsonObject().put("bool", regexpBool));

        JsonObject bool = new JsonObject()
                .put("must", must);
        JsonObject queryObject = new JsonObject()
                .put("bool", bool);

        search(esQueryObject(queryObject), handler);
    }

    public static void filter(HashMap<String, ArrayList<String>> result, Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();

        Set<Map.Entry<String, ArrayList<String>>> set = result.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            term.add(new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue()))));
        }
            JsonObject filter = new JsonObject()
                    .put("filter", term);

            JsonObject queryObject = new JsonObject()
                    .put("bool", filter);

            search(esQueryObject(queryObject), handler);
        }

    private static void executeEsSearch(JsonObject query, Handler<AsyncResult<JsonObject>> handler) {
        ElasticSearch.getInstance().post(RESOURCE_TYPE_NAME + "/_search?scroll=1m", query, search -> {
            if (search.failed()) {
                handler.handle(Future.failedFuture(search.cause()));
            } else {
                JsonObject resources = parseEsResponse(search.result());
                handler.handle(Future.succeededFuture(resources));

            }
        });
    }

    private static JsonObject parseEsResponse(JsonObject esResponse) {
        JsonArray hits = esResponse.getJsonObject("hits", new JsonObject()).getJsonArray("hits", new JsonArray());
        return new JsonObject().put("hits", hits).put("scroll_id", esResponse.getJsonObject("_scroll_id", new JsonObject()));
    }

    private static JsonObject esQueryObject(JsonObject query) {
        return new JsonObject()
                .put("query", query)
                .put("from", 0)
                .put("size", PAGE_SIZE);
    }



    public static void getPageItems(String scroll_id, Handler<Either<String, JsonObject>> handler) {
        JsonObject scrollData = new JsonObject()
                .put("scroll", "1m")
                .put("scroll_id", scroll_id);
        searchForPagination(scrollData, handler);
    }

    private static void searchForPagination(JsonObject scrollData, Handler<Either<String, JsonObject>> handler) {
        executeEsSearchForPagination(scrollData, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                handler.handle(new Either.Right<>(ar.result()));
            }
        });
    }

    private static void executeEsSearchForPagination(JsonObject scrollData, Handler<AsyncResult<JsonObject>> handler) {
        ElasticSearch.getInstance().post("_search/scroll", scrollData, search -> {
            if (search.failed()) {
                handler.handle(Future.failedFuture(search.cause()));
            } else {
                JsonObject scroll_id = search.result().getJsonObject("_scroll_id", new JsonObject());
                handler.handle(Future.succeededFuture(scroll_id));

            }
        });
    }
}