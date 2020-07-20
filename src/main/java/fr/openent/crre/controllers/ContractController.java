package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.ManagerRight;
import fr.openent.crre.service.ContractService;
import fr.openent.crre.service.impl.DefaultContractService;
import fr.openent.crre.utils.SqlQueryUtils;
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
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class ContractController extends ControllerHelper {

    private ContractService contractService;

    public ContractController () {
        super();
        this.contractService = new DefaultContractService(Crre.crreSchema, "contract");
    }

    @Get("/contracts")
    @ApiDoc("Display all contracts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getContracts (HttpServerRequest request) {
        contractService.getContracts(arrayResponseHandler(request));
    }

    @Post("/contract")
    @ApiDoc("Create a contract")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void createContract (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "contract",
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject contract) {
                        contractService.createContract(contract,
                                Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.CONTRACT.toString(),
                                        Actions.CREATE.toString(),
                                        null,
                                        contract));
                    }
                });
    }

    @Put("/contract/:id")
    @ApiDoc("Update a contract")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateContract (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "contract",
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject contract) {
                        try {
                            contractService.updateContract(contract,
                                    Integer.parseInt(request.params().get("id")),
                                    Logging.defaultResponseHandler(eb,
                                            request,
                                            Contexts.CONTRACT.toString(),
                                            Actions.UPDATE.toString(),
                                            request.params().get("id"),
                                            contract));
                        } catch (ClassCastException e) {
                            log.error("An error occurred when casting contract id", e);
                            badRequest(request);
                        }
                    }
                });
    }

    @Delete("/contract")
    @ApiDoc("Delete one or more contracts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteContracts (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                contractService.deleteContract(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.CONTRACT.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting contract id", e);
            badRequest(request);
        }
    }
}
