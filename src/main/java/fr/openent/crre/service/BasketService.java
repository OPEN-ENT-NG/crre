package fr.openent.crre.service;

import fr.openent.crre.model.BasketOrder;
import fr.openent.crre.model.BasketOrderItem;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface BasketService {
    /**
     * Create a basket order item
     *
     * @param basketOrderItem basket item to create with all information
     * @param user user information
     */
    Future<JsonObject> create(BasketOrderItem basketOrderItem, UserInfos user);

    /**
     * List basket order item for a user of a campaign and a structure
     *
     * @param idCampaign  campaign identifier
     * @param idStructure structure identifier
     * @param user user info
     */
    Future<List<BasketOrderItem>> listBasketOrderItem(Integer idCampaign, String idStructure, UserInfos user);

    /**
     * Get all order for a user
     *
     * @param user user info
     * @param page page to get
     * @param idCampaign campaign identifier
     * @param startDate start date for search
     * @param endDate end date for search
     * @param oldTable serach in history table or not
     */
    Future<List<BasketOrder>> getMyBasketOrders(UserInfos user, Integer page, Integer idCampaign, String startDate, String endDate,
                                                boolean oldTable);

    /**
     * Delete a basket item
     *
     * @param idBasket id of the basket item
     */
    Future<JsonObject> delete(Integer idBasket);

    /**
     * Update a basket's amount
     *
     * @param idBasket id of a basket item
     * @param amount   the new amount
     */
    Future<JsonObject> updateAmount(UserInfos user, Integer idBasket, Integer amount);

    /**
     * Update a basket's comment
     *
     * @param idBasket id of a basket item
     * @param comment the new comment
     */
    Future<JsonObject> updateComment(Integer idBasket, String comment);

    /**
     * Update a basket's reassort
     *
     * @param idBasket id of a basket item
     * @param reassort the new reassort value
     */
    Future<JsonObject> updateReassort(Integer idBasket, Boolean reassort);


    /**
     * Transform basket to an order
     *
     * @param basketOrderItemList
     * @param idCampaign
     * @param user
     * @param idStructure
     * @param nameBasket
     * @return
     */
    Future<JsonObject> takeOrder(List<BasketOrderItem> basketOrderItemList, Integer idCampaign, UserInfos user,
                                 String idStructure, String nameBasket);

    /**
     * List  basket list of a campaign and a structure to transform to an order
     *
     * @param idCampaign   id of the campaign
     * @param idStructure  id of the structure
     * @param basketIdList basket list to retrieve
     */
    Future<List<BasketOrderItem>> listBasketItemForOrder(Integer idCampaign, String idStructure, String idUser, List<Integer> basketIdList);

    /**
     * Search basket from a query (name, user_name or article)
     *
     * @param query       searching query (name, user_name or article)
     * @param user        user object
     * @param idCampaign campaign identifier
     * @param startDate
     * @param endDate
     * @param old
     */
    Future<List<BasketOrder>> search(String query, UserInfos user, JsonArray equipTab, int idCampaign, String startDate,
                                     String endDate, Integer page, Boolean old);
}
