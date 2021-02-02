package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.PersonnelRight;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;


/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class StructureController extends ControllerHelper {

    private final DefaultStructureService structureService;

    public StructureController(){
        super();
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Get("/structures")
    @ApiDoc("Returns all structures")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructures(HttpServerRequest request){
        structureService.getStructures(arrayResponseHandler(request));
    }



    @Put("/structure/amount/update")
    @ApiDoc("Update a basket's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    public void updateAmount(final HttpServerRequest  request){
            try {
                Integer seconde = Integer.parseInt(request.params().get("seconde"));
                Integer premiere = Integer.parseInt(request.params().get("premiere"));
                Integer terminale = Integer.parseInt(request.params().get("terminale"));
                String id_structure = request.params().get("id_structure");
                boolean pro = Boolean.getBoolean(request.params().get("pro"));
                Integer total_licence = 0;

                if(pro) {
                    total_licence = seconde * 3 + premiere * 3 + terminale * 3;
                } else {
                    total_licence = seconde * 9 + premiere * 8 + terminale * 7;
                }

                Future<JsonObject> updateAmountFuture = Future.future();
                Future<JsonObject> updateAmountLicenceFuture = Future.future();

                CompositeFuture.all(updateAmountFuture, updateAmountLicenceFuture).setHandler(event -> {
                    if(event.succeeded()) {
                        log.info("Update amount licence success");
                        request.response().setStatusCode(201).end();
                    } else {
                        log.error("Update licences amount failed");
                    }
                        });
                structureService.updateAmount(id_structure, seconde, premiere, terminale, handlerJsonObject(updateAmountFuture));
                structureService.updateAmountLicence(id_structure, total_licence, handlerJsonObject(updateAmountLicenceFuture));
            } catch (ClassCastException e) {
                log.error("An error occurred when updating licences amount", e);
            }
    }

    @Get("/structure/amount")
    @ApiDoc("Get all students amount by structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    public void getAmount(final HttpServerRequest  request) {
            try {
                String id_structure = request.params().get("id_structure");
                structureService.getAmount(id_structure, defaultResponseHandler(request));
            } catch (ClassCastException e) {
                log.error("An error occurred when casting basket id", e);
            }
    }
}
