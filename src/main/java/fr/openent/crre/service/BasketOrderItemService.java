package fr.openent.crre.service;

import fr.openent.crre.model.BasketOrderItem;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface BasketOrderItemService {
    /**
     * Create a basket order item
     *
     * @param basketOrderItem basket item to create with all information
     * @param user user information
     */
    Future<JsonObject> create(BasketOrderItem basketOrderItem, UserInfos user);

    /**
     * Delete a basket item
     *
     * @param idBasket id of the basket item
     */
    Future<JsonObject> delete(Integer idBasket);

    /**
     * Delete a list basket item
     *
     * @param basketIds list of id of the basket item
     */
    Future<List<BasketOrderItem>> delete(List<Integer> basketIds);

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
     * @param basketOrderItemList basket order items we want to add in order
     * @param idCampaign campaign identifier
     * @param user user information
     * @param idStructure structure identifier
     * @param nameBasket basket name
     */
    //todo décalé cette fonction dans orderService
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
     * List basket order item for a user of a campaign and a structure
     *
     * @param idCampaign  campaign identifier
     * @param idStructure structure identifier
     * @param userId user id
     */
    Future<List<BasketOrderItem>> listBasketOrderItem(Integer idCampaign, String idStructure, String userId, List<String> itemIds);
}
