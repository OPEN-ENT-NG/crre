package fr.openent.crre.service.impl;

import fr.openent.crre.service.ContractTypeService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class DefaultContractTypeService extends SqlCrudService implements ContractTypeService {

    public DefaultContractTypeService(String schema, String table) {
        super(schema, table);
    }

    public void listContractTypes(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }
}
