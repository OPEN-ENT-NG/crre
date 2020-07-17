package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SupplierController extends ControllerHelper {

    private SupplierService supplierService;

    public SupplierController() {
        super();
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
    }

    @Get("/suppliers")
    @ApiDoc("Returns all holders in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getHolders (HttpServerRequest request) {
        supplierService.getSuppliers(arrayResponseHandler(request));
    }

    @Post("/supplier")
    @ApiDoc("Create a holder")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void createHolder (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, pathPrefix + "supplier", new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject body) {
                        supplierService.createSupplier(body,
                                Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.SUPPLIER.toString(),
                                        Actions.CREATE.toString(),
                                        null,
                                        body));
                    }
                });
            }
        });
    }

    @Put("/supplier/:id")
    @ApiDoc("Update a holder based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateHolder (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "supplier", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                try {
                    supplierService.updateSupplier(Integer.parseInt(request.params().get("id")), body,
                            Logging.defaultResponseHandler(eb,
                                    request,
                                    Contexts.SUPPLIER.toString(),
                                    Actions.UPDATE.toString(),
                                    request.params().get("id"),
                                    body));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting supplier id", e);
                    badRequest(request);
                }
            }
        });
    }

    @Delete("/supplier")
    @ApiDoc("Delete a holder based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteHolder (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                supplierService.deleteSupplier(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.SUPPLIER.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting supplier id", e);
            badRequest(request);
        }
    }
}
