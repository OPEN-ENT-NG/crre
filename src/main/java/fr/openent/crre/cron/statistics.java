package fr.openent.crre.cron;

import fr.openent.crre.Crre;
import fr.openent.crre.service.impl.DefaultStatisticsService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static fr.openent.crre.Crre.CONFIG;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;

public class statistics extends ControllerHelper implements Handler<Long> {

    private final DefaultStatisticsService statisticsService;
    private final DefaultStructureService structureService;



    public statistics(Vertx vertx) {
        this.statisticsService = new DefaultStatisticsService(Crre.crreSchema);
        this.structureService = new DefaultStructureService( Crre.crreSchema);
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.info("CRRE statistics cron started");
        insertStatistics(event1 -> {
                if (event1.isRight())
                    log.info("Cron statistics launch successful");
                else
                    log.info("Cron synchonisation not full");
        });
    }

    public void insertStatistics(final Handler<Either<String, JsonObject>> eitherHandler) {
        List<Future> futures = new ArrayList<>();
        structureService.getAllStructuresDetail(event -> {
            if(event.isRight()) {
                JsonArray structures = event.right().getValue();
                for (int i = 0; i < structures.size(); i++) {
                    Future<JsonObject> addStructureStatFuture = Future.future();
                    futures.add(addStructureStatFuture);
                    JsonObject structure = structures.getJsonObject(i);
                    // All compute with structure object
                    structure.put("date", LocalDate.now().toString());
                    List<Future> futuresStat = new ArrayList<>();
                    Future<JsonArray> freeLicenceFuture = Future.future();
                    Future<JsonArray> totalRessourcesFuture = Future.future();
                    Future<JsonArray> ordersByYearFuture = Future.future();
                    Future<JsonArray> ordersByCampaignFuture = Future.future();
                    Future<JsonArray> ordersReassortFuture = Future.future();
                    Future<JsonArray> allRessourcesFuture = Future.future();
                    Future<JsonArray> numeriqueRessourceFuture = Future.future();
                    Future<JsonArray> papierRessourceFuture = Future.future();
                    futuresStat.add(freeLicenceFuture);
                    futuresStat.add(ordersByCampaignFuture);
                    futuresStat.add(ordersByYearFuture);
                    futuresStat.add(totalRessourcesFuture);
                    futuresStat.add(ordersReassortFuture);
                    futuresStat.add(allRessourcesFuture);
                    futuresStat.add(numeriqueRessourceFuture);
                    futuresStat.add(papierRessourceFuture);

                    CompositeFuture.all(futuresStat).setHandler(event2 -> {
                        if (event2.succeeded()) {
                            JsonArray free_total = freeLicenceFuture.result();
                            JsonArray ressources_total = totalRessourcesFuture.result();
                            JsonArray order_year = ordersByYearFuture.result();
                            JsonArray order_campaign = ordersByCampaignFuture.result();
                            JsonArray order_reassort = ordersReassortFuture.result();
                            JsonArray all_ressources = allRessourcesFuture.result();
                            JsonArray numeric_ressources = numeriqueRessourceFuture.result();
                            JsonArray paper_ressources = papierRessourceFuture.result();

                            if(free_total.size() > 0 || ressources_total.size() > 0) {
                                JsonObject stats = new JsonObject();
                                stats.put("free_total", free_total);
                                stats.put("ressources_total", ressources_total);
                                stats.put("order_year", order_year);
                                stats.put("order_campaign", order_campaign);
                                stats.put("order_reassort", order_reassort);
                                stats.put("all_ressources", all_ressources);
                                stats.put("numeric_ressources", numeric_ressources);
                                stats.put("paper_ressources", paper_ressources);
                                structure.put("stats", stats);
                            }
                            statisticsService.exportMongo(structure, handlerJsonObject(addStructureStatFuture));

                        } else {
                            log.info("Failed to retrieve statistics for a structure");
                            eitherHandler.handle(new Either.Left<>("Failed to retrieve statistics for a structure"));
                        }
                    });

                    // Commandes

                    statisticsService.getOrdersByYear(structure.getString("id_structure"), handlerJsonArray(ordersByYearFuture));
                    statisticsService.getOrdersByCampaign(structure.getString("id_structure"), handlerJsonArray(ordersByCampaignFuture));
                    statisticsService.getOrdersReassort(structure.getString("id_structure"), handlerJsonArray(ordersReassortFuture));

                    // Licences

                    statisticsService.getFreeLicences(structure.getString("id_structure"), handlerJsonArray(freeLicenceFuture));

                    // Finances

                    statisticsService.getTotalRessources(structure.getString("id_structure"), handlerJsonArray(totalRessourcesFuture));

                    // Ressources

                    statisticsService.getRessources(structure.getString("id_structure"), null, handlerJsonArray(allRessourcesFuture));
                    statisticsService.getRessources(structure.getString("id_structure"), "articlenumerique", handlerJsonArray(numeriqueRessourceFuture));
                    statisticsService.getRessources(structure.getString("id_structure"), "articlepapier", handlerJsonArray(papierRessourceFuture));

                }
                CompositeFuture.all(futures).setHandler(event2 -> {
                    if (event2.succeeded()) {
                        log.info("Insert structures in mongo successful");
                        eitherHandler.handle(new Either.Right<>(new JsonObject().put("result", "Insert structures in mongo successful")));
                    } else {
                        log.info("Failed to insert structures in mongo");
                        eitherHandler.handle(new Either.Left<>("Failed to insert structures in mongo"));
                    }
                });
            }
        });
    }
}

