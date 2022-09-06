package fr.openent.crre.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class ValidatorAndStructureRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {
        if (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())) {
            handler.handle(true);
        } else {
            String idStructure = resourceRequest.getParam("idStructure");
            handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString()) &&
                    idStructure != null && !idStructure.equals("null"));
        }
    }
}

