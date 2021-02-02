package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PurseService {

    /**
     * Launch purse import
     * @param campaignId Campaign id
     * @param statementsValues Object containing structure ids as key and purse amount as value
     * @param handler Function handler
     */
    void launchImport(Integer campaignId, JsonObject statementsValues, Handler<Either<String, JsonObject>> handler);

    /**
     * Get purses by campaign id
     * @param campaignId campaign id
     * @param handler handler function returning data
     */
    void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler);

    /**
     * get statement to decrease or increase an amount of Purse
     * @param price total price of an equipment (with options)
     * @param idCampaign Campaign id
     * @param idStructure Structure id
     * @param operation "+" or "-"
     * @return Statment
     */
     JsonObject updatePurseAmountStatement(Double price,Integer idCampaign, String idStructure, String operation);

    /**
     * decrease or increase an amount of Purse
     * @param price total price of an equipment (with options)
     * @param idCampaign Campaign id
     * @param idStructure Structure id
     * @param operation "+" or "-"
     * @param handler
     */
    void updatePurseAmount(Double price,Integer idCampaign, String idStructure, String operation, Handler<Either<String, JsonObject>> handler);

    void checkPurses(Integer id, Handler<Either<String, JsonArray>> handler);
}
