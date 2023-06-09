package fr.openent.crre.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * This resource filter does not check that the user is a prescriber in the establishment
 *
 * @deprecated Use {@link AccessInStructureRight}
 */
@Deprecated
public class AccessRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {
        handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_RIGHT.toString()));
    }
}
