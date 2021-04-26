package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

public interface OldBasketService {

    void search(String query, JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filter(JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);

}
