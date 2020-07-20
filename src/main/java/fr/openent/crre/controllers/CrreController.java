package fr.openent.crre.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;


public class CrreController extends ControllerHelper {

    public CrreController() {
        super();
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction("crre.access")
    public void view(HttpServerRequest request) {
        renderView(request);
    }

}
