package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.GradeService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

public class DefaultGradeService extends SqlCrudService implements GradeService {

    public DefaultGradeService(String schema, String table) {
        super(schema, table);
    }

    /**
     * List all the grades
     *
     * @param handler
     */
    @Override
    public void getGrades(Handler<Either<String, JsonArray>> handler) {
        String query = " SELECT grade.name, grade.id  FROM " +
                Crre.crreSchema + ".grade " +
                " GROUP BY id; ";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


}
