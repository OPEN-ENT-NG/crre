package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ProjectService {
    /**
     * List all the projects
     *
     * @param handler
     */
    void getProjects(Handler<Either<String, JsonArray>> handler);

    /**
     * Create a project
     *
     * @param project
     * @param idCampaign
     * @param idStructure
     * @param eitherHandler
     */
    void createProject(JsonObject project, Integer idCampaign, String idStructure, Handler<Either<String, JsonObject>> eitherHandler);

    /**
     * Get a single project by his id
     *
     * @param id
     * @param handler
     */
    void getProject(Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * Create baskets from orders an delete the project
     *  @param value
     * @param id
     * @param idCampaign
     * @param handler
     */
    void revertOrderAndDeleteProject(JsonArray value, Integer id, Integer idCampaign, String idStructure, Handler<Either<String, JsonObject>> handler);

    /**
     * Select all the orders and their parameters to prepare the baskets insert
     *
     * @param id
     * @param handler
     */
    void selectOrdersToBaskets(Integer id, Handler<Either<String, JsonArray>> handler);

    /**
     * Update the project
     *
     * @param handler
     * @param project
     * @param id
     */
    void updateProject(JsonObject project, Handler<Either<String, JsonObject>> handler, Integer id);

    /**
     * Check if the project can be delete
     *
     * @param id
     * @param handler
     */
    void deletableProject(Integer id, Handler<Either<String, JsonObject>> handler);
}
