package fr.openent.crre.controllers;

import fr.openent.crre.security.AccessRight;
import fr.openent.crre.service.UserService;
import fr.openent.crre.service.impl.DefaultUserService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class UserController extends ControllerHelper {

    private final UserService userService;

    public UserController() {
        super();
        this.userService = new DefaultUserService();
    }


    @Get("/user/structures")
    @ApiDoc("Retrieve all user structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessRight.class)
    public void getStructures(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> userService.getStructures(user.getUserId(), arrayResponseHandler(request)));
    }
}
