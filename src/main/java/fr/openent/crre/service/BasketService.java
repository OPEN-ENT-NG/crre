package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface BasketService {
    /**
     * Create a basket item
     * @param basket basket item to create
     * @param handler function handler returning data
     */
     void create(JsonObject basket, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * List  basket list of a campaign and a structure
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
    void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler, UserInfos user);

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
     * @param handler function handler returning data
     */
    void getMyBasketOrders(Handler<Either<String, JsonArray>> handler, UserInfos user);

    /**
     * Get all the baskets orders of my structures
     * @param handler function handler returning data
     */
    void getStructureHistoryBaskets(Handler<Either<String, JsonArray>> handler, UserInfos user);

    /**
     * Delete a basket item
     * @param idBasket id of the basket item
     * @param handler function handler returning data
     */
     void delete(Integer idBasket, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a basket's amount
     * @param idBasket id of a basket item
     * @param amount the new amount
     * @param handler function handler returning data
     */
     void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler );

    /**
     * Update a basket's comment
     *
     * @param idBasket
     * @param comment
     * @param handler
     */
    void updateComment(Integer idBasket, String comment, Handler<Either<String, JsonObject>> handler );

    /**
     * Update a basket's reassort
     *
     * @param idBasket
     * @param reassort
     * @param handler
     */
    void updateReassort(Integer idBasket, Boolean reassort, Handler<Either<String, JsonObject>> handler );

    /**
     * transform basket to an order
     * @param request the request
     * @param baskets list of basket's items to transform
     * @param idCampaign the id of the campaign
     * @param user the user that take an order
     * @param idStructure the id of the structure
     * @param nameStructure the name of the structure
     * @param objects
     * @param nameBasket
     * @param handler function handler returning data
     */
    void takeOrder(HttpServerRequest request, JsonArray baskets, Integer idCampaign, UserInfos user,
                   String idStructure, String nameStructure, JsonArray objects, String nameBasket, Handler<Either<String, JsonObject>> handler);
    /**
     * List  basket list of a campaign and a structure to transform to an order
     * @param idCampaign id of the campaign
     * @param idStructure id of the structure
     * @param baskets basket list to retrieve
     * @param handler function handler returning data
     */
    void listebasketItemForOrder(Integer idCampaign, String idStructure, JsonArray baskets, Handler<Either<String, JsonArray>> handler);

    /**
     * Update a price proposaf from a basket
     * @param id
     * @param price_proposal
     * @param eitherHandler
     */
    void updatePriceProposal(Integer id, Double price_proposal, Handler<Either<String, JsonObject>> eitherHandler);

    /**
     * Add file to a specific basket
     *
     * @param basketId basket identifier
     * @param fileId   file identifier
     * @param fileName file name
     * @param handler  Function handler returning data
     */
    void addFileToBasket(Integer basketId, String fileId, String fileName, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete file from basket
     *
     * @param basketId basket identifier
     * @param fileId   file identifier
     * @param handler  Function handler returning data
     */
    void deleteFileFromBasket(Integer basketId, String fileId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get file from a specific basket id
     *
     * @param basketId basket identifier
     * @param fileId   file identifier
     * @param handler  Function handler returning data
     */
    void getFile(Integer basketId, String fileId, Handler<Either<String, JsonObject>> handler);

    /**
     * Search basket from a query (name, user_name or article)
     *
     * @param query searching query (name, user_name or article)
     * @param user   user object
     * @param id_campaign  campaign identifier
     * @param arrayResponseHandler  Function handler returning data
     */

    void search(String query, JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void updateAllAmount(Integer id, Handler<Either<String, JsonObject>> handler);

    void searchName(String word, Handler<Either<String, JsonArray>> handler);

    void searchWithAll(String query, JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchWithoutEquip(String query, JsonArray filters, UserInfos user, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filter(JsonArray filters, UserInfos user, JsonArray equipTab, int id_campaign, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterGrade(List<String> filter, String query, Handler<Either<String, JsonArray>> handler);


}
