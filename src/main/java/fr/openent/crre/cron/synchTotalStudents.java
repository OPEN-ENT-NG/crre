package fr.openent.crre.cron;

        import fr.openent.crre.controllers.CrreController;
        import io.vertx.core.Handler;
        import io.vertx.core.Vertx;
        import org.entcore.common.controller.ControllerHelper;

public class synchTotalStudents extends ControllerHelper implements Handler<Long> {

    CrreController crreController;

    public synchTotalStudents(Vertx vertx, CrreController crreController) {
        this.crreController = crreController;
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.debug("CRRE cron started");
        crreController.getStudents(event1 -> {
            if(event1.isRight())
                log.debug("Cron launch successful");
            else
                log.debug("Cron synchonisation not full");
        });
    }
}

