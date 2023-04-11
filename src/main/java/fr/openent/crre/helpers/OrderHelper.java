package fr.openent.crre.helpers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.FilterModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.ExportWorker;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;

public class OrderHelper {
    private static final Logger log = LoggerFactory.getLogger(OrderHelper.class);

    public static Future<List<OrderUniversalModel>> listOrderAndCalculatePrice(FilterModel filterModel, HttpServerRequest request) {
        Promise<List<OrderUniversalModel>> promise = Promise.promise();

        Future<List<OrderUniversalModel>> orderFuture = ServiceFactory.getInstance().getOrderService().listOrder(filterModel.getSearchingText(), filterModel.getIdsCampaign(),
                filterModel.getIdsStructure(), filterModel.getIdsUser(), new ArrayList<>(), filterModel.getIdsOrder(),
                filterModel.getStartDate(), filterModel.getEndDate(), filterModel.getStatus());
        orderFuture.compose(orderUniversalModels -> {
                    List<String> itemIds = orderUniversalModels.stream()
                            .filter(order -> !order.getStatus().isHistoricStatus())
                            .map(OrderUniversalModel::getEquipmentKey)
                            .distinct()
                            .collect(Collectors.toList());
                    return itemIds.size() > 0 ? searchByIds(itemIds, null) : Future.succeededFuture(new JsonArray());
                })
                .onSuccess(itemArray -> {
                    List<OrderUniversalModel> orderUniversalModels = orderFuture.result();
                    List<JsonObject> itemsList = itemArray.stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .collect(Collectors.toList());
                    orderUniversalModels.stream()
                            .filter(orderUniversalModel -> !orderUniversalModel.getStatus().isHistoricStatus())
                            .forEach(orderUniversalModel -> {
                                JsonObject equipmentJson = itemsList.stream()
                                        .filter(item -> orderUniversalModel.getEquipmentKey().equals(item.getString(Field.ID)))
                                        .findFirst()
                                        .orElse(null);
                                if (equipmentJson != null) {
                                    JsonObject priceInfos = getPriceTtc(equipmentJson);
                                    orderUniversalModel.setEquipmentPrice(priceInfos.getDouble(Field.PRICETTC));
                                    orderUniversalModel.setEquipmentPriceht(priceInfos.getDouble(Field.PRIXHT));
                                    orderUniversalModel.setEquipmentTva5(priceInfos.getDouble(Field.PART_TVA5));
                                    orderUniversalModel.setEquipmentTva20(priceInfos.getDouble(Field.PART_TVA20));
                                    orderUniversalModel.setEquipmentName(equipmentJson.getString(Field.TITRE));
                                    orderUniversalModel.setOffers(equipmentJson.getJsonArray(Field.OFFRES));
                                } else {
                                    orderUniversalModel.setEquipmentPrice(0.0);
                                    orderUniversalModel.setEquipmentName(I18n.getInstance().translate("crre.item.not.found", Renders.getHost(request), I18n.acceptLanguage(request)));
                                }
                            });
                    promise.complete(orderUniversalModels);
                })
                .onFailure(error -> {
                    promise.fail(error);
                    log.error(String.format("[CRRE@%s::listOrderAndCalculatePrice] Fail list and calculate price %s::%s",
                            OrderHelper.class.getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                });

        return promise.future();
    }
}
