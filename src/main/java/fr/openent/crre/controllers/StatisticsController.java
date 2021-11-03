package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.cron.statistics;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.StatisticsService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultStatisticsService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;

public class StatisticsController extends BaseController {
    private final StatisticsService statisticsService;
    private final StructureService structureService;
    private final statistics statCron;

    public StatisticsController() {
        this.statisticsService = new DefaultStatisticsService(Crre.crreSchema);
        this.structureService = new DefaultStructureService(Crre.crreSchema);
        this.statCron = new statistics(Vertx.vertx());
    }

    @Get("region/statistics/structures")
    @ApiDoc("Get statistics by structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getStatisticsByStructure(final HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = getParams(request);
        statisticsService.getStatsByStructure(params, event -> {
            if (event.isRight()) {
                JsonArray stats = event.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch");
                renderJson(request, stats);
            } else {
                log.error("An error when you want get stats by structures ",
                        event.left().getValue());
                request.response().setStatusCode(400).end();
            }
        });
    }

    private HashMap<String, ArrayList<String>> getParams(HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = new HashMap<>();
        if (request.params().contains("year")) {
            params.put("year", new ArrayList<>(request.params().getAll("year")));
        }
        if (request.params().contains("catalog")) {
            params.put("catalog", new ArrayList<>(request.params().getAll("catalog")));
        }
        if (request.params().contains("public")) {
            params.put("public", new ArrayList<>(request.params().getAll("public")));
        }
        if (request.params().contains("reassort")) {
            params.put("reassort", new ArrayList<>(request.params().getAll("reassort")));
        }
        if (request.params().contains("orientation")) {
            params.put("orientation", new ArrayList<>(request.params().getAll("orientation")));
        }
        if (request.params().contains("city")) {
            params.put("city", new ArrayList<>(request.params().getAll("city")));
        }
        if (request.params().contains("region")) {
            params.put("region", new ArrayList<>(request.params().getAll("region")));
        }
        if (request.params().contains("consummation")) {
            params.put("consummation", new ArrayList<>(request.params().getAll("consummation")));
        }
        if (request.params().contains("query")) {
            params.put("query", new ArrayList<>(request.params().getAll("query")));
        }
        return params;
    }

    @Get("region/statistics")
    @ApiDoc("Get statistics")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getStatistics(final HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = getParams(request);
        boolean isReassort = false;
        if (!(params.get("reassort") == null)) {
            if (params.get("reassort").size() == 1) {
                isReassort = true;
            }
        }

        List<Future> futures = new ArrayList<>();
        Future<JsonObject> getNumericRessourcesFuture = Future.future();
        Future<JsonObject> getPaperRessourcesFuture = Future.future();
        Future<JsonObject> getAllRessourcesFuture = Future.future();
        Future<JsonObject> getRessourcesFuture = Future.future();
        Future<JsonObject> getOrdersFuture = Future.future();
        Future<JsonObject> getLicencesFuture = Future.future();
        Future<JsonObject> getStructuresMoreOneOrderFuture = Future.future();
        Future<JsonObject> getAllStructuresFuture = Future.future();
        futures.add(getNumericRessourcesFuture);
        futures.add(getPaperRessourcesFuture);
        futures.add(getAllRessourcesFuture);
        futures.add(getRessourcesFuture);
        futures.add(getOrdersFuture);
        futures.add(getLicencesFuture);
        futures.add(getStructuresMoreOneOrderFuture);
        futures.add(getAllStructuresFuture);

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                JsonObject allNumericRessources = new JsonObject().put("allNumericRessources", getNumericRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch"));
                JsonObject allPaperRessources = new JsonObject().put("allPaperRessources", getPaperRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch"));
                JsonObject allRessources = new JsonObject().put("allRessources", getAllRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch"));
                JsonObject allStructures = new JsonObject().put("allStructures", new JsonObject().put("percentage", 0).put("structures", 0).put("structuresMoreOneOrder", 0));
                JsonArray ressourcesArray = getRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                JsonObject ressources = new JsonObject().put("ressources", ressourcesArray.size() > 0 ? ressourcesArray.getJsonObject(0).getDouble("total") : 0);

                JsonArray ordersArray = getOrdersFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                JsonObject orders = new JsonObject().put("orders", ordersArray.size() > 0 ? ordersArray.getJsonObject(0).getInteger("total") : 0);

                JsonArray licencesArray = getLicencesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                JsonObject licencesResult = licencesArray.size() > 0 ? licencesArray.getJsonObject(0) : new JsonObject().put("amount", 0).put("initial_amount", 0);

                JsonArray structuresMoreOneOrderResult = getStructuresMoreOneOrderFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                JsonArray structuresResult = getAllStructuresFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                boolean multiplePublic = !(request.params().getAll("public").size() == 1);
                if (structuresMoreOneOrderResult.size() > 0 || structuresResult.size() > 0) {
                    computePercentageStructures(structuresMoreOneOrderResult, structuresResult, multiplePublic);
                    allStructures = new JsonObject().put("allStructures", computeTotalStructures(structuresMoreOneOrderResult, structuresResult, multiplePublic));
                }

                JsonObject concatStats = allRessources
                        .mergeIn(allNumericRessources)
                        .mergeIn(allPaperRessources)
                        .mergeIn(ressources)
                        .mergeIn(orders)
                        .mergeIn(new JsonObject().put("licences", licencesResult))
                        .mergeIn(new JsonObject().put("structuresMoreOneOrder", structuresMoreOneOrderResult))
                        .mergeIn(new JsonObject().put("structures", structuresResult))
                        .mergeIn(allStructures);
                JsonArray stats = new JsonArray().add(concatStats);
                renderJson(request, stats);
            } else {
                log.error("An error when you want get all stats ",
                        event.cause());
                request.response().setStatusCode(400).end();
            }

        });

        statisticsService.getOrdersCompute("numeric_ressources", params, true, isReassort, handlerJsonObject(getNumericRessourcesFuture));
        statisticsService.getOrdersCompute("paper_ressources", params, true, isReassort, handlerJsonObject(getPaperRessourcesFuture));
        statisticsService.getOrdersCompute("all_ressources", params, true, isReassort, handlerJsonObject(getAllRessourcesFuture));
        statisticsService.getOrdersCompute("ressources_total", params, false, isReassort, handlerJsonObject(getRessourcesFuture));
        statisticsService.getOrdersCompute("order_year", params, false, isReassort, handlerJsonObject(getOrdersFuture));

        statisticsService.getLicencesCompute(params, handlerJsonObject(getLicencesFuture));

        statisticsService.getStructureCompute(params, true, isReassort, handlerJsonObject(getStructuresMoreOneOrderFuture));
        statisticsService.getStructureCompute(params, false, isReassort, handlerJsonObject(getAllStructuresFuture));


    }

