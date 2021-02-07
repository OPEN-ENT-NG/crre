package fr.openent.crre.cron;

import fr.openent.crre.controllers.CrreController;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.entcore.common.controller.ControllerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.crre.Crre.CONFIG;

public class synchTotalStudents extends ControllerHelper implements Handler<Long> {

    CrreController crreController;

    public synchTotalStudents(Vertx vertx, CrreController crreController) {
        this.crreController = crreController;
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.info("CRRE cron started");
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        if(CONFIG.getString("dateSynch").equals(formatter.format(today))) {
            crreController.getStudents(event1 -> {
                if (event1.isRight())
                    log.info("Cron launch successful");
                else
                    log.info("Cron synchonisation not full");
            });
        }
    }
}

