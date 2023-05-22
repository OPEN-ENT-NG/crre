package fr.openent.crre.helpers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.model.FilterModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.model.OrderUniversalOfferModel;
import fr.openent.crre.service.ServiceFactory;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.JsonHelper.jsonArrayToList;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;

public class OrderHelper {
    private static final Logger log = LoggerFactory.getLogger(OrderHelper.class);

    public static Future<List<OrderUniversalModel>> listOrderAndCalculateEquipmentFromId(FilterModel filterModel, HttpServerRequest request) {
        Promise<List<OrderUniversalModel>> promise = Promise.promise();
        List<List<Integer>> partition = ListUtils.partition(filterModel.getIdsOrder().stream().distinct().collect(Collectors.toList()), 5000);

        Function<List<Integer>, Future<List<OrderUniversalModel>>> function = idOrderList ->
                listOrderAndCalculatePrice(filterModel.clone().setIdsOrder(idOrderList), request);

        FutureHelper.compositeSequential(function, partition, true)
                .onSuccess(res ->
                        promise.complete(res.stream().flatMap(listFuture -> listFuture.result().stream()).collect(Collectors.toList())))
                .onFailure(error -> {
                    log.error(String.format("[CRRE@%s::listOrderAndCalculateEquipmentFromId] Fail to list order and calculate equipment from id %s::%s",
                            OrderHelper.class.getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                    promise.fail(error);
                });

        return promise.future();
    }

    public static Future<List<OrderUniversalModel>> listOrderAndCalculatePrice(FilterModel filterModel, HttpServerRequest request) {
        Promise<List<OrderUniversalModel>> promise = Promise.promise();

        Future<List<OrderUniversalModel>> orderFuture = ServiceFactory.getInstance().getOrderService().listOrder(filterModel);
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
                                    orderUniversalModel.setEquipmentImage(equipmentJson.getString(Field.URLCOUVERTURE));
                                    orderUniversalModel.setEquipmentEditor(equipmentJson.getString(Field.EDITEUR, ""));
                                    orderUniversalModel.setEquipmentDiffusor(equipmentJson.getString(Field.DISTRIBUTEUR, ""));
                                    orderUniversalModel.setEquipmentCatalogueType(getUniqueTypeCatalogue(orderUniversalModel, equipmentJson));
                                    orderUniversalModel.setEquipmentType(equipmentJson.getString(Field.TYPE, ""));
                                    orderUniversalModel.setEquipmentBookseller(equipmentJson.getString(ItemField.BOOKSELLER));
                                    if (Field.ARTICLENUMERIQUE.equals(orderUniversalModel.getEquipmentType())) {
                                        orderUniversalModel.setOffers(computeOffersUniversal(equipmentJson.getJsonArray(Field.OFFRES), orderUniversalModel));
                                        orderUniversalModel.setEquipmentEanLibrary(equipmentJson.getJsonArray(Field.OFFRES).getJsonObject(0).getString(Field.EANLIBRAIRE));
                                    } else {
                                        orderUniversalModel.setEquipmentEanLibrary(orderUniversalModel.getEquipmentKey());
                                    }
                                } else {
                                    orderUniversalModel.setEquipmentPrice(0.0);
                                    orderUniversalModel.setEquipmentImage("/crre/public/img/pages-default.png");
                                    if (request != null) {
                                        orderUniversalModel.setEquipmentName(I18n.getInstance().translate("crre.item.not.found", Renders.getHost(request), I18n.acceptLanguage(request)));
                                    } else {
                                        orderUniversalModel.setEquipmentName(I18n.getInstance().translate("crre.item.not.found", Field.DEFAULT_DASH_DOMAIN, (String) null));
                                    }
                                    orderUniversalModel.setValid(false);
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

    //todo #Territoire Rajouter territoire
    private static String getUniqueTypeCatalogue(OrderUniversalModel order, JsonObject equipment) {
        if (equipment.getString(Field.TYPECATALOGUE, "").contains("|")) {
            if (order.getCampaign().getUseCredit() != null && order.getCampaign().getUseCredit().contains(Field.CONSUMABLE_MIN)) {
                if (Field.ARTICLEPAPIER.equals(equipment.getString(Field.TYPE))) {
                    return Field.AO_IDF_CONSO;
                } else {
                    return Field.CONSOMMABLE;
                }
            } else {
                if (Field.ARTICLEPAPIER.equals(equipment.getString(Field.TYPE))) {
                    if ((order.getCampaign().getCatalog() != null && order.getCampaign().getCatalog().contains(Field.PRO)) ||
                            !equipment.getString(Field.TYPECATALOGUE).contains(Field.AO_IDF_PAP)) {
                        return Field.AO_IDF_PAP_PRO;
                    } else {
                        return Field.AO_IDF_PAP;
                    }
                } else {
                    if (ArrayUtils.contains(equipment.getString(Field.TYPECATALOGUE).split(Pattern.quote("|")), Field.RESSOURCE)) {
                        return Field.RESSOURCE;
                    } else {
                        return Field.NUMERIQUE;
                    }
                }
            }
        } else {
            return equipment.getString(Field.TYPECATALOGUE);
        }
    }

    private static List<OrderUniversalOfferModel> computeOffersUniversal(JsonArray offersJsonArray, OrderUniversalModel orderUniversalModel) {
        List<OrderUniversalOfferModel> offers = new ArrayList<>();
        if (!offersJsonArray.isEmpty() && offersJsonArray.getJsonObject(0) != null &&
                offersJsonArray.getJsonObject(0).getJsonArray(Field.LEPS) != null &&
                !offersJsonArray.getJsonObject(0).getJsonArray(Field.LEPS).isEmpty()) {
            JsonArray leps = offersJsonArray.getJsonObject(0).getJsonArray(Field.LEPS);
            Integer amount = orderUniversalModel.getAmount();
            int gratuit = 0;
            int gratuite = 0;
            for (int i = 0; i < leps.size(); i++) {
                JsonObject lep = leps.getJsonObject(i);
                JsonArray conditions = lep.getJsonArray(Field.CONDITIONS);
                OrderUniversalOfferModel orderUniversalOffer = new OrderUniversalOfferModel();
                if (conditions != null && conditions.size() > 1) {
                    for (int j = 0; j < conditions.size(); j++) {
                        int condition = conditions.getJsonObject(j).getInteger(Field.CONDITIONS_FREE);
                        if (amount >= condition && gratuit < condition) {
                            gratuit = condition;
                            gratuite = conditions.getJsonObject(j).getInteger(Field.FREE);
                        }
                    }
                } else if (conditions != null && conditions.size() == 1) {
                    gratuit = conditions.getJsonObject(0).getInteger(Field.CONDITIONS_FREE);
                    gratuite = (int) (conditions.getJsonObject(0).getInteger(Field.FREE) * Math.floor(amount * 1.0 / gratuit));
                }

                if (gratuite > 0) {
                    orderUniversalOffer.setAmount(gratuite);
                    orderUniversalOffer.setName(lep.getString(Field.TITRE));
                    orderUniversalOffer.setTitre(offersJsonArray.getJsonObject(0).getJsonArray(Field.LICENCE).getJsonObject(0).getString(Field.VALEUR));
                    orderUniversalOffer.setEan(lep.getString(Field.EAN));
                    orderUniversalOffer.setTotalPriceTTC(0.0);
                    orderUniversalOffer.setTotalPriceHT(0.0);
                    orderUniversalOffer.setUnitedPriceTTC(0.0);
                    orderUniversalOffer.setTypeCatalogue(orderUniversalModel.getEquipmentCatalogueType());

                    orderUniversalOffer.setIdOffer("F" + orderUniversalModel.getOrderRegionId() + "_" + i);
                    orderUniversalOffer.setOrderUniversalModel(orderUniversalModel);

                    offers.add(orderUniversalOffer);
                }
            }
        }
        return offers;
    }
}
