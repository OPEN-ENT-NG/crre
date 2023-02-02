package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.security.AccessRight;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.UserService;
import fr.openent.crre.service.impl.DefaultUserService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

public class UserController extends ControllerHelper {

    private final UserService userService;

    public UserController(ServiceFactory serviceFactory) {
        super();
        this.userService = serviceFactory.getUserService();
    }

    @Get("/user/structures")
    @ApiDoc("Retrieve all user structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void getStructures(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user ->
                userService.getStructures(user.getUserId(), structuresResult -> {
                    if(structuresResult.isRight()) {
                        JsonArray structures = structuresResult.right().getValue();
                        renderJson(request, structures);
                    } else {
                        JsonObject error = (new JsonObject()).put(Field.ERROR, String.format("[Crre@%s::getStructures] Unable to retrieve structures infos : %s", this.getClass().getSimpleName(), structuresResult.left().getValue()));
                        Renders.renderJson(request, error, 400);
                    }
                }));
    }
}
