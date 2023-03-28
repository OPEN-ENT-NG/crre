package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.model.OrderRegionBeautifyModel;
import fr.openent.crre.model.OrderRegionComplex;
import fr.openent.crre.model.export.ExportOrderRegion;
import fr.openent.crre.model.export.ExportTypeEnum;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StorageService;
import fr.openent.crre.service.StructureService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.ListUtils;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;


public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
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

    private void processExport(JsonObject body,  Message<JsonObject> message) {
        final ExportTypeEnum action = ExportTypeEnum.getValue(body.getString(Field.ACTION, ""), ExportTypeEnum.NULL);
        JsonObject params = body.getJsonObject(Field.PARAMS);
        log.info(String.format("[CRRE@%s::chooseExport] Export Type : %s", this.getClass().getSimpleName(), action.name()));
        Future<JsonObject> future;
        switch (action) {
            case ORDER_REGION:
                 future = processOrderRegion(new ExportOrderRegion(params));
                break;
            default:
                log.error("[Crre@ExportWorker::catchError ] Error for create file export excel");
                future = Future.failedFuture("Error for create file export excel");
                break;
        }

        future.onSuccess(res -> message.reply(new JsonObject().put(Field.STATUS, Field.OK).put(Field.DATA, res)))
                .onFailure(error -> message.fail(500, error.getMessage()));
    }


    private Future<JsonObject> processOrderRegion(ExportOrderRegion exportOrderRegion) {
        Promise<JsonObject> promise = Promise.promise();

        log.info("[Crre@ExportWorker::processOrderRegion ] Process orders");
        Future<JsonArray> structureFuture = structureService.getStructureById(new JsonArray(new ArrayList<>(exportOrderRegion.getIdsStructures())), null);
        Future<JsonArray> equipmentsFuture = searchByIds(exportOrderRegion.getIdsEquipments(), null);


        CompositeFuture.all(structureFuture, equipmentsFuture)
                .compose(res -> this.getOrder(exportOrderRegion.getIdsOrders()))
                .compose(orderRegionComplexList -> {
                    JsonArray structures = structureFuture.result();
                    JsonArray equipments = equipmentsFuture.result();
                    List<OrderRegionBeautifyModel> orderRegionList = orderRegionService.orderResultToBeautifyModel(structures, orderRegionComplexList, equipments);
                    return writeCSVFile(exportOrderRegion.getIdUser(), orderRegionList);
                })
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    log.error(String.format("[Crre@%s::processOrderRegion] : %s", this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<JsonObject> writeCSVFile(String idUser, List<OrderRegionBeautifyModel> orderRegion) {
        Promise<JsonObject> promise = Promise.promise();

        UserUtils.getUserInfos(eb, idUser, userInfos -> {
            List<List<OrderRegionBeautifyModel>> partition = ListUtils.partition(orderRegion, 10000);
            AtomicInteger i = new AtomicInteger(0);
            Function<List<OrderRegionBeautifyModel>, Future<JsonObject>> function = orderRegionList ->
                    this.writeCSVFileForOnePartitionElement(userInfos, orderRegionList, i.getAndIncrement());

            FutureHelper.compositeSequential(function, partition, true)
                    .onSuccess(res -> promise.complete(res.get(0).result()))
                    .onFailure(error -> {
                        log.error(String.format("[CRRE@%s::writeCSVFile] Fail to write csv file %s", this.getClass().getSimpleName(), error.getMessage()));
                        promise.fail(error);
                    });
        });

        return promise.future();
    }

    private Future<JsonObject> writeCSVFileForOnePartitionElement(UserInfos user, List<OrderRegionBeautifyModel> orderRegionList, Integer index) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject data = orderRegionService.generateExport(orderRegionList);
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy"));
        JsonObject body = new JsonObject()
                .put(Field.NAME, "CRRE_Export_" + day + "_" + index + ".csv")
                .put("format", "csv");

        final Buffer buff = Buffer.buffer();
        buff.appendString(data.getString(Field.CSVFILE));
        storageService.add(body, buff, null)
                .onSuccess(storageEntries -> {
                    String application = config.getString("app-name");
                    workspaceHelper.addDocument(storageEntries, user, body.getString(Field.NAME), application, false,
                            null, event -> {
                                if (event.failed()) {
                                    log.error(String.format("[CRRE@%s::processOrderRegion] Failed to create a workspace document : ",
                                            this.getClass().getSimpleName(), event.cause().getMessage()));
                                    promise.fail(event.cause());
                                } else {
                                    promise.complete(data);
                                }
                            });
                })
                .onFailure(error -> {
                    log.error(String.format("[Crre@%s::writeCSVFileForOnePartitionElement] Failed to create a new entry in the storage",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<List<OrderRegionComplex>> getOrder(List<Integer> listOrders) {
        Promise<List<OrderRegionComplex>> promise = Promise.promise();
        List<List<Integer>> partition = ListUtils.partition(listOrders, 5000);

        Function<List<Integer>, Future<List<OrderRegionComplex>>> function = idOrderList -> this.orderRegionService.getOrdersRegionById(idOrderList, null);

        FutureHelper.compositeSequential(function, partition, true)
                .onSuccess(res ->
                        promise.complete(res.stream().flatMap(listFuture -> listFuture.result().stream()).collect(Collectors.toList())))
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::getOrder]Fail to get all order %s", this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }
}