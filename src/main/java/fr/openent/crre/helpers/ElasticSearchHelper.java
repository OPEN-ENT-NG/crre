package fr.openent.crre.helpers;


import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.elasticsearch.ElasticSearch;

import java.util.*;

public class ElasticSearchHelper {
    private static final String REGEXP_FORMAT = ".*%s.*";
    private static final Integer PAGE_SIZE = 10000;
    private static final String RESOURCE_TYPE_NAME = "_doc";
    private static final  List<String> PLAIN_TEXT_FIELDS = Arrays.asList("ean", "titre", "editeur", "disciplines", "niveaux", "auteur", "ark");

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
                    result.add(((JsonObject)article).getJsonObject("_source")
                            .put("type", ((JsonObject)article).getString("_index"))
                            .put("id", ((JsonObject)article).getString("_id"))
                    );
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
                for (Object article:ar.result()) {
                    result.add(((JsonObject)article).getJsonObject("_source")
                            .put("type", ((JsonObject)article).getString("_index"))
                            .put("id", ((JsonObject)article).getString("_id"))
                    );
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }

    public static void getAllFilters(Handler<Either<String, JsonArray>> handler) {
        JsonArray fields = new JsonArray().add("disciplines.libelle")
                                        .add("niveaux.libelle")
                                        .add("editeur");
        JsonObject query = new JsonObject()
                .put("from", 0)
                .put("size", PAGE_SIZE)
                .put("_source", fields);

        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                JsonArray result = new JsonArray();
                for (Object article:ar.result()) {
                    result.add(((JsonObject)article).getJsonObject("_source")
                            .put("type", ((JsonObject)article).getString("_index"))
                            .put("id", ((JsonObject)article).getString("_id"))
                    );
                }
                handler.handle(new Either.Right<>(result));
            }
        });
    }


    public static void plainTextSearch(String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = regexpField(field, query);
            should.add(regexp);
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

    public static void plainTextSearchName(String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
            JsonObject regexp = regexpField("titre", query);
            should.add(regexp);

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

    public static void filter_waiting(List<String> filters, String query, Handler<Either<String, JsonArray>> handler) {
        JsonObject term = new JsonObject();
        JsonArray should = new JsonArray();
        JsonObject queryObject = new JsonObject();

        if (query != null) {
            JsonObject regexp = regexpField("titre", query);
            should.add(regexp);
        }
        ArrayList<String> filter_tab = new ArrayList<>(filters);
        term = new JsonObject().put("terms", new JsonObject().put("niveaux.libelle", new JsonArray(filter_tab)));
        JsonObject bool = new JsonObject().put("bool", new JsonObject().put("filter", term));
        JsonObject nested = new JsonObject().put("path", "niveaux")
                .put("query", bool);
        JsonArray j = new JsonArray().add(new JsonObject().put("nested", nested));




        if(query != null) {
            JsonObject request = new JsonObject()
                    .put("filter", j)
                    .put("minimum_should_match", 1)
                    .put("should", should);

            queryObject.put("bool", request);
        } else {
            JsonObject filter = new JsonObject()
                    .put("filter", j);
            queryObject.put("bool", filter);
        }
        search(esQueryObject(queryObject), handler);
    }

    public static void filter(HashMap<String, ArrayList<String>> result, Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();
        JsonObject filter = new JsonObject();
        JsonArray j = new JsonArray();

        Set<Map.Entry<String, ArrayList<String>>> set = result.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            if(me.getKey().equals("disciplines.libelle") || me.getKey().equals("niveaux.libelle") || me.getKey().equals("technos.technologie")) {
                JsonObject t = new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue())));
                JsonObject bool = new JsonObject().put("bool", new JsonObject().put("filter", t));
                JsonObject nested = new JsonObject().put("path", me.getKey().split("\\.")[0])
                                                    .put("query", bool);
                j = new JsonArray().add(new JsonObject().put("nested", nested));
            } else {
                term.add(new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue()))));
            }
        }
        filter.put("filter", term.addAll(j));

        JsonObject queryObject = new JsonObject()
                .put("bool", filter);

        search(esQueryObject(queryObject), handler);
    }

    public static void searchById(String id, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject match = new JsonObject().put("_id", id);
        queryObject.put("match", match);
        search(esQueryObject(queryObject), handler);
    }

    public static void searchByIds(List<String> ids, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject terms = new JsonObject().put("_id", new JsonArray(ids));
        queryObject.put("terms", terms);
        search(esQueryObject(queryObject), handler);
    }



    public static void searchfilter(HashMap<String, ArrayList<String>> result, String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();
        JsonArray should = new JsonArray();
        JsonObject request = new JsonObject();
        JsonArray j = new JsonArray();

        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = regexpField(field, query);
            should.add(regexp);
        }


        Set<Map.Entry<String, ArrayList<String>>> set = result.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            if(me.getKey().equals("disciplines.libelle") || me.getKey().equals("niveaux.libelle") || me.getKey().equals("technos.technologie")) {
                JsonObject t = new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue())));
                JsonObject bool = new JsonObject().put("bool", new JsonObject().put("filter", t));
                JsonObject nested = new JsonObject().put("path", me.getKey().split("\\.")[0])
                        .put("query", bool);
                j = new JsonArray().add(new JsonObject().put("nested", nested));
            } else {
                term.add(new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue()))));
            }
        }
                request.put("filter", term.addAll(j))
                .put("minimum_should_match", 1)
                .put("should", should);

        JsonObject queryObject = new JsonObject()
                .put("bool", request);


        search(esQueryObject(queryObject), handler);
    }

    private static void executeEsSearch (JsonObject query, Handler<AsyncResult<JsonArray>> handler) {
        ElasticSearch.getInstance().search(RESOURCE_TYPE_NAME, query, search -> {
            if (search.failed()) {
                handler.handle(Future.failedFuture(search.cause()));
            } else {
                List resources = parseEsResponse(search.result());
                handler.handle(Future.succeededFuture(new JsonArray(resources)));

            }
        });
    }

    private static List parseEsResponse(JsonObject esResponse) {
        return esResponse
                .getJsonObject("hits", new JsonObject())
                .getJsonArray("hits", new JsonArray()).getList();
    }

    private static JsonObject esQueryObject(JsonObject query) {
        return new JsonObject()
                .put("query", query)
                .put("from", 0)
                .put("size", PAGE_SIZE);
    }


    private static JsonObject regexpField(String field, String query) {
        JsonObject regexp = new JsonObject()
                .put(field, formatRegexp(query));

        return new JsonObject().put("regexp", regexp);
    }
}
