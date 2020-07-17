package fr.openent.lystore.controllers;

import fr.openent.lystore.service.UserService;
import fr.openent.lystore.service.impl.DefaultUserService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class UserController extends ControllerHelper {

    private UserService userService;

    public UserController() {
        super();
        this.userService = new DefaultUserService();
    }


    @Get("/user/structures")
    @ApiDoc("Retrieve all user structures")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructures(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                userService.getStructures(user.getUserId(), arrayResponseHandler(request));
            }
        });
    }
}
