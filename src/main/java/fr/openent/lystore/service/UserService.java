package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface UserService {

    /**
     * Return all user structures based on user id
     * @param userId User id
     * @param handler Function handler returning data
     */
    void getStructures(String userId, Handler<Either<String, JsonArray>> handler);
}
