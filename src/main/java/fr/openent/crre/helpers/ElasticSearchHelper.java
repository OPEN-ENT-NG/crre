package fr.openent.crre.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.elasticsearch.ElasticSearch;

import java.util.*;
import java.util.regex.Pattern;

public class ElasticSearchHelper {
    private static final String REGEXP_FORMAT = ".*%s.*";
    private static final Integer PAGE_SIZE = 100000;
    private static final String RESOURCE_TYPE_NAME = "_doc";
    private static final List<String> PLAIN_TEXT_FIELDS =
            Arrays.asList("ean", "titre", "editeur", "disciplines", "niveaux", "auteur", "ark");

    private ElasticSearchHelper() {
        throw new IllegalStateException("Utility class");
    }

    private static String formatRegexp(String query) {
        return String.format(REGEXP_FORMAT, query.toLowerCase());
    }

    private static void search(JsonObject query, JsonArray pro, JsonArray conso, Handler<Either<String, JsonArray>> handler) {
        ESHandler(query, pro, conso, handler);
    }

    private static void ESHandler(JsonObject query, JsonArray pro, JsonArray conso, Handler<Either<String, JsonArray>> handler) {
        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                handler.handle(new Either.Left<>(ar.cause().toString()));
            } else {
                JsonArray result = new JsonArray();
                boolean isConso = conso.size() > 0 ? conso.getBoolean(0) : false;
                boolean isPro = pro.size() > 0 ? pro.getBoolean(0) : false;
                for (Object article : ar.result()) {
                    JsonObject articleJson = ((JsonObject) article);
                    JsonObject addingArticle = articleJson.getJsonObject("_source");
                    String typeNumeric = "";
                    String typePapier = "";
                    String type = "";
                    if (articleJson.getString("_index").equals("articlenumerique") && addingArticle.getJsonArray("offres").size() > 0) {
                        type = addingArticle.getJsonArray("offres").getJsonObject(0).getString("type") == null ? "" : addingArticle.getJsonArray("offres").getJsonObject(0).getString("type");
                        addingArticle.put("typeCatalogue", type);
                        typeNumeric = type;
                    } else {
                        type = addingArticle.getString("type", "") == null ? "" : addingArticle.getString("type", "");
                        addingArticle.put("typeCatalogue", type);
                        typePapier = type;
                    }
                    addingArticle.put("type", articleJson.getString("_index"))
                            .put("id", articleJson.getString("_id"));
                    boolean consoNumeric = isConso == Pattern.compile(".*conso.*", Pattern.CASE_INSENSITIVE).matcher(typeNumeric).find();
                    boolean consoPapier = isConso == Pattern.compile(".*conso.*", Pattern.CASE_INSENSITIVE).matcher(typePapier).find();
                    boolean proNumeric = false;
                    if (addingArticle.getJsonArray("niveaux").size() > 0) {
                        for (int i = 0; i < addingArticle.getJsonArray("niveaux").size(); i++) {
                            String niveau = addingArticle.getJsonArray("niveaux").getJsonObject(i).getString("libelle") == null ? "" : addingArticle.getJsonArray("niveaux").getJsonObject(i).getString("libelle");
                            if (niveau.equals("Lycée pro.")) {
                                proNumeric = true;
                            }
                        }
                    }
                    proNumeric = isPro == proNumeric;
                    boolean proPapier = isPro == Pattern.compile(".*pro.*", Pattern.CASE_INSENSITIVE).matcher(typePapier).find();
                    if ((articleJson.getString("_index").equals("articlenumerique") && (conso.size() <= 0 || consoNumeric) && (pro.size() <= 0 || proNumeric)) ||
                            (articleJson.getString("_index").equals("articlepapier") && (conso.size() <= 0 || consoPapier) && (pro.size() <= 0 || proPapier)) || conso.size() != 1 && pro.size() != 1) {
                        result.add(addingArticle);
                    }
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

        ESHandler(query, new JsonArray(), new JsonArray(), handler);
    }


    public static void plainTextSearch(String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = regexpField(field, query);
            should.add(regexp);
        }

        regexSearchBool(handler, should);
    }

