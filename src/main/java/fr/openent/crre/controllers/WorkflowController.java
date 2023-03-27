package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.UserHelper;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.WorkflowService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.util.List;
import java.util.stream.Collectors;

public class WorkflowController extends ControllerHelper {
    private final WorkflowService workflowService;

    public WorkflowController(ServiceFactory serviceFactory) {
        this.workflowService = serviceFactory.getWorkflowService();
    }

    @Post("/user/workflow")
    @ApiDoc("List all workflow for a user from a structure scope")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listUserWorkflow(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.LISTUSERWORKFLOW, body -> {
            List<String> idStructureList = body.getJsonArray(Field.ID_STRUCTURE_LIST, new JsonArray()).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());

            if (idStructureList.isEmpty()) {
                Renders.badRequest(request, "\"id_structure_list\" is not valid");
                return;
            }

            UserHelper.getUserInfos(eb, request)
                    .compose(userInfos -> this.workflowService.getWorkflowListFromStructureScope(userInfos.getUserId(), idStructureList))
                    .onSuccess(workflowList -> Renders.renderJson(request, JsonObject.mapFrom(workflowList)))
                    .onFailure(error -> Renders.renderError(request));

        });

    }
}
