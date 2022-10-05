package fr.openent.crre.cron;

import fr.openent.crre.controllers.OrderRegionController;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class updateStatus extends ControllerHelper implements Handler<Long> {

    private final DefaultOrderRegionService orderRegionService;
    private final OrderRegionController orderRegionController;


    public updateStatus(Vertx vertx) {
        this.orderRegionController = new OrderRegionController(vertx, null, null);
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.info("[CRRE] : Update status cron started");
        try {
            updateStatusByLDE(event1 -> {
                if (event1.isRight())
                    log.info("Cron update status successful");
                else
                    log.info("Cron update status failed");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateStatusByLDE(final Handler<Either<String, JsonObject>> eitherHandler) throws IOException {
        Scanner sc = this.orderRegionController.getOrderLDE();
        int part = 0;
        updateCommand(sc, part, eitherHandler);
    }

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
                order.put(Field.STATUS, values[21]);
                order.put("id", values[22]);
                if (!values[22].equals("0")) {
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

