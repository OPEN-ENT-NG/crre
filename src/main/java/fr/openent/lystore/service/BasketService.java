package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface BasketService {
    /**
     * Create a basket item
     * @param basket basket item to create
     * @param handler function handler returning data
     */
     void create(JsonObject basket, Handler<Either<String, JsonObject>> handler);

    /**
     * List  basket list of a campaign and a structure
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
     void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);

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
     * transform basket to an order
     * @param request the request
     * @param baskets list of basket's items to transform
     * @param idCampaign the id of the campaign
     * @param idStructure the id of the structure
     * @param nameStructure the name of the structure
     * @param idProject the id of the project
     * @param objects
     * @param handler function handler returning data
     */
    void takeOrder(HttpServerRequest request, JsonArray baskets, Integer idCampaign,
                   String idStructure, String nameStructure, Integer idProject, JsonArray objects, Handler<Either<String, JsonObject>> handler);
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
}
