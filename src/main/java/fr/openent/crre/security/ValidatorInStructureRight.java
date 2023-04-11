package fr.openent.crre.security;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class ValidatorInStructureRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        String idStructure = httpServerRequest.getParam(Field.IDSTRUCTURE) != null ? httpServerRequest.getParam(Field.IDSTRUCTURE) :
                httpServerRequest.params().get(Field.IDSTRUCTURE);
        WorkflowHelper.authorize(idStructure, userInfos, Crre.VALIDATOR_RIGHT)
                .onSuccess(handler)
                .onFailure(error -> handler.handle(false));
    }
}
