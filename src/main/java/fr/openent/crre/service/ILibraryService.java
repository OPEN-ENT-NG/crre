package fr.openent.crre.service;

import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.library.ILibraryConfigModel;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ILibraryService<T extends ILibraryConfigModel> {
    void getStatus(T params);

    void sendOrder(List<OrderUniversalModel> orderList, T params);

    T generateModel(JsonObject jsonObject);
}
