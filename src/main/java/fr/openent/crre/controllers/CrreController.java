package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import static fr.openent.crre.Crre.ACCESS_RIGHT;


public class CrreController extends ControllerHelper {
    private final static String STAR = "**********";

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
        JsonObject safeConfig = config.copy();

        JsonObject elasticsearchConfig = safeConfig.getJsonObject(Field.ELASTICSEARCHCONFIG, null);
        if (elasticsearchConfig != null) {
            if (elasticsearchConfig.getString(Field.USERNAME, null) != null) elasticsearchConfig.put(Field.USERNAME, STAR);
            if (elasticsearchConfig.getString(Field.PASSWORD, null) != null) elasticsearchConfig.put(Field.PASSWORD, STAR);
        }

        JsonObject emailConfig = safeConfig.getJsonObject(Field.EMAILCONFIG, null);
        if (emailConfig != null) {
            if (emailConfig.getString(Field.USERNAME, null) != null) emailConfig.put(Field.USERNAME, STAR);
            if (emailConfig.getString(Field.PASSWORD, null) != null) emailConfig.put(Field.PASSWORD, STAR);
            if (emailConfig.getString(Field.API_DASH_KEY, null) != null) emailConfig.put(Field.API_DASH_KEY, STAR);

        }

        renderJson(request, safeConfig);    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction(ACCESS_RIGHT)
    public void view(HttpServerRequest request) {
        renderView(request);
        eventStore.createAndStoreEvent(CrreEvent.ACCESS.name(), request);
    }
}
