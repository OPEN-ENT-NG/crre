package fr.openent.crre.cron;

import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.DefaultNotificationService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

public class NotifyNewOrdersAdmin extends ControllerHelper implements Handler<Long> {

    private final DefaultNotificationService notificationService;


    public NotifyNewOrdersAdmin(ServiceFactory serviceFactory) {
        this.notificationService = serviceFactory.getNotificationService();
        this.vertx = serviceFactory.getVertx();
    }

    @Override
    public void handle(Long event) {
        log.info(String.format("[CRRE@%s::handle] Notify new orders for admin cron started", this.getClass().getSimpleName()));
        this.notificationService.sendNotificationAdmin();
    }
}

