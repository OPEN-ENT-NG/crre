package fr.openent.crre.cron;

import fr.openent.crre.model.config.ConfigBooksellerModel;
import fr.openent.crre.service.ServiceFactory;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Map;

public class UpdateStatusCron implements Handler<Long> {
    private final Map<String, ConfigBooksellerModel> booksellerConfig;
    private static final Logger log = LoggerFactory.getLogger(UpdateStatusCron.class);

    public UpdateStatusCron(ServiceFactory serviceFactory) {
        booksellerConfig = serviceFactory.getConfig().getBooksellerConfig();
    }

    @Override
    public void handle(Long event) {
        log.info(String.format("[CRRE@%s::handle] Update status cron started", this.getClass().getSimpleName()));

        booksellerConfig.values().forEach(booksellerModel -> booksellerModel.getType().getService().updateStatus(booksellerModel.getParam()));
    }
}

