package fr.openent.crre.helpers;

import fr.openent.crre.service.ServiceFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.entcore.common.user.UserInfos;

import java.util.Collections;

public class WorkflowHelper {

    private WorkflowHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Future<Boolean> authorize(String idStructure, UserInfos userInfos, String workflow) {
        if (idStructure == null) {
            return Future.failedFuture("idStructure is null");
        }

        Promise<Boolean> promise = Promise.promise();

        ServiceFactory.getInstance().getWorkflowService().getWorkflowListFromStructureScope(userInfos.getUserId(), Collections.singletonList(idStructure))
                .onSuccess(workflowList -> promise.complete(workflowList.get(idStructure).stream().anyMatch(workflowNeo4jModel -> workflowNeo4jModel.getDisplayName().equals(workflow))))
                .onFailure(promise::fail);

        return promise.future();
    }
}
