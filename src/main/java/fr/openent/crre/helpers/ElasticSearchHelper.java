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
    private static final Integer PAGE_SIZE = 10000;
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
                for (Object article:ar.result()) {
                    result.add(((JsonObject)article).getJsonObject("_source"));
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }

    public static void search_All(Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonObject query = new JsonObject()
                .put("from", 0)
                .put("size", PAGE_SIZE)
                .put("query", new JsonObject().put("match_all", new JsonObject()));

        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                JsonArray result = new JsonArray();
                for (Object article:ar.result()) {
                    result.add(((JsonObject)article).getJsonObject("_source"));
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }


    public static void plainTextSearch(String query, Handler<Either<String, JsonArray>> handler, Integer page) {
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

        search(esQueryObject(queryObject, page), handler);
    }

    public static void filter(HashMap<String, ArrayList<String>> result, Handler<Either<String, JsonArray>> handler, Integer page) {


        JsonArray term = new JsonArray();

        Set<Map.Entry<String, ArrayList<String>>> set = result.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            term.add(new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue()))));
        }
        JsonObject filter = new JsonObject()
                .put("filter", term);

        JsonObject queryObject = new JsonObject()
                .put("bool", filter);

        search(esQueryObject(queryObject, page), handler);
    }

    private static void executeEsSearch (JsonObject query, Handler<AsyncResult<JsonArray>> handler) {
        ElasticSearch.getInstance().search(RESOURCE_TYPE_NAME, query, search -> {
            if (search.failed()) {
                handler.handle(Future.failedFuture(search.cause()));
            } else {
                List<JsonObject> resources = parseEsResponse(search.result());
                handler.handle(Future.succeededFuture(new JsonArray(resources)));

            }
        });
    }

    private static List<JsonObject> parseEsResponse(JsonObject esResponse) {
        return esResponse
                .getJsonObject("hits", new JsonObject())
                .getJsonArray("hits", new JsonArray()).getList();
    }

    private static JsonObject esQueryObject(JsonObject query, Integer page) {
        return new JsonObject()
                .put("query", query)
                .put("from", 0)
                .put("size", PAGE_SIZE);
    }

}
