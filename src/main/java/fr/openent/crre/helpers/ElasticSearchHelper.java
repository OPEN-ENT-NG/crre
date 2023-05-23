package fr.openent.crre.helpers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.core.constants.ItemFilterField;
import fr.openent.crre.helpers.elasticsearch.ElasticSearch;
import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.model.item.Item;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private static Future<List<Item>> execute(JsonObject query, JsonArray pro, JsonArray conso, JsonArray ressource) {
        Promise<List<Item>> promise = Promise.promise();
        executeEsSearch(query, ar -> {
            if (ar.failed()) {
                promise.fail(ar.cause().toString());
            }
            List<Item> result = new ArrayList<>();
            boolean isConso = conso.size() > 0 ? conso.getBoolean(0) : false;
            boolean isPro = pro.size() > 0 ? pro.getBoolean(0) : false;
            boolean isRessource = ressource.size() > 0 ? ressource.getBoolean(0) : false;
            List<Item> items = JsonHelper.jsonArrayToList(ar.result(), JsonObject.class)
                    .stream()
                    .filter(item -> item.containsKey(Field._SOURCE) && item.containsKey(Field._INDEX))
                    .map(item -> new Item(item.getJsonObject(Field._SOURCE).put(ItemField.TYPE_CATALOG,item.getString(Field._INDEX))))
                    .filter(item -> item.getOffers() != null && !item.getOffers().isEmpty())
                    .collect(Collectors.toList());

            if(items.stream().allMatch(item -> item.getEan() == null)) {
                promise.complete(items);
                return;
            }
            items.forEach(item -> {
                List<String> types = Arrays.stream(item.getType().split(Pattern.quote("|"))).collect(Collectors.toList());
                // TODO: #TERRITOIRE rajouter le filre (contenu dans typesNumeric en theorie)
                boolean ressourceNumeric = isRessource == types.contains(Field.RESSOURCE);
                boolean consoNumeric = isConso == types.contains(Field.CONSOMMABLE);
                boolean manuelNumeric = !isConso == types.contains(Field.NUMERIQUE);
                boolean consoPapier = isConso == Pattern.compile(".*conso.*", Pattern.CASE_INSENSITIVE).matcher(item.getType()).find();
                boolean proNumeric = false;
                boolean lgtNumeric = false;
                if (!item.getLevels().isEmpty() && item.getCatalog().equals(Field.ARTICLENUMERIQUE)) {
                    for (int i = 0; i < item.getLevels().size(); i++) {
                        String level = item.getLevels().get(i);
                        if (level.equals("Lycée pro.")) {
                            proNumeric = true;
                        } else if (level.equals("Lycée général") || level.equals("Lycée techno.")) {
                            lgtNumeric = true;
                        }
                    }
                }
                proNumeric = isPro && proNumeric;
                lgtNumeric = !isPro && lgtNumeric;
                boolean proPapier = isPro && Pattern.compile(".*pro.*", Pattern.CASE_INSENSITIVE).matcher(item.getType()).find();
                if (isConso && isPro) {
                    proPapier = true;
                }
                boolean lgtPapier = !isPro && Pattern.compile(".*pap$", Pattern.CASE_INSENSITIVE).matcher(item.getType()).find();
                if ((item.getCatalog().equals(Field.ARTICLENUMERIQUE) && (conso.size() <= 0 || consoNumeric || (manuelNumeric && !isConso)) && (ressource.size() <= 0 || ressourceNumeric) && (pro.size() <= 0 || proNumeric || lgtNumeric)) ||
                        (item.getCatalog().equals(Field.ARTICLEPAPIER) && (conso.size() <= 0 || consoPapier) && (pro.size() <= 0 || proPapier || lgtPapier) && !isRessource)
                        || ressource.size() != 1 && conso.size() != 1 && pro.size() != 1) {
                    result.add(item);
                }
            });
            promise.complete(result);
        });
        return promise.future();
    }

    /**
     * @deprecated  Replaced by {@link #execute(JsonObject, JsonArray, JsonArray, JsonArray)}
     */
    @Deprecated
    private static Future<JsonArray> ESHandler(JsonObject query, JsonArray pro, JsonArray conso, JsonArray ressource) {
        Promise<JsonArray> promise = Promise.promise();

        execute(query, pro, conso, ressource)
                .onSuccess(itemList -> promise.complete(IModelHelper.toJsonArray(itemList)))
                .onFailure(promise::fail);

        return promise.future();
    }

    public static void search_All(Handler<Either<String, JsonArray>> handler) {
        JsonObject query = new JsonObject()
                .put(Field.FROM, 0)
                .put(Field.SIZE, PAGE_SIZE)
                .put(Field.QUERY, new JsonObject().put(Field.MATCH_ALL, new JsonObject()));

        ESHandler(query, new JsonArray(), new JsonArray(), new JsonArray())
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }


    public static void plainTextSearch(String query, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        for (String field : PLAIN_TEXT_FIELDS) {
            JsonObject regexp = regexpField(field, query);
            should.add(regexp);
        }

        regexSearchBool(handler, should, resultFieldsExpected);
    }

    private static void regexSearchBool(Handler<Either<String, JsonArray>> handler, JsonArray should, List<String> resultFieldsExpected) {
        JsonObject regexpBool = new JsonObject()
                .put(Field.SHOULD, should);
        JsonArray must = new JsonArray()
                .add(new JsonObject().put(Field.BOOL, regexpBool));

        JsonObject bool = new JsonObject()
                .put(Field.MUST, must);
        JsonObject queryObject = new JsonObject()
                .put(Field.BOOL, bool);

        ESHandler(esQueryObject(queryObject, resultFieldsExpected), new JsonArray(), new JsonArray(), new JsonArray())
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static Future<JsonArray> plainTextSearchName(String query, List<String> resultFieldsExpected) {
        Promise<JsonArray> promise = Promise.promise();

        plainTextSearchName(query, resultFieldsExpected, FutureHelper.handlerEitherPromise(promise));

        return promise.future();
    }

    public static void plainTextSearchName(String query, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {
        JsonArray should = new JsonArray();
        JsonObject regexTitre = regexpField(Field.TITRE, query);
        JsonObject regexEAN = regexpField(Field.EAN, query);
        JsonObject regexArk = regexpField(Field.ARK, query);
        JsonObject regexAuthor = regexpField(Field.AUTEUR, query);
        JsonObject regexDistributor = regexpField(Field.DISTRIBUTEUR, query);
        JsonObject regexEditor = regexpField(Field.EDITEUR, query);
        should.add(regexTitre).add(regexEAN).add(regexArk).add(regexAuthor).add(regexDistributor).add(regexEditor);

        regexSearchBool(handler, should, resultFieldsExpected);
    }

    public static void filters(FilterItemModel filterItemModel, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {
        JsonArray term = new JsonArray();
        JsonObject filter = new JsonObject();
        JsonArray query = new JsonArray();
        JsonArray conso = new JsonArray();
        JsonArray pro = new JsonArray();
        JsonArray ressource = new JsonArray();


        prepareFilterES(filterItemModel, term, query, conso, pro, ressource);

        filter.put(Field.FILTER, term.addAll(query));

        JsonObject queryObject = new JsonObject()
                .put(Field.BOOL, filter);

        ESHandler(esQueryObject(queryObject, resultFieldsExpected), pro, conso, ressource)
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    private static void prepareFilterES(FilterItemModel filters, JsonArray term, JsonArray query,
                                        JsonArray conso, JsonArray pro, JsonArray ressource) {
        JsonObject filtersJson = filters.toJson();
        for (String key : filtersJson.fieldNames()) {
            switch (key) {
                case Field.DISCIPLINES: {
                    JsonObject terms = new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.DISCIPLINES_FIELD, filtersJson.getJsonArray(key)));
                    JsonObject bool = new JsonObject().put(Field.BOOL, new JsonObject().put(Field.FILTER, terms));
                    JsonObject nested = new JsonObject().put(Field.PATH, ItemFilterField.DISCIPLINES_FIELD.split("\\.")[0])
                            .put(Field.QUERY, bool);
                    query.add(new JsonObject().put(Field.NESTED, nested));
                    break;
                }
                case Field.CLASSES: {
                    JsonObject terms = new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.CLASSES_FIELD, filtersJson.getJsonArray(key)));
                    JsonObject bool = new JsonObject().put(Field.BOOL, new JsonObject().put(Field.FILTER, terms));
                    JsonObject nested = new JsonObject().put(Field.PATH, ItemFilterField.CLASSES_FIELD.split("\\.")[0])
                            .put(Field.QUERY, bool);
                    query.add(new JsonObject().put(Field.NESTED, nested));
                    break;
                }
                case Field.GRADES: {
                    JsonObject terms = new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.GRADES_FIELD, filtersJson.getJsonArray(key)));
                    JsonObject bool = new JsonObject().put(Field.BOOL, new JsonObject().put(Field.FILTER, terms));
                    JsonObject nested = new JsonObject().put(Field.PATH, ItemFilterField.GRADES_FIELD.split("\\.")[0])
                            .put(Field.QUERY, bool);
                    query.add(new JsonObject().put(Field.NESTED, nested));
                    break;
                }
                case Field.ITEM_TYPES: {
                    JsonArray values = filtersJson.getJsonArray(key);
                    if (values.size() > 0) {
                        switch (values.getString(0)) {
                            case Field.CONSUMABLE:
                                conso.add(true);
                                break;
                            case Field.RESSOURCE:
                                ressource.add(true);
                                break;
                            case Field.MANUAL:
                                conso.add(false);
                                ressource.add(false);
                                break;
                        }
                    }
                    break;
                }
                case Field.STRUCTURE_SECTORS: {
                    JsonArray values = filtersJson.getJsonArray(key);
                    if (values.size() == 1) {
                        pro.add(values.getString(0).equals(Field.STRUCTURE_PRO));
                    }
                    break;
                }
                case Field.TARGETS: {
                    term.add(new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.TARGET, filtersJson.getJsonArray(key))));
                    break;
                }
                case Field.EDITORS: {
                    term.add(new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.EDITORS_FIELD, filtersJson.getJsonArray(key))));
                    break;
                }
                case Field.DISTRIBUTORS: {
                    term.add(new JsonObject().put(Field.TERMS, new JsonObject().put(ItemFilterField.DISTRIBUTORS_FIELD, filtersJson.getJsonArray(key))));
                    break;
                }
                case Field.CATALOGS: {
                    term.add(new JsonObject().put(Field.TERMS, new JsonObject().put(Field._INDEX, filtersJson.getJsonArray(key))));
                    break;
                }
                case Field.BOOKSELLERS: {
                    term.add(new JsonObject().put(Field.TERMS, new JsonObject().put(ItemField.BOOKSELLER, filtersJson.getJsonArray(key))));
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    public static void searchById(String id, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject match = new JsonObject().put(Field._ID, id);
        queryObject.put(Field.MATCH, match);
        ESHandler(esQueryObject(queryObject, resultFieldsExpected), new JsonArray(), new JsonArray(), new JsonArray())
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static Future<JsonArray> searchByIds(List<String> ids, List<String> resultFieldsExpected) {
        Promise<JsonArray> promise = Promise.promise();

        searchByIds(ids, resultFieldsExpected, FutureHelper.handlerEitherPromise(promise));

        return promise.future();
    }

    public static void searchByIds(List<String> ids, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {

        JsonObject queryObject = new JsonObject();
        JsonObject terms = new JsonObject().put(Field._ID, new JsonArray(ids));
        queryObject.put(Field.TERMS, terms);
        ESHandler(esQueryObject(queryObject, resultFieldsExpected), new JsonArray(), new JsonArray(), new JsonArray())
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    public static Future<List<Item>> searchfilter(FilterItemModel filters, List<String> resultFieldsExpected) {
        Promise<List<Item>> promise = Promise.promise();
        JsonArray term = new JsonArray();
        JsonArray should = new JsonArray();
        JsonObject request = new JsonObject();
        JsonArray query = new JsonArray();
        JsonArray conso = new JsonArray();
        JsonArray pro = new JsonArray();
        JsonArray ressource = new JsonArray();


        if (filters.getSearchingText() != null) {
            for (String field : PLAIN_TEXT_FIELDS) {
                JsonObject regexp = regexpField(field, filters.getSearchingText());
                should.add(regexp);
            }
        }

        prepareFilterES(filters, term, query, conso, pro, ressource);
        request.put(Field.FILTER, term.addAll(query));

        if (filters.getSearchingText() != null) {
            request.put(Field.MINIMUM_SHOULD_MATCH, 1)
                    .put(Field.SHOULD, should);
        }

        JsonObject queryObject = new JsonObject()
                .put(Field.BOOL, request);

        execute(esQueryObject(queryObject, resultFieldsExpected), pro, conso, ressource)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
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

    private static Future<JsonArray> executeEsSearch(JsonObject query) {
        Promise<JsonArray> promise = Promise.promise();
        ElasticSearch.getInstance().search(RESOURCE_TYPE_NAME, query, search -> {
            if (search.failed()) {
                promise.fail(search.cause());
            } else {
                List resources = parseEsResponse(search.result());
                promise.complete(new JsonArray(resources));
            }
        });
        return promise.future();
    }

    private static List parseEsResponse(JsonObject esResponse) {
        return esResponse
                .getJsonObject("hits", new JsonObject())
                .getJsonArray("hits", new JsonArray()).getList();
    }

    private static JsonObject esQueryObject(JsonObject query, List<String> resultFieldsExpected) {
        JsonObject queryResult = new JsonObject()
                .put(Field.QUERY, query)
                .put(Field.FROM, 0)
                .put(Field.SIZE, PAGE_SIZE)
                .put(Field.SORT, new JsonArray()
                        .add(new JsonObject().put(Field._INDEX, Field.ASC))
                        .add(new JsonObject().put(Field.TITRE, Field.ASC))
                );

        if (resultFieldsExpected != null && !resultFieldsExpected.isEmpty()) {
            queryResult.put(Field._SOURCE, new JsonArray(new ArrayList<>(resultFieldsExpected)));
        }

        return queryResult;
    }


    private static JsonObject regexpField(String field, String query) {
        JsonObject regexp = new JsonObject()
                .put(field, formatRegexp(query));

        return new JsonObject().put("regexp", regexp);
    }
}
