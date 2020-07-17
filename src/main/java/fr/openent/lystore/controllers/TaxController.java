package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.TaxService;
import fr.openent.lystore.service.impl.DefaultTaxService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class TaxController extends ControllerHelper {

    private final TaxService taxService;

    public TaxController () {
        super();
        this.taxService = new DefaultTaxService(Lystore.lystoreSchema, "tax");
    }

    @Get("/taxes")
    @ApiDoc("List all taxes in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listTaxes (HttpServerRequest request) {
        taxService.list(arrayResponseHandler(request));
    }
}
