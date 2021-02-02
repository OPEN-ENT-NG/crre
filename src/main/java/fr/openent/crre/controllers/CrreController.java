package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import fr.openent.crre.service.impl.DefaultStructureService;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;


public class CrreController extends ControllerHelper {
    private final DefaultStructureService structureService;

    private final EventStore eventStore;

    private enum CrreEvent {ACCESS}

    public CrreController() {
        super();
        this.structureService = new DefaultStructureService( Crre.crreSchema);
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Crre.class.getSimpleName());
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction("crre.access")
    public void view(HttpServerRequest request) {
        renderView(request);
        eventStore.createAndStoreEvent(CrreEvent.ACCESS.name(), request);
    }

    public void getStudents(final Handler<Either<String, JsonObject>> eitherHandler) {
        structureService.getAllStructure(structures -> {
            if(structures.isRight()) {
                JsonArray structure_id = structures.right().getValue();
                JsonArray ids = new JsonArray();
                for (int i = 0; i < structure_id.size(); i++) {
                    ids.add(structure_id.getJsonObject(0).getString("id_structure"));
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
                                            log.error("Failed to insert");
                                            eitherHandler.handle(new Either.Left<>("Failed to insert"));
                                        }
                                    });
                                });
                            }
                        });
                    } else {
                        log.error("Failed to get students or insert into structure");
                        eitherHandler.handle(new Either.Left<>("Failed to get students or insert into structure"));
                    }
                });
                structureService.insertStructures(ids, handlerJsonArray(insertStructuresFuture));
                structureService.getStudentsByStructure(ids, handlerJsonArray(getStudentsByStructureFuture));
            } else {
                log.error("Failed to get all structures");
                eitherHandler.handle(new Either.Left<>("Failed to get all structures"));
            }
        });
    }

}
