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
     * @param user user info
     * @param page page to get
     * @param idCampaign campaign identifier
     * @param startDate start date for search
     * @param endDate end date for search
     * @param oldTable serach in history table or not
     */
    Future<List<BasketOrder>> getMyBasketOrders(String userId, Integer page, Integer idCampaign, String startDate, String endDate,
                                                boolean oldTable);

    /**
     * Search basket from a query (name, user_name or article)
     *
     * @param query       searching query (name, user_name or article)
     * @param user        user object
     * @param idCampaign campaign identifier
     * @param startDate starting date filter
     * @param endDate ending date filter
     * @param old search in historic or not
     */
    Future<List<BasketOrder>> search(String query, UserInfos user, JsonArray equipTab, int idCampaign, String startDate,
                                     String endDate, Integer page, Boolean old);

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
}