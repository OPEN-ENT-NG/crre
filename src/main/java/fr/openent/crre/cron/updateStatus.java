package fr.openent.crre.cron;

import fr.openent.crre.controllers.OrderRegionController;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import io.vertx.core.*;
import org.entcore.common.controller.ControllerHelper;

public class updateStatus extends ControllerHelper implements Handler<Long> {

    private final DefaultOrderRegionService orderRegionService;
    private final OrderRegionController orderRegionController;

    public updateStatus(ServiceFactory serviceFactory) {
        this.orderRegionController = new OrderRegionController(serviceFactory);
        this.orderRegionService = serviceFactory.getOrderRegionService();
    }

    @Override
    public void handle(Long event) {
        log.info("[CRRE] : Update status cron started");
        //todo CRRE-578
    }
}

