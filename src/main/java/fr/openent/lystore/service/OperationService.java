package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface OperationService {

    void getLabels (Handler<Either<String, JsonArray>> handler);

    void getOperations(List<String> filters, Handler<Either<String, JsonArray>> handler);

    void create(JsonObject operation,  Handler<Either<String, JsonObject>> handler);

    void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler);

    void addInstructionId(Integer instructionId, JsonArray operationIds, Handler<Either<String, JsonObject>> handler);

    void removeInstructionId( JsonArray operationIds, Handler<Either<String, JsonObject>> handler);

    void deleteOperation(JsonArray operationIds,  Handler<Either<String, JsonObject>> handler);

    void getOperationOrders(Integer operationId, Handler<Either<String, JsonArray>> handler);

    void deleteOrdersOperation(JsonArray ordersIds, Handler<Either<String, JsonObject>> handler);
}