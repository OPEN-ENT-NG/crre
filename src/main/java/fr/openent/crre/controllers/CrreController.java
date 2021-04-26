package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;


public class CrreController extends ControllerHelper {

    private final EventStore eventStore;

    private enum CrreEvent {ACCESS}

    public CrreController() {
        super();
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Crre.class.getSimpleName());
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction("crre.access")
    public void view(HttpServerRequest request) {
        renderView(request);
        eventStore.createAndStoreEvent(CrreEvent.ACCESS.name(), request);
    }
}
