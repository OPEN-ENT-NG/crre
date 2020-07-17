package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface UserInfoService {
  void getUserInfo(String idUser,Handler<Either<String, JsonArray>> handler);
}
