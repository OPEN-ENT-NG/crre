package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface OrderRegionService {
    void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void updateOrderRegion(JsonObject order,int idOrder, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void createOrdersRegion(JsonObject order, UserInfos event, Number id_project, Handler<Either<String, JsonObject>> handler);

    void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void getOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void updateOperation(Integer id, JsonArray orders, Handler<Either<String, JsonObject>> handler);

    public void linkOrderToOperation(Integer id_order_client_equipment, Integer id_operation, Handler<Either<String, JsonObject>> handler);

    void createProject (Integer idProject,  Handler<Either<String, JsonObject>> handler);

}
