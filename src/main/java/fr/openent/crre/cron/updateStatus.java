package fr.openent.crre.cron;

import fr.openent.crre.controllers.OrderRegionController;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.ColumnsLDEOrders;
import fr.openent.crre.model.OrderLDEModel;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.math.NumberUtils;
import org.entcore.common.controller.ControllerHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class updateStatus extends ControllerHelper implements Handler<Long> {

    private final DefaultOrderRegionService orderRegionService;
    private final OrderRegionController orderRegionController;

    /**
     * @deprecated  Replaced by {@link #updateStatus(ServiceFactory)}
     */
    @Deprecated
    public updateStatus(Vertx vertx) {
        this.orderRegionController = new OrderRegionController(vertx, null, null);
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.vertx = vertx;
    }

    public updateStatus(ServiceFactory serviceFactory) {
        this.orderRegionController = new OrderRegionController(serviceFactory);
        this.orderRegionService = serviceFactory.getOrderRegionService();
    }

    @Override
    public void handle(Long event) {
        log.info("[CRRE] : Update status cron started");
        updateStatusByLDE()
                .onSuccess(res -> {
                    log.info("[CRRE] : Update LDE status finished with success");
                })
                .onFailure(error -> log.error(error.getMessage()));
    }

    private Future<Void> updateStatusByLDE() {
        Promise<Void> promise = Promise.promise();

        List<OrderLDEModel> listOrder = new ArrayList<>();
        List<Future> listFutureUpdate = new ArrayList<>();

        this.orderRegionController.getOrderLDE(groupOrderLDEModelHandler(listOrder, listFutureUpdate))
                .onSuccess(event -> {
                    listFutureUpdate.add(this.orderRegionService.updateOldOrderLDEModel(listOrder)); //save the last order group
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

    /**
     * We will store the orders one by one to avoid making a save in the DB for each data.
     * Every 1000 data we will launch updateOldOrderLDEModel.
     * We do not do more than 1000 to avoid saturating the memory of the JVM.
     *
     * @param listOrder list to store orders
     * @param futureList List of future. Each 100 add future updateOldOrderLDEModel
     * @return handler witch must be executed for each order data
     */
    private Handler<OrderLDEModel> groupOrderLDEModelHandler(List<OrderLDEModel> listOrder, List<Future> futureList) {
        return event -> {
            listOrder.add(event);

            if (listOrder.size() >= 1000) {
                futureList.add(this.orderRegionService.updateOldOrderLDEModel(listOrder));
                listOrder.clear();
            }
        };
    }

    /**
     * Block main thread and use a lot of JVM memory with {@link Scanner}.
     *
     * @deprecated  Replaced by {@link #updateStatusByLDE()}
     */
    @Deprecated
    public void updateStatusByLDE(final Handler<Either<String, JsonObject>> eitherHandler) throws IOException {
        Scanner sc = this.orderRegionController.getOrderLDE();
        int part = 0;
        updateCommand(sc, part, eitherHandler);
    }

    /**
     * Use a lot of JVM memory with {@link Scanner}.
     *
     * @deprecated No replacement
     */
    @Deprecated
    private void updateCommand(Scanner sc, int part, final Handler<Either<String, JsonObject>> eitherHandler) {
        JsonArray ordersRegion = new JsonArray();
        int total = part * 1000;
        if (!sc.hasNextLine()) {
            eitherHandler.handle(new Either.Right<>(new JsonObject().put("message", "update LDE status finished with success")));
            log.info("update LDE status finished with success");
        } else {
            log.info("Processing LDE status part " + part);
            while (sc.hasNextLine() && total < 1000 * part + 1000) {
                total++;
                String userLine = sc.nextLine();
                String[] values = userLine.split(Pattern.quote("|"));
                JsonObject order = new JsonObject();
                String etat = values[ColumnsLDEOrders.ETAT.column()];
                String id = values[ColumnsLDEOrders.ID_CGI.column()];
                if (!etat.isEmpty() && NumberUtils.isParsable(etat) &&
                        !id.isEmpty() && NumberUtils.isParsable(id) && !id.equals("0")) {
                    order.put(Field.STATUS, etat);
                    order.put(Field.ID, id);
                    ordersRegion.add(order);
                }
            }
            if (ordersRegion.size() > 0) {
                orderRegionService.updateOldOrders(ordersRegion, event1 -> {
                    if (event1.isRight()) {
                        updateCommand(sc, part + 1, eitherHandler);
                    } else {
                        log.error("Failed to update status : ", event1.left());
                        eitherHandler.handle(new Either.Left<>("Failed to insert students : " + event1.left()));
                    }
                });
            } else {
                updateCommand(sc, part + 1, eitherHandler);
            }
        }
    }
}

