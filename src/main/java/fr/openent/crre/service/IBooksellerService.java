package fr.openent.crre.service;

import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.config.bookseller.IBooksellerConfigModel;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface IBooksellerService<T extends IBooksellerConfigModel> {
    Future<Void> updateStatus(T params);

    Future<Void> sendOrder(List<OrderUniversalModel> orderList, T params);

    T generateModel(JsonObject jsonObject);
}
