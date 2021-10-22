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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
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
        return params;
    }

    @Get("region/statistics")
    @ApiDoc("Get statistics")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getStatistics(final HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = getParams(request);
        boolean isReassort = false;
        if(!(params.get("reassort") == null)) {
            if(params.get("reassort").size() == 1) {
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
                if(structuresMoreOneOrderResult.size() > 0 && structuresResult.size() > 0) {
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
    public void exportMongoStat(final HttpServerRequest request) {
        log.info("CRRE statistics started");
        statCron.insertStatistics(event1 -> {
            if (event1.isRight()) {
                log.info("Update statistics launch successful");
            } else {
                log.info("Update statistics failed");
            }
        });
}

    @Get("region/orders/mongo")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void exportMongo(final HttpServerRequest request) {
        List<Future> futures = new ArrayList<>();
        structureService.getAllStructuresDetail(event -> {
            if (event.isRight()) {
                JsonArray structures = event.right().getValue();
                for (int i = 0; i < structures.size(); i++) {
                    Future<JsonObject> addStructureStatFuture = Future.future();
                    futures.add(addStructureStatFuture);
                    JsonObject structure = structures.getJsonObject(i);
                    // All compute with structure object
                    structure.put("date", LocalDate.now().toString());
                    List<Future> futuresStat = new ArrayList<>();
                    Future<JsonArray> licencesFuture = Future.future();
                    Future<JsonArray> freeLicenceFuture = Future.future();
                    Future<JsonArray> totalRessourcesFuture = Future.future();
                    Future<JsonArray> ordersByYearFuture = Future.future();
                    Future<JsonArray> ordersByCampaignFuture = Future.future();
                    Future<JsonArray> numeriqueRessourceFuture = Future.future();
                    Future<JsonArray> papierRessourceFuture = Future.future();
                    futuresStat.add(licencesFuture);
                    futuresStat.add(freeLicenceFuture);
                    futuresStat.add(ordersByCampaignFuture);
                    futuresStat.add(ordersByYearFuture);
                    futuresStat.add(totalRessourcesFuture);
                    futuresStat.add(numeriqueRessourceFuture);
                    futuresStat.add(papierRessourceFuture);

                    CompositeFuture.all(futuresStat).setHandler(event2 -> {
                        if (event2.succeeded()) {
                            JsonArray licences = licencesFuture.result();
                            JsonArray free_total = freeLicenceFuture.result();
                            JsonArray ressources_total = totalRessourcesFuture.result();
                            JsonArray order_year = ordersByYearFuture.result();
                            JsonArray order_campaign = ordersByCampaignFuture.result();
                            JsonArray numeric_ressources = numeriqueRessourceFuture.result();
                            JsonArray paper_ressources = papierRessourceFuture.result();

                            if (free_total.size() > 0 || ressources_total.size() > 0) {
                                JsonObject stats = new JsonObject();
                                // Every 30/04, add a new line in licences mongo db with new year
                                if (LocalDate.now().getMonthValue() == 5 && LocalDate.now().getDayOfMonth() == 1) {
                                    licences.add(licences.getJsonObject(0));
                                }
                                // Put a n+1 year if after 30/04
                                if (LocalDate.now().getMonthValue() < 5) {
                                    licences.getJsonObject(licences.size() - 1).put("year", LocalDate.now().getYear() + "");
                                } else {
                                    licences.getJsonObject(licences.size() - 1).put("year", LocalDate.now().getYear() + 1 + "");
                                }
                                double licencesPercentage = (licences.getJsonObject(licences.size() - 1).getInteger("amount") / licences.getJsonObject(0).getInteger("initial_amount")) * 100;
                                licences.getJsonObject(licences.size() - 1).put("percentage", 100 - licencesPercentage);
                                stats.put("licences", licences);
                                stats.put("free_total", free_total);
                                stats.put("ressources_total", ressources_total);
                                stats.put("order_year", order_year);
                                stats.put("order_campaign", order_campaign);
                                stats.put("numeric_ressources", numeric_ressources);
                                stats.put("paper_ressources", paper_ressources);
                            }
                            String JSON_DATA = "{ \"licences\":[ { \"amount\":5000, \"initial_amount\":10000, \"year\":\"2021\", \"percentage\":50 }, { \"amount\":2000, \"initial_amount\":10000, \"year\":\"2020\", \"percentage\":20 }, { \"amount\":3000, \"initial_amount\":10000, \"year\":\"2019\", \"percentage\":30 } ], \"free_total\":[ { \"total\":150, \"reassort\":false, \"year\":\"2021\" }, { \"total\":200, \"reassort\":false, \"year\":\"2020\" }, { \"total\":175, \"reassort\":false, \"year\":\"2019\" }, { \"total\":15, \"reassort\":true, \"year\":\"2021\" }, { \"total\":20, \"reassort\":true, \"year\":\"2020\" }, { \"total\":17, \"reassort\":true, \"year\":\"2019\" } ], \"ressources_total\":[ { \"total\":200000, \"reassort\":false, \"year\":\"2021\" }, { \"total\":250000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":225000, \"reassort\":false, \"year\":\"2019\" }, { \"total\":2000, \"reassort\":true, \"year\":\"2021\" }, { \"total\":2500, \"reassort\":true, \"year\":\"2020\" }, { \"total\":2250, \"reassort\":true, \"year\":\"2019\" } ], \"order_year\":[ { \"total\":1000, \"reassort\":false, \"year\":\"2021\" }, { \"total\":1500, \"reassort\":false, \"year\":\"2020\" }, { \"total\":1250, \"reassort\":false, \"year\":\"2019\" }, { \"total\":100, \"reassort\":true, \"year\":\"2021\" }, { \"total\":150, \"reassort\":true, \"year\":\"2020\" }, { \"total\":125, \"reassort\":true, \"year\":\"2019\" } ], \"order_campaign\":[ { \"name\":\"Campagne papier\", \"id\":2, \"total\":750 }, { \"name\":\"Campagne numérique\", \"id\":1, \"total\":250 } ], \"numeric_ressources\":[ { \"total\":1500, \"reassort\":false, \"year\":\"2021\" }, { \"total\":2000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":1750, \"reassort\":false, \"year\":\"2019\" }, { \"total\":150, \"reassort\":true, \"year\":\"2021\" }, { \"total\":200, \"reassort\":true, \"year\":\"2020\" }, { \"total\":175, \"reassort\":true, \"year\":\"2019\" } ], \"paper_ressources\":[ { \"total\":500, \"reassort\":false, \"year\":\"2021\" }, { \"total\":1000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":750, \"reassort\":false, \"year\":\"2019\" }, { \"total\":50, \"reassort\":true, \"year\":\"2021\" }, { \"total\":100, \"reassort\":true, \"year\":\"2020\" }, { \"total\":75, \"reassort\":true, \"year\":\"2019\" } ] }";
                            JsonObject statss = new JsonObject(JSON_DATA);
                            structure.put("stats", statss);
                            statisticsService.exportMongo(structure, handlerJsonObject(addStructureStatFuture));
                        }
                    });

                    // Licences

                    statisticsService.getLicences(structure.getString("id_structure"), handlerJsonArray(licencesFuture));

                    // Commandes

                    statisticsService.getStats("orders", structure.getString("id_structure"), handlerJsonArray(ordersByYearFuture));
                    statisticsService.getOrdersByCampaign(structure.getString("id_structure"), handlerJsonArray(ordersByCampaignFuture));

                    // Licences

                    statisticsService.getStats("free", structure.getString("id_structure"), handlerJsonArray(freeLicenceFuture));

                    // Finances

                    statisticsService.getStats("ressources", structure.getString("id_structure"), handlerJsonArray(totalRessourcesFuture));

                    // Ressources

                    statisticsService.getStats("articlenumerique", structure.getString("id_structure"), handlerJsonArray(numeriqueRessourceFuture));
                    statisticsService.getStats("articlepapier", structure.getString("id_structure"), handlerJsonArray(papierRessourceFuture));

                }
                CompositeFuture.all(futures).setHandler(event2 -> {
                    if (event2.succeeded()) {
                        log.info("export mongo ok");
                    }
                });
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

    private void computePercentageStructures(JsonArray structuresMoreOneOrderResult, JsonArray structuresResult, boolean multiplePublic) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        if (multiplePublic && structuresMoreOneOrderResult.size() > 1 && structuresResult.size() > 1) {
            // Add percentage for private
            Double percentagePrivate = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") / structuresResult.getJsonObject(0).getDouble("total") * 100;
            structuresMoreOneOrderResult.getJsonObject(0).put("percentage", df2.format(percentagePrivate));

            // Add percentage for public
            Double percentagePublic = structuresMoreOneOrderResult.getJsonObject(1).getDouble("total") / structuresResult.getJsonObject(1).getDouble("total") * 100;
            structuresMoreOneOrderResult.getJsonObject(1).put("percentage", df2.format(percentagePublic));
        } else {
            // Add percentage for private or public
            Double percentage = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") / structuresResult.getJsonObject(0).getDouble("total") * 100;
            structuresMoreOneOrderResult.getJsonObject(0).put("percentage", df2.format(percentage));
        }
    }

    private JsonObject computeTotalStructures(JsonArray structuresMoreOneOrderResult, JsonArray structuresResult, boolean multiplePublic) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        JsonObject allStructuresObject;
        if (multiplePublic && structuresMoreOneOrderResult.size() > 1 && structuresResult.size() > 1) {
            //Compute total structure
            double totalStructure = structuresResult.getJsonObject(0).getDouble("total") + structuresResult.getJsonObject(1).getDouble("total");
            double totalStructureMoreOneOrder = structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") + structuresMoreOneOrderResult.getJsonObject(1).getDouble("total");

            allStructuresObject = new JsonObject().put("structures", totalStructure);
            JsonObject allStructuresMoreOneOrderObject = new JsonObject().put("structuresMoreOneOrder", totalStructureMoreOneOrder);
            JsonObject allStructuresPercentage = new JsonObject().put("percentage", df2.format(totalStructureMoreOneOrder / totalStructure * 100));
            allStructuresObject.mergeIn(allStructuresMoreOneOrderObject).mergeIn(allStructuresPercentage);

        } else {
            //Compute total structure
            allStructuresObject = new JsonObject().put("structures", structuresResult.getJsonObject(0).getInteger("total"));
            JsonObject allStructuresMoreOneOrderObject = new JsonObject().put("structuresMoreOneOrder", structuresMoreOneOrderResult.getJsonObject(0).getInteger("total"));
            JsonObject allStructuresPercentage = new JsonObject().put("percentage", df2.format(structuresMoreOneOrderResult.getJsonObject(0).getDouble("total") / structuresResult.getJsonObject(0).getDouble("total") * 100));
            allStructuresObject.mergeIn(allStructuresMoreOneOrderObject).mergeIn(allStructuresPercentage);
        }
        return allStructuresObject;
    }
}
