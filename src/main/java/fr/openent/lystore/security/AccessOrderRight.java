package fr.openent.lystore.security;

import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by agnes.lapeyronnie on 27/02/2018.
 */
public class AccessOrderRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {

        if (WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString())) {
            handler.handle(true);
        } else {
            String idStructure = request.params().get("idStructure");
            handler.handle(user.getStructures().contains(idStructure));
        }
    }
}
