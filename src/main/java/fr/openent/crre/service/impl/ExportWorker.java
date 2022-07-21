package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.StorageService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
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
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;


public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Storage storage;
    public static final String ORDER_REGION = "saveOrderRegion";
    private OrderRegionService orderRegionService;
    private StructureService structureService;
    private StorageService storageService;
    private WorkspaceHelper workspaceHelper;
    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);
    private JsonObject paramsEB = new JsonObject();

    @Override
    public void start() {
        super.start();
        String neo4jConfig = (String) vertx.sharedData().getLocalMap("server").get("neo4jConfig");
        Neo4j.getInstance().init(vertx, new JsonObject(neo4jConfig));
        this.storage = new StorageFactory(vertx).getStorage();
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
        this.storageService = new DefaultStorageService(storage);
        this.workspaceHelper = new WorkspaceHelper(eb, storage);
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        message.reply(new JsonObject().put("status", "ok"));
        log.info(String.format("[Crre@%s] ExportWorker called ", this.getClass().getSimpleName()));
        this.paramsEB = message.body();
        processExport(this.paramsEB);
    }


    private void processExport(JsonObject params) {
        Handler<Either<String, Boolean>> exportHandler = event -> {
            log.info(String.format("[Crre@%s] exportHandler", this.getClass().getSimpleName()));
            if (event.isRight()) {
                log.info("[Crre@ExportWorker] Export finish");
            } else {
                log.error(event.left().getValue());
            }
        };
        chooseExport(params, exportHandler);
    }


    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
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


    private void processOrderRegion(JsonObject params, Handler<Either<String, Boolean>> exportHandler) {
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
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> orderRegionFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();

        CompositeFuture.all(structureFuture, orderRegionFuture, equipmentsFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray orderRegion = orderRegionFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                JsonArray ordersClient = new JsonArray(), ordersRegion = new JsonArray();
                orderRegionService.beautifyOrders(structures, orderRegion, equipments, ordersClient, ordersRegion);
                String csvFile = orderRegionService.generateExport(orderRegion);
                String day = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"));
                JsonObject body = new JsonObject()
                        .put("name", "CRRE_Export_" + day + ".csv")
                        .put("format", "csv");
                final Buffer buff = Buffer.buffer();
                buff.appendString(csvFile);
                storageService.add(body, buff, null, addFileEvent -> {
                    if (addFileEvent.isRight()) {
                        JsonObject storageEntries = addFileEvent.right().getValue();
                        String application = config.getString("app-name");
                        UserUtils.getUserInfos(eb, idUser, user -> {
                            workspaceHelper.addDocument(storageEntries, user, body.getString("name"), application, false, null, createEvent -> {
                                if (createEvent.succeeded()) {
                                    log.info("[Crre@ExportWorker::processOrderRegion] process DONE");
                                    exportHandler.handle(new Either.Right<>(true));
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
            } else {
                log.error("ERROR [Crre@ExportWorker::processOrderRegion] : " + event.cause().getMessage(), event.cause());
                exportHandler.handle(new Either.Left<>("ERROR [Crre@ExportWorker::processOrderRegion] : " + event.cause().getMessage()));
            }


        });

        orderRegionService.getOrdersRegionById(listOrders, old, handlerJsonArray(orderRegionFuture));
        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
        searchByIds(idsEquipments.getList(), handlerJsonArray(equipmentsFuture));
    }
}