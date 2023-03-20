package fr.openent.crre.security;

import fr.openent.crre.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.stream.Collectors;

public class ValidatorAndStructureHistoricRight implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {
        if (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())) {
            handler.handle(true);
        } else {
            RequestUtils.bodyToJson(resourceRequest, body -> {
                String idStructure = body.getJsonArray(Field.IDS_STRUCTURE).stream().filter(String.class::isInstance).map(String.class::cast).collect(Collectors.toList()).get(0);
                handler.handle(WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString()) &&
                        idStructure != null && !idStructure.equals("null"));
            });
        }
    }
}

