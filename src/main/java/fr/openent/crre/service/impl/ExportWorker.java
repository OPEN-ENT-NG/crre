package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.config.ConfigModel;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StorageService;
import fr.openent.crre.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static java.lang.Math.min;


public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    public static final String ORDER_REGION = "saveOrderRegion";
    private OrderRegionService orderRegionService;
    private StructureService structureService;
    private StorageService storageService;
    private WorkspaceHelper workspaceHelper;
    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);

    @Override
    public void start() {
        super.start();
        String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
        Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
        Storage storage = new StorageFactory(vertx).getStorage();
        ServiceFactory serviceFactory = new ServiceFactory(vertx, null, null, storage);
        this.orderRegionService = serviceFactory.getOrderRegionService();
        this.structureService = serviceFactory.getStructureService();
        this.storageService = serviceFactory.getStorageService();
        this.workspaceHelper = new WorkspaceHelper(eb, storage);
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        log.info(String.format("[Crre@%s] ExportWorker called ", this.getClass().getSimpleName()));
        JsonObject paramsEB = message.body();
        processExport(paramsEB, message);
    }


    private void processExport(JsonObject params, Message<JsonObject> message) {
        Handler<Either<String, JsonObject>> exportHandler = event -> {
            log.info(String.format("[Crre@%s] exportHandler", this.getClass().getSimpleName()));
            if (event.isRight()) {
                log.info("[Crre@ExportWorker] Export finish");
                message.reply(new JsonObject().put(Field.STATUS, Field.OK).put("data",event.right().getValue()));
            } else {
                log.error(event.left().getValue());
            }
        };
        chooseExport(params, exportHandler);
    }


    private void chooseExport(JsonObject body, Handler<Either<String, JsonObject>> exportHandler) {
        final String action = body.getString("action", "");
        JsonObject params = body.getJsonObject("params");
        log.info("[Crre@ExportWorker::chooseExport ] " + this.getClass().toString() + "  Export Type : " + action);
        switch (action) {
            case ORDER_REGION:
                processOrderRegion(params, exportHandler);
                break;
            default:
                log.error("[Crre@ExportWorker::catchError ] Error for create file export excel");
                break;
        }
    }


    private void processOrderRegion(JsonObject params, Handler<Either<String, JsonObject>> exportHandler) {
        log.info("[Crre@ExportWorker::processOrderRegion ] Process orders");
        JsonArray idsOrders = params.getJsonArray("idsOrders");
        JsonArray idsEquipments = params.getJsonArray("idsEquipments");
        JsonArray idsStructures = params.getJsonArray("idsStructures");
        String idUser = params.getString("idUser");
        boolean old = params.getBoolean("old");

        JsonArray idStructures = new JsonArray();
        for (Object structureId : idsStructures) {
            idStructures.add((String) structureId);
        }
        List<Integer> listOrders = idsOrders.getList();
        List<Future> futures = new ArrayList<>();
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();
        futures.add(structureFuture);
        futures.add(equipmentsFuture);

        getOrderRecursively(old, 0, listOrders, futures);

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                List<Long> ordersClientId = new ArrayList<>();
                JsonArray orderRegion = new JsonArray();
                for (int i = 2; i < futures.size(); i++) {
                    orderRegion.addAll((JsonArray) futures.get(i).result());
                }
                orderRegionService.beautifyOrders(structures, orderRegion, equipments, ordersClientId);
                writeCSVFile(exportHandler, idUser, orderRegion, listOrders.size(), 0);
            } else {
                log.error("ERROR [Crre@ExportWorker::processOrderRegion] : " + event.cause().getMessage(), event.cause());
                exportHandler.handle(new Either.Left<>("ERROR [Crre@ExportWorker::processOrderRegion] : " + event.cause().getMessage()));
            }


        });

        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipments.getList(), handlerJsonArray(equipmentsFuture));
    }

    private void writeCSVFile(Handler<Either<String, JsonObject>> exportHandler, String idUser, JsonArray orderRegion, int ordersSize, int e) {
        JsonArray orderRegionSplit = new JsonArray();
        for(int i = e * 100000; i < min((e +1) * 100000, orderRegion.size()); i ++){
            orderRegionSplit.add(orderRegion.getJsonObject(i));
        }
        JsonObject data = orderRegionService.generateExport(orderRegionSplit);
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"));
        JsonObject body = new JsonObject()
                .put(Field.NAME, "CRRE_Export_" + day + "_" + e + ".csv")
                .put("format", "csv");
        if (ordersSize < 1000) {
            log.info("[Crre@ExportWorker::processOrderRegion] process DONE");
            exportHandler.handle(new Either.Right<>(data));
        } else {
            final Buffer buff = Buffer.buffer();
            buff.appendString(data.getString("csvFile"));
            storageService.add(body, buff, null, addFileEvent -> {
                if (addFileEvent.isRight()) {
                    JsonObject storageEntries = addFileEvent.right().getValue();
                    String application = config.getString("app-name");
                    UserUtils.getUserInfos(eb, idUser, user -> {
                        workspaceHelper.addDocument(storageEntries, user, body.getString(Field.NAME), application, false, null, createEvent -> {
                            if (createEvent.succeeded()) {
                                if ((e + 1) * 100000 < orderRegion.size()) {
                                    writeCSVFile(exportHandler, idUser, orderRegion, 100000, e + 1);
                                } else {
                                    log.info("[Crre@ExportWorker::processOrderRegion] process DONE");
                                    exportHandler.handle(new Either.Right<>(new JsonObject()));
                                }
                            } else {
                                log.error("[Crre@processOrderRegion] Failed to create a workspace document : " +
                                        createEvent.cause().getMessage());
                            }
                        });
                    });
                } else {
                    log.error("[Crre@createFile] Failed to create a new entry in the storage");
                }
            });
        }
    }

    private void getOrderRecursively(boolean old, int e, List<Integer> listOrders, List<Future> futures) {
        Future<JsonArray> orderRegionFuture = Future.future();
        futures.add(orderRegionFuture);
        List<Integer> subList = listOrders.subList(e * 5000, min((e +1) * 5000, listOrders.size()));
        orderRegionService.getOrdersRegionById(subList, old, handlerJsonArray(orderRegionFuture));
        if ((e + 1) * 5000 < listOrders.size()) {
            getOrderRecursively(old, e + 1, listOrders, futures);
        }
    }
}