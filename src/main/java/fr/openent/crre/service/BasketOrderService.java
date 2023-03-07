package fr.openent.crre.service;

import fr.openent.crre.model.BasketOrder;
import fr.openent.crre.model.TransactionElement;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface BasketOrderService {
    /**
     * Get all order for a user
     *
     * @param userId user info id
     * @param page page to get
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param startDate start date for search
     * @param endDate end date for search
     * @param oldTable serach in history table or not
     */
    Future<List<BasketOrder>> getMyBasketOrders(String userId, Integer page, Integer idCampaign, String idStructure,
                                                String startDate, String endDate, boolean oldTable);

    /**
     * Search basket from a query (name, user_name or article)
     *
     * @param query       searching query (name, user_name or article)
     * @param user        user object
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param startDate starting date filter
     * @param endDate ending date filter
     * @param old search in historic or not
     */
    Future<List<BasketOrder>> search(String query, UserInfos user, List<String> equipementIdList, int idCampaign, String idStructure,
                                     String startDate, String endDate, Integer page, Boolean old);

    /**
     * Get transaction element for create a new basketOrder
     *
     * @param user user information
     * @param idStructure structure identifier
     * @param idCampaign campaign identifier
     * @param basketName basket order name
     * @param total
     * @param amount price of total basket order
     */
    TransactionElement getTransactionInsertBasketName(UserInfos user, String idStructure, Integer idCampaign, String basketName, double total, int amount);

    /**
     * Get all basket order from a list of id
     *
     * @param basketIdList list of basket order id
     */
    Future<List<BasketOrder>> getBasketOrderList(List<Integer> basketIdList);

    /**
     * Get all basket order from a list of order region id
     *
     * @param orderRegionIdList list of order region id
     */
    Future<List<BasketOrder>> getBasketOrderListByOrderRegion(List<Integer> orderRegionIdList);
}