    private static void regexSearchBool(Handler<Either<String, JsonArray>> handler, JsonArray should) {
        JsonObject regexpBool = new JsonObject()
                .put("should", should);
        JsonArray must = new JsonArray()
                .add(new JsonObject().put("bool", regexpBool));

        JsonObject bool = new JsonObject()
                .put("must", must);
        JsonObject queryObject = new JsonObject()
                .put("bool", bool);

        search(esQueryObject(queryObject), new JsonArray(), new JsonArray(), handler);
    }

    public static void plainTextSearchName(String query, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        JsonObject regexp = regexpField("titre", query);
        should.add(regexp);

        regexSearchBool(handler, should);
    }

    public static void filters(HashMap<String, ArrayList<String>> result, Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();
        JsonObject filter = new JsonObject();
        JsonArray query = new JsonArray();
        JsonArray conso = new JsonArray();
        JsonArray pro = new JsonArray();

        prepareFilterES(result, term, query, conso, pro);

        filter.put("filter", term.addAll(query));

        JsonObject queryObject = new JsonObject()
                .put("bool", filter);

        search(esQueryObject(queryObject), pro, conso, handler);
    }

    private static void prepareFilterES(HashMap<String, ArrayList<String>> result, JsonArray term, JsonArray query, JsonArray conso, JsonArray pro) {
        Set<Map.Entry<String, ArrayList<String>>> set = result.entrySet();

        for (Map.Entry<String, ArrayList<String>> me : set) {
            switch (me.getKey()) {
                case "disciplines.libelle":
                case "classes.libelle":
                case "niveaux.libelle":
                case "technos.technologie": {
                    JsonObject terms = new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue())));
                    JsonObject bool = new JsonObject().put("bool", new JsonObject().put("filter", terms));
                    JsonObject nested = new JsonObject().put("path", me.getKey().split("\\.")[0])
                            .put("query", bool);
                    query.add(new JsonObject().put("nested", nested));
                    break;
                }
                case "conso": {
                    if (me.getValue().size() == 1) {
                        if (me.getValue().get(0).equals("Consommable")) {
                            conso.add(true);
                        } else {
                            conso.add(false);
                        }
                    }
                    break;
                }
                case "pro": {
                    if (me.getValue().size() == 1) {
                        if (me.getValue().get(0).equals("Lycée professionnel")) {
                            pro.add(true);
                        } else {
                            pro.add(false);
                        }
                    }
                    break;
                }
                default: {
                    term.add(new JsonObject().put("terms", new JsonObject().put(me.getKey(), new JsonArray(me.getValue()))));
                    break;
                }
            }
        }
    }

    public static void searchById(String id, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject match = new JsonObject().put("_id", id);
        queryObject.put("match", match);
        search(esQueryObject(queryObject), new JsonArray(), new JsonArray(), handler);
    }

    public static void searchByIds(List<String> ids, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject terms = new JsonObject().put("_id", new JsonArray(ids));
        queryObject.put("terms", terms);
        search(esQueryObject(queryObject), new JsonArray(), new JsonArray(), handler);
    }


    public static void searchfilter(HashMap<String, ArrayList<String>> result, String query,
                                    Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();
        JsonArray should = new JsonArray();
        JsonObject request = new JsonObject();
        JsonArray j = new JsonArray();
        JsonArray conso = new JsonArray();
        JsonArray pro = new JsonArray();


        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = regexpField(field, query);
            should.add(regexp);
        }

        prepareFilterES(result, term, j, conso, pro);
        request.put("filter", term.addAll(j))
                .put("minimum_should_match", 1)
                .put("should", should);

        JsonObject queryObject = new JsonObject()
                .put("bool", request);

        search(esQueryObject(queryObject), pro, conso, handler);
    }

    private static void executeEsSearch(JsonObject query, Handler<AsyncResult<JsonArray>> handler) {
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
                .put("size", PAGE_SIZE)
                .put("sort", new JsonArray()
                        .add(new JsonObject().put("_index","asc"))
                        .add(new JsonObject().put("titre","asc"))
                );
    }


    private static JsonObject regexpField(String field, String query) {
        JsonObject regexp = new JsonObject()
                .put(field, formatRegexp(query));

        return new JsonObject().put("regexp", regexp);
    }
}
