package fr.openent.crre.service.impl;

import fr.openent.crre.service.ProgramService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultProgramService extends SqlCrudService implements ProgramService {

    public DefaultProgramService(String schema, String table) {
        super(schema, table);
    }

    public void listPrograms(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    @Override
    public void getProgramById(Number id, Handler<Either<String, JsonObject>> handler) {
        retrieve(id.toString(), handler);
    }
}
