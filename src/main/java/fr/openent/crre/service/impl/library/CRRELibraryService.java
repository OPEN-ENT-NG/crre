package fr.openent.crre.service.impl.library;

import fr.openent.crre.helpers.FileHelper;
import fr.openent.crre.helpers.HttpRequestHelper;
import fr.openent.crre.model.CRRELibraryElementModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.library.CRREParam;
import fr.openent.crre.service.ILibraryService;
import fr.openent.crre.service.ServiceFactory;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.ext.web.client.HttpRequest;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CRRELibraryService implements ILibraryService<CRREParam> {
    private static final Logger log = LoggerFactory.getLogger(CRRELibraryService.class);

    @Override
    public Future<Void> getStatus(CRREParam params) {
        Promise<Void> promise = Promise.promise();

        List<CRRELibraryElementModel> listOrder = new ArrayList<>();
        List<Future> listFutureUpdate = new ArrayList<>();

        this.getOrderLDE(params, groupOrderLDEModelHandler(listOrder, listFutureUpdate))
                .onSuccess(event -> {
                    listFutureUpdate.add(this.updateOldOrderLDEModel(listOrder)); //save the last order group
                    CompositeFuture.join(listFutureUpdate).onComplete(res -> {
                        if (res.failed()) {
                            log.error("[CRRE] Some update failed.");
                        }
                        promise.complete();
                    });
                })
                .onFailure(error -> {
                    String message = String.format("[CRRE@%s::updateStatusByLDE] Failed to retrieve Order LDE %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    @Override
    public void sendOrder(List<OrderUniversalModel> orderList, CRREParam params) {
        //todo CRRE-576
    }

    @Override
    public CRREParam generateModel(JsonObject jsonObject) {
        return new CRREParam(jsonObject);
    }

    /**
     * We will store the orders one by one to avoid making a save in the DB for each data.
     * Every 1000 data we will launch updateOldOrderLDEModel.
     * We do not do more than 1000 to avoid saturating the memory of the JVM.
     *
     * @param listOrder list to store orders
     * @param futureList List of future. Each 100 add future updateOldOrderLDEModel
     * @return handler witch must be executed for each order data
     */
    private Handler<CRRELibraryElementModel> groupOrderLDEModelHandler(List<CRRELibraryElementModel> listOrder, List<Future> futureList) {
        return orderLDEModel -> {
            listOrder.stream()
                    .filter(order -> order.getCGIId().equals(orderLDEModel.getCGIId()))
                    .findAny().ifPresent(listOrder::remove);
            listOrder.add(orderLDEModel);

            if (listOrder.size() >= 1000) {
                futureList.add(this.updateOldOrderLDEModel(listOrder));
                listOrder.clear();
            }
        };
    }

    /**
     * Get order LDE from LDE API. Due to a large amount of data, the response must be processed little by little.
     * This is why we provide a handler which will be executed for each order.
     *
     * @param orderLDEModelHandler handler executed for each order of the HTTP response
     * @return future completed when HTTP response has finish to be read
     */
    private Future<Void> getOrderLDE(CRREParam params, Handler<CRRELibraryElementModel> orderLDEModelHandler) {
        Promise<Void> promise = Promise.promise();

        HttpRequest<Buffer> request = ServiceFactory.getInstance().getWebClient().getAbs(params.getApiUrl());
        FileSystem fs = ServiceFactory.getInstance().getVertx().fileSystem();

        //Create tmpFile
        FileHelper.createTempFile(fs)
                //Get tmpFile
                .compose(path -> FileHelper.getFile(fs, path))
                //Write in tmpFile in Stream
                .compose(tmpFile -> HttpRequestHelper.getHttpRequestResponseAsStream(request, tmpFile, false))
                //Read tmpFile in Stream
                .onSuccess(tmpFile -> {
                    //Use atomic to skip header csv line
                    AtomicBoolean headerIsSkip = new AtomicBoolean(false);
                    RecordParser recordParser = RecordParser.newDelimited("\r", bufferedLine -> {
                        if (!headerIsSkip.get()) {
                            headerIsSkip.set(true);
                            return;
                        }
                        orderLDEModelHandler.handle(new CRRELibraryElementModel(bufferedLine.toString()));
                    }).exceptionHandler(error -> {
                        String message = String.format("[CRRE@%s::getOrderLDE] Failed to execute handler: %s",
                                this.getClass().getSimpleName(), error.getMessage());
                        log.error(message);
                    });

                    tmpFile.handler(recordParser)
                            .exceptionHandler(error -> {
                                String message = String.format("[CRRE@%s::getOrderLDE] Failed to stream order LDE response: %s",
                                        this.getClass().getSimpleName(), error.getMessage());
                                log.error(message);
                                promise.fail(error.getMessage());
                            })
                            .endHandler(v -> {
                                tmpFile.close();
                                promise.complete();
                            });
                })
                .onFailure(error -> {
                    String message = String.format("[CRRE@%s::getOrderLDE] Failed to get LDE order: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    promise.fail(error.getMessage());
                });
        return promise.future();
    }

    private Future<JsonObject> updateOldOrderLDEModel(List<CRRELibraryElementModel> listOrder) {
        Promise<JsonObject> promise = Promise.promise();

        List<CRRELibraryElementModel> ordersRegion = listOrder.stream()
                .filter(orderLDEModel -> !orderLDEModel.getEtat().isEmpty() && NumberUtils.isParsable(orderLDEModel.getEtat()) &&
                        !orderLDEModel.getCGIId().isEmpty() && NumberUtils.isParsable(orderLDEModel.getCGIId()) &&
                        !orderLDEModel.getCGIId().equals("0"))
                .collect(Collectors.toList());
        if (ordersRegion.isEmpty()) {
            return Future.succeededFuture();
        }

        ServiceFactory.getInstance().getOrderRegionService().updateOldOrdersWithTransaction(ordersRegion)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::updateOldOrderLDEModel] Failed to update order: %s",
                            this.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }
}
