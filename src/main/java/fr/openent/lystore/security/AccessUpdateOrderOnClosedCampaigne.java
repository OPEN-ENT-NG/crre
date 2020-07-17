package fr.openent.lystore.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import fr.openent.lystore.Lystore;


public class AccessUpdateOrderOnClosedCampaigne implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {
        if(WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString())
                || WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())){
            handler.handle(true);
        }else{
            resourceRequest.pause();
            Integer campaignId = Integer.parseInt(resourceRequest.getParam("idCampaign"));
            String checkQuery = "SELECT accessible " +
                    "FROM  "+ Lystore.lystoreSchema + ".campaign " +
                    "WHERE id = ? ;";

            Sql.getInstance().prepared(checkQuery, new JsonArray().add(campaignId), SqlResult.validResultHandler(event -> {
                if (event.isRight()) {
                    JsonArray result = event.right().getValue();
                    boolean campaignIsAccessible = result.getJsonObject(0).getBoolean("accessible");
                    resourceRequest.resume();
                    handler.handle( campaignIsAccessible && WorkflowActionUtils.hasRight(user, WorkflowActions.ACCESS_RIGHT.toString()));
                } else {
                    resourceRequest.resume();
                    handler.handle(false);
                }
            }));
        }
    }
}
