package fr.openent.crre.security;

import fr.openent.crre.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class UpdateStatusRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {
        if (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())) {
            handler.handle(true);
        } else {
            String status = resourceRequest.params().get(Field.STATUS);
            if (status != null) {
                handler.handle((WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString()) && status.equals(Field.rejected)) ||
                        (WorkflowActionUtils.hasRight(user, WorkflowActions.PRESCRIPTOR_RIGHT.toString()) && status.equals(Field.resubmit)));
            } else {
                handler.handle(false);
            }
        }
    }
}

