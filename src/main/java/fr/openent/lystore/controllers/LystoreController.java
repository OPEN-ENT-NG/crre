package fr.openent.lystore.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;


public class LystoreController extends ControllerHelper {

    public LystoreController() {
        super();
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction("lystore.access")
    public void view(HttpServerRequest request) {
        renderView(request);
    }

}
