package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface GradeService {
    /**
     * List all the grades
     *
     * @param eitherHandler
     */
    void getGrades(Handler<Either<String, JsonArray>> eitherHandler);


}
