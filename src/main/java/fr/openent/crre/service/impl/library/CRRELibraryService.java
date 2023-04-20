package fr.openent.crre.service.impl.library;

import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.library.CRREParam;
import fr.openent.crre.service.ILibraryService;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class CRRELibraryService implements ILibraryService<CRREParam> {
    @Override
    public void getStatus(CRREParam params) {
        //todo CRRE-577
    }

    @Override
    public void sendOrder(List<OrderUniversalModel> orderList, CRREParam params) {
        //todo CRRE-576
    }

    @Override
    public CRREParam generateModel(JsonObject jsonObject) {
        return new CRREParam(jsonObject);
    }
}
