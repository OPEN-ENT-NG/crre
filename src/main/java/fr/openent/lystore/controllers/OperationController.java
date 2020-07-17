package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OperationService;
import fr.openent.lystore.service.impl.DefaultOperationService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;


import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class OperationController  extends ControllerHelper {
    private OperationService operationService;

    public OperationController () {
        super();
        this.operationService = new DefaultOperationService(Lystore.lystoreSchema, "operation");
    }

    @Get("/labels")
    @ApiDoc("Returns all labels in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLabels (HttpServerRequest request) {
        operationService.getLabels(arrayResponseHandler(request));
    }

    @Get("/operations/")
    @ApiDoc("List all operations in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOperations(HttpServerRequest request) {
        operationService.getOperations(request.params().getAll("q"), arrayResponseHandler(request));
    }

    @Post("/operation")
    @ApiDoc("Create an operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "operation", operation -> operationService.create(operation, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.CREATE.toString(),
                null,
                operation)));
    }

    @Put("/operation/:idOperation")
    @ApiDoc("Uptdate an operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJson(request, pathPrefix + "operation", operation -> operationService.updateOperation(idOperation, operation, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.UPDATE.toString(),
                idOperation.toString(),
                operation)));
    }

    @Put("/operations/instructionAttribute/:idInstruction")
    @ApiDoc("Uptdate an operation for to give id instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void addInstructionId(final HttpServerRequest request) {
        final Integer idInstruction = Integer.parseInt(request.params().get("idInstruction"));
        RequestUtils.bodyToJsonArray(request, operationIds -> operationService.addInstructionId(idInstruction, operationIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.UPDATE.toString(),
                idInstruction.toString(),
                new JsonObject().put("ids", operationIds))));
    }

    @Put("/operations/instructionRemove")
    @ApiDoc("Uptdate an operation for to give id instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void removeInstructionId(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, operationIds -> operationService.removeInstructionId( operationIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.UPDATE.toString(),
                operationIds.toString(),
                new JsonObject().put("ids", operationIds))));
    }

    @Delete("/operations")
    @ApiDoc("Delete operations")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOperaton(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, operationIds -> operationService.deleteOperation(operationIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.DELETE.toString(),
                operationIds.toString(),
                new JsonObject().put("ids", operationIds))));
    }


    @Get("/operations/:id/orders")
    @ApiDoc("Retrieve orders based on given operation identifier")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOperationOrders(HttpServerRequest request) {
        Integer operationId = Integer.parseInt(request.getParam("id"));
        operationService.getOperationOrders(operationId, arrayResponseHandler(request));
    }

    @Put("/operation/delete/orders")
    @ApiDoc("delete link between orders and operation")
    @SecuredAction(value = "",type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOrdersFromOperation(HttpServerRequest request){
       RequestUtils.bodyToJsonArray(request, operationIds -> operationService.deleteOrdersOperation(operationIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.DELETE.toString(),
                operationIds.toString(),
                new JsonObject().put("ids", operationIds))));
    }
}