    @Get("region/stats/mongo")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void exportMongoStat(HttpServerRequest request) {
        log.info("CRRE statistics started");
        statCron.insertStatistics(event1 -> {
            if (event1.isRight()) {
                log.info("Update statistics launch successful");
            } else {
                log.info("Update statistics failed");
            }
        });
    }

    @Get("/region/statistics/years")
    @ApiDoc("get all years from statistics ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getAllYears(HttpServerRequest request) {
        statisticsService.getAllYears(event -> {
            JsonArray yearsResult = event.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch");
            renderJson(request, yearsResult);
        });
    }

    private void computePercentageStructures(JsonArray structuresMoreOneOrderResult, JsonArray structuresResult,
                                             boolean multiplePublic) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        if (multiplePublic && structuresMoreOneOrderResult.size() > 1 && structuresResult.size() > 1) {
            // Add percentage for private
            Double percentagePrivate = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") /
                    structuresResult.getJsonObject(0).getDouble("total") * 100;
            structuresMoreOneOrderResult.getJsonObject(0).put("percentage", df2.format(percentagePrivate));

            // Add percentage for public
            Double percentagePublic = structuresMoreOneOrderResult.getJsonObject(1).getDouble("total") /
                    structuresResult.getJsonObject(1).getDouble("total") * 100;
            structuresMoreOneOrderResult.getJsonObject(1).put("percentage", df2.format(percentagePublic));
        } else {
            // Add percentage for private or public
            double totalStructuresMoreOneOrder = structuresMoreOneOrderResult.size() > 0 ?
                    structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") : 0.0;
            Double percentage = totalStructuresMoreOneOrder /
                    structuresResult.getJsonObject(0).getDouble("total") * 100;
            if (!structuresMoreOneOrderResult.isEmpty()) {
                structuresMoreOneOrderResult.getJsonObject(0).put("percentage", df2.format(percentage));
            }
        }
    }

    private JsonObject computeTotalStructures(JsonArray structuresMoreOneOrderResult, JsonArray structuresResult,
                                              boolean multiplePublic) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        JsonObject allStructuresObject = new JsonObject();
        if (structuresMoreOneOrderResult.size() > 0 || structuresResult.size() > 0) {
            //Compute total structure
            Double totalStructure = structuresResult.size() > 1 ?
                    structuresResult.getJsonObject(0).getDouble("total") + structuresResult.getJsonObject(1).getDouble("total") :
                    structuresResult.getJsonObject(0).getDouble("total");
            Double totalStructureMoreOneOrder = 0.0;
            if (structuresMoreOneOrderResult.size() > 1) {
                totalStructureMoreOneOrder = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") +
                        structuresMoreOneOrderResult.getJsonObject(1).getDouble("total");
            }
            if (structuresMoreOneOrderResult.size() == 1) {
                totalStructureMoreOneOrder = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total");
            }

            allStructuresObject = new JsonObject().put("structures", totalStructure);
            JsonObject allStructuresMoreOneOrderObject = new JsonObject().put("structuresMoreOneOrder", totalStructureMoreOneOrder);
            JsonObject allStructuresPercentage =
                    new JsonObject().put("percentage", df2.format(totalStructureMoreOneOrder / totalStructure * 100));
            allStructuresObject.mergeIn(allStructuresMoreOneOrderObject).mergeIn(allStructuresPercentage);
        }
        return allStructuresObject;
    }
}
