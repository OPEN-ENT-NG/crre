package fr.openent.crre.cron;

import fr.openent.crre.Crre;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

public class synchTotalStudents extends ControllerHelper implements Handler<Long> {

    private final DefaultStructureService structureService;


    public synchTotalStudents(Vertx vertx) {
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.info("[CRRE] cron synchTotalStudents started");
        getStudents(event1 -> {
            if (event1.isRight())
                log.info("[CRRE] Cron synchTotalStudents finish with success");
            else
                log.info("[CRRE] Cron synchTotalStudents not full");
        });
    }

    public void getStudents(final Handler<Either<String, JsonObject>> eitherHandler) {
        structureService.getAllStructure(structures -> {
            if(structures.isRight()) {
                log.info("[CRRE][getStudents] getStudents@getAllStructures OK");
                JsonArray structure_id = structures.right().getValue();
                JsonArray ids = new JsonArray();
                for (int i = 0; i < structure_id.size(); i++) {
                    ids.add(structure_id.getJsonObject(i).getString("id_structure"));
                }
                structureService.insertStudentsInfos(ids, event -> {
                    if (event.isRight()) {
                        eitherHandler.handle(new Either.Right<>(event.right().getValue()));
                        log.info("[CRRE] getStudents@insertStudentsInfos Insert students total success");
                    } else {
                        log.error("[CRRE] getStudents@insertStudentsInfos Failed to insert students : ", event.left());
                        eitherHandler.handle(new Either.Left<>("[CRRE] getStudents@insertStudentsInfos Failed to insert students : " + event.left()));
                    }
                });
            } else {
                log.error("[CRRE] getStudents@insertStudentsInfos Failed to get all structures",structures.left());
                eitherHandler.handle(new Either.Left<>("[CRRE] getStudents@insertStudentsInfos Failed to get all structures"));
            }
        });
    }
}

