package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface OldBasketService {

    /**
     * List  basket list of a campaign and a structure
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
    void listBasket(Integer idCampaign, String idStructure, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * Get a basket thanks to its id
     * @param idBasketOrder campaign identifier
     * @param handler function handler returning data
     */
    void getBasketOrder(Integer idBasketOrder, Handler<Either<String, JsonArray>> handler);

    /**
     * Get all baskets orders of a campaign
     * @param idCampaign campaign identifier
     * @param handler function handler returning data
     */
    void getBasketsOrders(Integer idCampaign, Handler<Either<String, JsonArray>> handler, UserInfos user);

    /**
     * Get all my baskets orders
     * @param startDate
     * @param endDate
     * @param handler function handler returning data
     */
    void getMyBasketOrders(UserInfos user, Integer page, Integer id_campaign, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    void search(String query, JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchWithAll(String query, JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                       Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filter(JsonArray filters, UserInfos user, int id_campaign, String startDate, String endDate, Integer page,
                Handler<Either<String, JsonArray>> arrayResponseHandler);



}
