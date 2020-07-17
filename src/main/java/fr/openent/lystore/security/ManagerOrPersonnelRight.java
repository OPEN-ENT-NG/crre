package fr.openent.lystore.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by rahnir on 17/01/2018.
 */
public class ManagerOrPersonnelRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString())
                || WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_RIGHT.toString()));
    }

}
