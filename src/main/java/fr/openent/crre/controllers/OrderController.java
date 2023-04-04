package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.OrderClientEquipmentModel;
import fr.openent.crre.model.OrderRegionBeautifyModel;
import fr.openent.crre.model.OrderUniversalModel;
import fr.openent.crre.security.*;
import fr.openent.crre.service.NotificationService;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import static fr.openent.crre.core.constants.Field.UTF8_BOM;
import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.helpers.JsonHelper.jsonArrayToList;
import static fr.openent.crre.utils.OrderUtils.convertPriceString;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static java.lang.Integer.parseInt;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;


public class OrderController extends ControllerHelper {

    private final OrderService orderService;
    private final NotificationService notificationService;

    public OrderController(ServiceFactory serviceFactory) {
        this.orderService = serviceFactory.getOrderService();
        this.notificationService = serviceFactory.getNotificationService();
    }

    @Get("/orders/mine/:idCampaign/:idStructure")
    @ApiDoc("Get my list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listMyOrdersByCampaignByStructure(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user -> {
                Integer idCampaign = Integer.parseInt(request.params().get(Field.IDCAMPAIGN));
                String idStructure = request.params().get(Field.IDSTRUCTURE);
                List<String> basketIdList = request.params().getAll(Field.BASKET_ID);
                String startDate = request.getParam(Field.STARTDATE);
                String endDate = request.getParam(Field.ENDDATE);
                Boolean old = request.getParam(Field.OLD) == null ? null : Boolean.parseBoolean(request.getParam(Field.OLD));

                List<OrderStatus> orderStatusList = Arrays.stream(OrderStatus.values())
                        .filter(orderStatus -> old == null || orderStatus.isHistoricStatus() == old)
                        .collect(Collectors.toList());

                Future<List<OrderUniversalModel>> orderFuture = orderService.listOrder(Collections.singletonList(idCampaign),
                        Collections.singletonList(idStructure), Collections.singletonList(user.getUserId()), basketIdList, new ArrayList<>(),
                        startDate, endDate, orderStatusList);

                orderFuture
                        .compose(orderUniversalModels -> {
                            List<String> idEquipments = orderUniversalModels.stream()
                                    .filter(orderUniversalModel -> !orderUniversalModel.getStatus().isHistoricStatus())
                                    .map(OrderUniversalModel::getEquipmentKey)
                                    .distinct()
                                    .collect(Collectors.toList());
                            return searchByIds(idEquipments, null);
                        })
                        .onSuccess(equipmentsArray -> {
                            List<JsonObject> equipmentList = equipmentsArray.stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast)
                                    .collect(Collectors.toList());
                            List<OrderUniversalModel> orderUniversalModels = orderFuture.result();
                            orderUniversalModels.stream()
                                    .filter(orderUniversalModel -> !orderUniversalModel.getStatus().isHistoricStatus())
                                    .forEach(orderUniversalModel -> {
                                        JsonObject equipmentJson = equipmentList.stream()
                                                .filter(equipment -> orderUniversalModel.getEquipmentKey().equals(equipment.getString(Field.ID)))
                                                .findFirst()
                                                .orElse(null);
                                        if (equipmentJson != null) {
                                            JsonObject priceInfos = getPriceTtc(equipmentJson);
                                            orderUniversalModel.setEquipmentPrice(priceInfos.getDouble(Field.PRICETTC));
                                            orderUniversalModel.setEquipmentName(equipmentJson.getString(Field.TITRE));
                                            orderUniversalModel.setEquipmentImage(equipmentJson.getString(Field.URLCOUVERTURE));
                                            orderUniversalModel.setOffers(computeOffers(orderUniversalModel, equipmentJson.getJsonArray(Field.OFFRES)));
                                        } else {
                                            orderUniversalModel.setEquipmentPrice(0.0);
                                            orderUniversalModel.setEquipmentName(I18n.getInstance().translate("crre.item.not.found", getHost(request), I18n.acceptLanguage(request)));
                                            orderUniversalModel.setEquipmentImage("/crre/public/img/pages-default.png");
                                        }
                                    });

                            renderJson(request, IModelHelper.toJsonArray(orderUniversalModels));
                        })
                        .onFailure(error -> {
                            badRequest(request);
                            log.error(String.format("[CRRE@%s::listMyOrdersByCampaignByStructure] Problem when catching orders %s",
                                    this.getClass().getSimpleName(), error.getMessage()));
                        });
            });
        } catch (ClassCastException e) {
            log.error("An error occured when casting campaign id ", e);
            renderError(request);
        }
    }

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
    public void listOrders(final HttpServerRequest request) {
        if (request.params().contains(Field.STATUS)) {
            final String status = request.params().get(Field.STATUS);
            String idStructure = request.getParam("idStructure");
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            Integer page = request.getParam(Field.PAGE) == null ? null : Integer.parseInt(request.params().get(Field.PAGE));
            orderService.listOrder(status, idStructure, page, startDate, endDate, arrayResponseHandler(request));
        } else {
            badRequest(request);
        }
    }


    @Get("/orders/amount")
    @ApiDoc("Get the total amount of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
    public void listOrdersAmountLicences(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains(Field.STATUS)) {
                final String status = request.params().get(Field.STATUS);
                String idStructure = request.getParam("idStructure");
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                // Récupération de tout les filtres
                JsonArray filters = new JsonArray();
                boolean exist;
                int length = request.params().entries().size();
                for (int i = 0; i < length; i++) {
                    String key = request.params().entries().get(i).getKey();
                    exist = false;
                    if (!key.equals(Field.ID) && !key.equals("q") && !key.equals("idStructure") && !key.equals("page") &&
                            !key.equals("startDate") && !key.equals("endDate")) {
                        for (int f = 0; f < filters.size(); f++) {
                            if (filters.getJsonObject(f).containsKey(key)) {
                                filters.getJsonObject(f).getJsonArray(key).add(request.params().entries().get(i).getValue());
                                exist = true;
                            }
                        }
                        if (!exist) {
                            filters.add(new JsonObject().put(request.params().entries().get(i).getKey(), new JsonArray().add(request.params().entries().get(i).getValue())));
                        }
                    }
                }
                Promise<JsonObject> getOrderAmount = Promise.promise();
                Promise<JsonArray> getOrderCredit = Promise.promise();
                Promise<JsonObject> getOrderAmountConsumable = Promise.promise();
                Promise<JsonArray> getTotalAmount = Promise.promise();
                List<Future> promises = new ArrayList<>();
                promises.add(getOrderAmount.future());
                promises.add(getOrderCredit.future());
                promises.add(getOrderAmountConsumable.future());
                promises.add(getTotalAmount.future());
                JsonObject result = new JsonObject();
                CompositeFuture.all(promises).onComplete(event -> {
                    if (event.succeeded()) {
                        int amount = 0;
                        if (getOrderAmount.future().result().getString("nb_licences") != null) {
                            amount = Integer.parseInt(getOrderAmount.future().result().getString("nb_licences"));
                        }
                        result.put(Field.LICENCE, amount);
                        amount = 0;
                        if (getOrderAmountConsumable.future().result().getString("nb_licences") != null) {
                            amount = Integer.parseInt(getOrderAmountConsumable.future().result().getString("nb_licences"));
                        }
                        result.put("consumable_licence", amount);
                        JsonArray totalAmount = getTotalAmount.future().result();
                        JsonArray order_credit = getOrderCredit.future().result();
                        if (order_credit.size() > 0) {
                            HashSet<String> idsEquipment = new HashSet<>();
                            HashSet<Long> idsOrderFiltered = new HashSet<>();
                            int total_amount = 0;
                            for (int i = 0; i < order_credit.size(); i++) {
                                idsEquipment.add(order_credit.getJsonObject(i).getString("equipment_key"));
                            }
                            for (int i = 0; i < totalAmount.size(); i++) {
                                idsOrderFiltered.add(totalAmount.getJsonObject(i).getLong(Field.ID));
                                total_amount += totalAmount.getJsonObject(i).getLong("amount");
                            }
                            int finalTotal_amount = total_amount;
                            searchByIds(new ArrayList<>(idsEquipment), null, equipments -> {
                                if (equipments.isRight()) {
                                    JsonArray equipmentsArray = equipments.right().getValue();
                                    double total = 0;
                                    double totalFiltered = 0;
                                    double total_consumable = 0;
                                    for (int i = 0; i < order_credit.size(); i++) {
                                        JsonObject order = order_credit.getJsonObject(i);
                                        String idEquipment = order.getString("equipment_key");
                                        Long idOrder = order.getLong(Field.ID);
                                        String credit = order.getString("use_credit");
                                        if (equipmentsArray.size() > 0) {
                                            for (int j = 0; j < equipmentsArray.size(); j++) {
                                                JsonObject equipment = equipmentsArray.getJsonObject(j);
                                                if (idEquipment.equals(equipment.getString(Field.ID))) {
                                                    double totalPriceEquipment = order.getInteger("amount") *
                                                            getPriceTtc(equipment).getDouble("priceTTC");
                                                    if ((credit.equals("credits") || credit.equals("consumable_credits")) && idsOrderFiltered.contains(idOrder)) {
                                                        totalFiltered += totalPriceEquipment;
                                                    }
                                                    if (credit.equals("credits"))
                                                        total += totalPriceEquipment;
                                                    else
                                                        total_consumable += totalPriceEquipment;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    result.put("credit", total);
                                    result.put("consumable_credit", total_consumable);
                                    result.put("total", finalTotal_amount);
                                    result.put("total_filtered", totalFiltered);
                                    renderJson(request, result);
                                } else {
                                    log.error("[CRRE] OrderController@listOrdersAmount searchByIds failed : " + equipments.left().getValue());
                                    badRequest(request);
                                }
                            });
                        } else {
                            renderJson(request, result);
                        }
                    } else {
                        log.error("[CRRE] OrderController@listOrdersAmount CompositeFuture.all failed : " + event.cause().getMessage());
                        badRequest(request);
                    }
                });
                orderService.listOrderAmount(status, idStructure, user, startDate, endDate, false, handlerJsonObject(getOrderAmount));
                orderService.listOrderAmount(status, idStructure, user, startDate, endDate, true, handlerJsonObject(getOrderAmountConsumable));
                orderService.getTotalAmountOrder(status, idStructure, user, startDate, endDate, filters, handlerJsonArray(getTotalAmount));
                orderService.listOrderCredit(status, idStructure, user, startDate, endDate, filters, handlerJsonArray(getOrderCredit));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/users")
    @ApiDoc("Get the list of users who orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
    public void listUsers(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains(Field.STATUS)) {
                final String status = request.params().get(Field.STATUS);
                final String idStructure = request.params().get("idStructure");
                orderService.listUsers(status, idStructure, user, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/search_filter")
    @ApiDoc("Filter orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorAndStructureRight.class)
    public void filter(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = request.getParam(Field.PAGE) == null ? null : Integer.parseInt(request.params().get(Field.PAGE));
                String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
                String startDate = request.getParam(Field.STARTDATE);
                String endDate = request.getParam(Field.ENDDATE);
                String idStructure = request.getParam(Field.IDSTRUCTURE);

                // Récupération de tout les filtres
                final Map<String, List<String>> filters = request.params().entries().stream()
                        .filter(stringStringEntry -> !stringStringEntry.getKey().equals(Field.ID) &&
                                !stringStringEntry.getKey().equals(Field.Q) && !stringStringEntry.getKey().equals(Field.IDSTRUCTURE) &&
                                !stringStringEntry.getKey().equals(Field.PAGE) &&
                                !stringStringEntry.getKey().equals(Field.STARTDATE) && !stringStringEntry.getKey().equals(Field.ENDDATE))
                        .collect(Collectors.groupingBy(Map.Entry::getKey))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                stringListEntry -> stringListEntry.getValue().stream().map(Map.Entry::getValue).collect(Collectors.toList())));

                // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                if (request.params().contains(Field.Q)) {
                    q = URLDecoder.decode(request.getParam(Field.Q), Field.UTF_DASH_8).toLowerCase();
                }
                Integer id_campaign = null;
                if (request.getParam(Field.ID) != null) {
                    id_campaign = parseInt(request.getParam(Field.ID));
                }
                String finalQ = q;
                Integer finalId_campaign = id_campaign;
                plainTextSearchName(finalQ, Collections.singletonList(Field.EAN), equipments -> {
                    List<String> equipementIdList = equipments.right().getValue().stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(jsonObject -> jsonObject.getString(Field.EAN))
                            .collect(Collectors.toList());
                    orderService.search(finalQ, filters, idStructure, equipementIdList, finalId_campaign, startDate, endDate, page)
                            .onSuccess(result -> Renders.renderJson(request, result))
                            .onFailure(error -> Renders.renderError(request));
                });
            } catch (UnsupportedEncodingException | NumberFormatException e) {
                e.printStackTrace();
                renderError(request);
            }
        });
    }

    @Get("/orders/exports")
    @ApiDoc("Export list of customer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorAndStructureRight.class)
    public void export(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request,
                user -> {
                    List<String> orderIds = request.params().getAll(Field.ID);
                    List<Integer> idsOrders = orderIds == null ? new ArrayList<>() : SqlQueryUtils.getIntegerIds(orderIds);

                    Integer idCampaign = Integer.parseInt(request.params().get(Field.IDCAMPAIGN));
                    String idStructure = request.params().get(Field.IDSTRUCTURE);
                    String startDate = request.getParam(Field.STARTDATE);
                    String endDate = request.getParam(Field.ENDDATE);
                    Boolean old = request.getParam(Field.OLD) == null ? null : Boolean.parseBoolean(request.getParam(Field.OLD));

                    List<OrderStatus> orderStatusList = Arrays.stream(OrderStatus.values())
                            .filter(orderStatus -> old == null || orderStatus.isHistoricStatus() == old)
                            .collect(Collectors.toList());
                    Future<List<OrderUniversalModel>> orderFuture = orderService.listOrder(Collections.singletonList(idCampaign),
                            Collections.singletonList(idStructure), Collections.singletonList(user.getUserId()), new ArrayList<>(), idsOrders,
                            startDate, endDate, orderStatusList);
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
                                                orderUniversalModel.setEquipmentName(I18n.getInstance().translate("crre.item.not.found", getHost(request), I18n.acceptLanguage(request)));
                                            }
                                        });
                                new ArrayList<>(orderUniversalModels).forEach(orderUniversal -> {
                                    if (orderUniversal.getOffers() != null && orderUniversal.getOffers().size() > 0) {
                                        int index = orderUniversalModels.indexOf(orderUniversal);
                                        orderUniversalModels.addAll(index + 1, computeOffersUniversal(orderUniversal));
                                    }
                                });
                                request.response()
                                        .putHeader("Content-Type", "text/csv; charset=utf-8")
                                        .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                        .end(generateExport(request, orderUniversalModels));
                            });
                });
    }

    private List<OrderUniversalModel> computeOffersUniversal(OrderUniversalModel orderUniversal) {
        List<OrderUniversalModel> offers = new ArrayList<>();
        if ((orderUniversal.getOffers().isEmpty() || orderUniversal.getOffers().getValue(0) instanceof JsonObject) &&
        orderUniversal.getOffers().getJsonObject(0).getString(Field.ID) != null) {
            jsonArrayToList(orderUniversal.getOffers(), JsonObject.class).forEach(offer -> {
                OrderUniversalModel orderUniversalOffer = new OrderUniversalModel()
                        .setAmount(offer.getInteger(Field.AMOUNT))
                        .setEquipmentName(offer.getString(Field.NAME))
                        .setComment(offer.getString(Field.COMMENT))
                        .setEquipmentKey(offer.getString(Field.EAN))
                        .setPrescriberValidationDate(offer.getString(Field.CREATION_DATE))
                        .setBasket(orderUniversal.getBasket());
                offers.add(orderUniversalOffer);
            });
        } else {
            if(orderUniversal.getOffers().isEmpty() || orderUniversal.getOffers().getValue(0) instanceof JsonObject &&
                    orderUniversal.getOffers().getJsonObject(0).getJsonArray(Field.LEPS) != null &&
                    orderUniversal.getOffers().getJsonObject(0).getJsonArray(Field.LEPS).size() > 0) {
                JsonArray leps = orderUniversal.getOffers().getJsonObject(0).getJsonArray(Field.LEPS);
                Integer amount = orderUniversal.getAmount();
                int gratuit = 0;
                int gratuite = 0;
                for (int i = 0; i < leps.size(); i++) {
                    JsonObject offer = leps.getJsonObject(i);
                    JsonArray conditions = offer.getJsonArray(Field.CONDITIONS);
                    OrderUniversalModel orderUniversalOffer = new OrderUniversalModel();
                    if (conditions.size() > 1) {
                        for (int j = 0; j < conditions.size(); j++) {
                            int condition = conditions.getJsonObject(j).getInteger(Field.CONDITIONS_FREE);
                            if (amount >= condition && gratuit < condition) {
                                gratuit = condition;
                                gratuite = conditions.getJsonObject(j).getInteger(Field.FREE);
                            }
                        }
                    } else if (offer.getJsonArray(Field.CONDITIONS).size() == 1) {
                        gratuit = offer.getJsonArray(Field.CONDITIONS).getJsonObject(0).getInteger(Field.CONDITIONS_FREE);
                        gratuite = (int) (offer.getJsonArray(Field.CONDITIONS).getJsonObject(0).getInteger(Field.FREE) * Math.floor(amount / gratuit));
                    }

                    if (gratuite > 0) {
                        orderUniversalOffer.setAmount(gratuite);
                        orderUniversalOffer.setEquipmentName(offer.getString(Field.TITRE));
                        orderUniversalOffer.setComment(orderUniversal.getEquipmentKey());
                        orderUniversalOffer.setEquipmentKey(offer.getString(Field.EAN));
                        orderUniversalOffer.setPrescriberValidationDate(orderUniversal.getPrescriberValidationDate());
                        orderUniversalOffer.setBasket(orderUniversal.getBasket());
                        offers.add(orderUniversalOffer);
                    }
                }
            }
        }
        return offers;
    }

    private JsonArray computeOffers(OrderUniversalModel orderUniversalModel, JsonArray equipmentOffers) {
        JsonArray offers = new JsonArray();
        if (equipmentOffers != null && equipmentOffers.getJsonObject(0).getJsonArray(Field.LEPS).size() > 0) {
            JsonArray leps = equipmentOffers.getJsonObject(0).getJsonArray(Field.LEPS);
            Integer amount = orderUniversalModel.getAmount();
            int gratuit = 0;
            int gratuite = 0;
            for (int i = 0; i < leps.size(); i++) {
                JsonObject offer = leps.getJsonObject(i);
                JsonArray conditions = offer.getJsonArray(Field.CONDITIONS);
                JsonObject newOffer = new JsonObject()
                        .put(Field.TITRE, Field.MANUAL + " " + offer.getJsonArray(Field.LICENCE).getJsonObject(0).getString(Field.VALUE));
                if (conditions.size() > 1) {
                    for (int j = 0; j < conditions.size(); j++) {
                        int condition = conditions.getJsonObject(j).getInteger(Field.CONDITIONS_FREE);
                        if (amount >= condition && gratuit < condition) {
                            gratuit = condition;
                            gratuite = conditions.getJsonObject(j).getInteger(Field.FREE);
                        }
                    }
                } else if (offer.getJsonArray(Field.CONDITIONS).size() == 1) {
                    gratuit = offer.getJsonArray(Field.CONDITIONS).getJsonObject(0).getInteger(Field.CONDITIONS_FREE);
                    gratuite = (int) (offer.getJsonArray(Field.CONDITIONS).getJsonObject(0).getInteger(Field.FREE) * Math.floor(amount / gratuit));
                }

                newOffer.put(Field.AMOUNT, gratuite);
                newOffer.put(Field.ID, offer.getString(Field.EAN));
                newOffer.put(Field.NAME, offer.getString(Field.TITRE));
                if (gratuite > 0) {
                    offers.add(newOffer);
                }
            }
        }
        return offers;
    }

    private static String generateExport(HttpServerRequest request, List<OrderUniversalModel> orders) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < orders.size(); i++) {
            report.append(generateExportLine(request, orders.get(i)));
        }
        return report.toString();
    }

    private static String getExportHeader(HttpServerRequest request) {
        return I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("basket", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("name.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate(Field.EAN, getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.unit.price.ht", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.5", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.20", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.unit.price.ttc", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountHT", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountTTC", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("csv.comment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate(Field.STATUS, getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    private static String generateExportLine(HttpServerRequest request, OrderUniversalModel order) {
        return (order.getPrescriberValidationDateFormat() != null ? order.getPrescriberValidationDateFormat() : "") + ";" +
                (order.getBasket().getName() != null ? order.getBasket().getName() : "") + ";" +
                (order.getEquipmentName() != null ? order.getEquipmentName() : "") + ";" +
                (order.getEquipmentKey() != null ? order.getEquipmentKey() : "") + ";" +
                exportPriceComment(order) +
                (order.getStatus() != null ? I18n.getInstance().translate(order.getStatus().toString(), getHost(request), I18n.acceptLanguage(request)) : "")
                + "\n";
    }

    public static String exportPriceComment(OrderRegionBeautifyModel orderRegionBeautifyModel) {
        return (orderRegionBeautifyModel.getOrderRegion().getAmount() != null ? orderRegionBeautifyModel.getOrderRegion().getAmount() : "") + ";" +
                (orderRegionBeautifyModel.getPriceht() != null ? orderRegionBeautifyModel.getPriceht() : "") + ";" +
                (orderRegionBeautifyModel.getTva5() != null ? orderRegionBeautifyModel.getTva5() : "") + ";" +
                (orderRegionBeautifyModel.getTva20() != null ? orderRegionBeautifyModel.getTva20() : "") + ";" +
                (orderRegionBeautifyModel.getUnitedPriceTTC() != null ? convertPriceString(orderRegionBeautifyModel.getUnitedPriceTTC()) : "") + ";" +
                (orderRegionBeautifyModel.getTotalPriceHT() != null ? convertPriceString(orderRegionBeautifyModel.getTotalPriceHT()) : "") + ";" +
                (orderRegionBeautifyModel.getTotalPriceTTC() != null ? convertPriceString(orderRegionBeautifyModel.getTotalPriceTTC()) : "") + ";" +
                (orderRegionBeautifyModel.getOrderRegion().getComment() != null ? orderRegionBeautifyModel.getOrderRegion().getComment().replaceAll("\n", "").replaceAll("\r", "") : "") + ";";
    }

    public static String exportPriceComment(OrderUniversalModel orderUniversalModel) {
        return (orderUniversalModel.getAmount() != null ? orderUniversalModel.getAmount() : "") + ";" +
                (orderUniversalModel.getEquipmentPriceht() != null ? orderUniversalModel.getEquipmentPriceht() : "") + ";" +
                (orderUniversalModel.getEquipmentPriceTva5() != null ? orderUniversalModel.getEquipmentTva5() : "") + ";" +
                (orderUniversalModel.getEquipmentPriceTva20() != null ? orderUniversalModel.getEquipmentTva20() : "") + ";" +
                (orderUniversalModel.getUnitedPriceTTC() != null ? convertPriceString(orderUniversalModel.getUnitedPriceTTC()) : "") + ";" +
                (orderUniversalModel.getTotalPriceHT() != null ? convertPriceString(orderUniversalModel.getTotalPriceHT()) : "") + ";" +
                (orderUniversalModel.getTotalPriceTTC() != null ? convertPriceString(orderUniversalModel.getTotalPriceTTC()) : "") + ";" +
                (orderUniversalModel.getComment() != null ? orderUniversalModel.getComment().replaceAll("\n", "").replaceAll("\r", "") : "") + ";";
    }

    /**
     * @deprecated Use {@link #exportPriceComment(OrderRegionBeautifyModel)}
     */
    @Deprecated
    public static String exportPriceComment(JsonObject log) {
        return (log.getInteger("amount") != null ? log.getInteger("amount").toString() : "") + ";" +
                (log.getDouble("priceht") != null ? log.getDouble("priceht").toString() : "") + ";" +
                (log.getDouble("tva5") != null ? log.getDouble("tva5").toString() : "") + ";" +
                (log.getDouble("tva20") != null ? log.getDouble("tva20").toString() : "") + ";" +
                (log.getDouble("unitedPriceTTC") != null ? convertPriceString(log.getDouble("unitedPriceTTC")) : "") + ";" +
                (log.getDouble("totalPriceHT") != null ? convertPriceString(log.getDouble("totalPriceHT")) : "") + ";" +
                (log.getDouble("totalPriceTTC") != null ? convertPriceString(log.getDouble("totalPriceTTC")) : "") + ";" +
                (log.getString("comment") != null ?
                        log.getString("comment").replaceAll("\n", "").replaceAll("\r", "") : "") + ";";
    }

    public static String exportStudents(OrderRegionBeautifyModel orderRegionBeautify) {
        return (orderRegionBeautify.getStudents().getSeconde() != null ? orderRegionBeautify.getStudents().getSeconde() : "") + ";" +
                (orderRegionBeautify.getStudents().getPremiere() != null ? orderRegionBeautify.getStudents().getPremiere() : "") + ";" +
                (orderRegionBeautify.getStudents().getTerminale() != null ? orderRegionBeautify.getStudents().getTerminale() : "") + ";" +
                (orderRegionBeautify.getStudents().getSecondetechno() != null ? orderRegionBeautify.getStudents().getSecondetechno() : "") + ";" +
                (orderRegionBeautify.getStudents().getPremieretechno() != null ? orderRegionBeautify.getStudents().getPremieretechno() : "") + ";" +
                (orderRegionBeautify.getStudents().getTerminaletechno() != null ? orderRegionBeautify.getStudents().getTerminaletechno() : "") + ";" +
                (orderRegionBeautify.getStudents().getSecondepro() != null ? orderRegionBeautify.getStudents().getSecondepro() : "") + ";" +
                (orderRegionBeautify.getStudents().getPremierepro() != null ? orderRegionBeautify.getStudents().getPremierepro() : "") + ";" +
                (orderRegionBeautify.getStudents().getTerminalepro() != null ? orderRegionBeautify.getStudents().getTerminalepro() : "") + ";" +
                (orderRegionBeautify.getStudents().getBma1() != null ? orderRegionBeautify.getStudents().getBma1() : "") + ";" +
                (orderRegionBeautify.getStudents().getBma2() != null ? orderRegionBeautify.getStudents().getBma2() : "") + ";" +
                (orderRegionBeautify.getStudents().getCap1() != null ? orderRegionBeautify.getStudents().getCap1() : "") + ";" +
                (orderRegionBeautify.getStudents().getCap2() != null ? orderRegionBeautify.getStudents().getCap2() : "") + ";" +
                (orderRegionBeautify.getStudents().getCap3() != null ? orderRegionBeautify.getStudents().getCap3() : "");
    }

    /**
     * @deprecated Use {@link #exportStudents(OrderRegionBeautifyModel)}
     */
    @Deprecated
    public static String exportStudents(JsonObject log) {
        return (log.getInteger("seconde") != null ? log.getInteger("seconde").toString() : "") + ";" +
                (log.getInteger("premiere") != null ? log.getInteger("premiere").toString() : "") + ";" +
                (log.getInteger("terminale") != null ? log.getInteger("terminale").toString() : "") + ";" +
                (log.getInteger("secondetechno") != null ? log.getInteger("secondetechno").toString() : "") + ";" +
                (log.getInteger("premieretechno") != null ? log.getInteger("premieretechno").toString() : "") + ";" +
                (log.getInteger("terminaletechno") != null ? log.getInteger("terminaletechno").toString() : "") + ";" +
                (log.getInteger("secondepro") != null ? log.getInteger("secondepro").toString() : "") + ";" +
                (log.getInteger("premierepro") != null ? log.getInteger("premierepro").toString() : "") + ";" +
                (log.getInteger("terminalepro") != null ? log.getInteger("terminalepro").toString() : "") + ";" +
                (log.getInteger("bma1") != null ? log.getInteger("bma1").toString() : "") + ";" +
                (log.getInteger("bma2") != null ? log.getInteger("bma2").toString() : "") + ";" +
                (log.getInteger("cap1") != null ? log.getInteger("cap1").toString() : "") + ";" +
                (log.getInteger("cap2") != null ? log.getInteger("cap2").toString() : "") + ";" +
                (log.getInteger("cap3") != null ? log.getInteger("cap3").toString() : "");
    }

    @Put("/orders/:status")
    @ApiDoc("update status orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(UpdateStatusRight.class)
    public void updateStatus(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", orders -> {
            OrderStatus status = OrderStatus.getValue(request.params().get(Field.STATUS));
            if (status != null) {
                List<Integer> orderClientEquipmentIdList = orders.getJsonArray(Field.IDS)
                        .stream()
                        .filter(Integer.class::isInstance)
                        .map(Integer.class::cast)
                        .collect(Collectors.toList());
                orderService.updateStatus(orderClientEquipmentIdList, status)
                        .onSuccess(orderClientEquipmentModels -> {
                            List<Integer> orderClientEquipmentIdUpdated = orderClientEquipmentModels.stream()
                                    .map(OrderClientEquipmentModel::getId)
                                    .collect(Collectors.toList());
                            Renders.renderJson(request, new JsonArray(orderClientEquipmentIdUpdated));
                            this.notificationService.sendNotificationPrescriber(orderClientEquipmentIdUpdated);
                            UserUtils.getUserInfos(eb, request, userInfos ->
                                    Logging.insert(userInfos, Contexts.ORDER.toString(), Actions.UPDATE.toString(),
                                            orderClientEquipmentIdUpdated.stream().map(String::valueOf).collect(Collectors.toList()),
                                            new JsonObject().put(Field.STATUS, status)));
                        })
                        .onFailure(error -> Renders.renderError(request));
            } else {
                noContent(request);
            }
        });
    }

    @Put("/order/:idOrder/amount")
    @ApiDoc("Update an order's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateAmounts(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, order -> {
            if (!order.containsKey("amount")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = Integer.parseInt(request.params().get("idOrder"));
                Integer amount = order.getInteger("amount");
                orderService.updateAmount(id, amount, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                renderError(request);
            }
        });
    }

    @Put("/order/:idOrder/reassort")
    @ApiDoc("Update an order's reassort")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderReassortRight.class)
    public void updateReassort(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, order -> {
            if (!order.containsKey("reassort")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = Integer.parseInt(request.params().get("idOrder"));
                Boolean reassort = order.getBoolean("reassort");
                orderService.updateReassort(id, reassort, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                renderError(request);
            }
        });
    }
}
