package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import static fr.openent.crre.Crre.ACCESS_RIGHT;


public class CrreController extends ControllerHelper {

    private final EventStore eventStore;

    private enum CrreEvent {ACCESS}

    public CrreController() {
        super();
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Crre.class.getSimpleName());
    }

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        renderJson(request, config);
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction(ACCESS_RIGHT)
    public void view(HttpServerRequest request) {
        renderView(request);
        eventStore.createAndStoreEvent(CrreEvent.ACCESS.name(), request);
    }
}
