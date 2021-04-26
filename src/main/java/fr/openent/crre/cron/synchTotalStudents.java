package fr.openent.crre.cron;

import fr.openent.crre.Crre;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.crre.Crre.CONFIG;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

public class synchTotalStudents extends ControllerHelper implements Handler<Long> {

    private final DefaultStructureService structureService;


    public synchTotalStudents(Vertx vertx) {
        this.structureService = new DefaultStructureService( Crre.crreSchema);
        this.vertx = vertx;
    }

    @Override
    public void handle(Long event) {
        log.info("CRRE cron started");
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        if(CONFIG.getString("dateSynch").equals(formatter.format(today))) {
            getStudents(event1 -> {
                if (event1.isRight())
                    log.info("Cron launch successful");
                else
                    log.info("Cron synchonisation not full");
            });
        }
    }

    public void getStudents(final Handler<Either<String, JsonObject>> eitherHandler) {
        structureService.getAllStructure(structures -> {
            if(structures.isRight()) {
                JsonArray structure_id = structures.right().getValue();
                JsonArray ids = new JsonArray();
                for (int i = 0; i < structure_id.size(); i++) {
                    ids.add(structure_id.getJsonObject(i).getString("id_structure"));
                }
                Future<JsonArray> getStudentsByStructureFuture = Future.future();
                Future<JsonArray> insertStructuresFuture = Future.future();

                CompositeFuture.all(getStudentsByStructureFuture, insertStructuresFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray students = getStudentsByStructureFuture.result();
                        structureService.insertStudents(students, result -> {
                            if(result.isRight()) {
                                structureService.getTotalStructure(total_structure -> {
                                    JsonArray total = total_structure.right().getValue();
                                    structureService.insertTotalStructure(total, event2 -> {
                                        if(event2.isRight()) {
                                            log.info("Insert total success");
                                            eitherHandler.handle(new Either.Right<>(event2.right().getValue()));
                                        } else {
                                            log.error("Failed to insert total of students", event2.left());
                                            eitherHandler.handle(new Either.Left<>("Failed to insert"));
                                        }
                                    });
                                });
                            } else {
                                log.error("Failed to insert students", result.left());
                                eitherHandler.handle(new Either.Left<>("Failed to get students or insert into structure"));
                            }
                        });
                    } else {
                        log.error("Failed to get students or insert into structure", event.cause());
                        eitherHandler.handle(new Either.Left<>("Failed to get students or insert into structure"));
                    }
                });
                structureService.insertStructures(ids, handlerJsonArray(insertStructuresFuture));
                structureService.getStudentsByStructure(ids, handlerJsonArray(getStudentsByStructureFuture));
            } else {
                log.error("Failed to get all structures",structures.left());
                eitherHandler.handle(new Either.Left<>("Failed to get all structures"));
            }
        });
    }
}

