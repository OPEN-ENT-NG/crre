package fr.openent.crre.controllers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.CreditTypeEnum;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.OrderHelper;
import fr.openent.crre.helpers.UserHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.model.*;
import fr.openent.crre.security.*;
import fr.openent.crre.service.NotificationService;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.utils.OrderUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
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
                List<OrderStatus> statusList = request.params().getAll(Field.STATUS).stream()
                        .map(OrderStatus::getValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                FilterModel filterModel = new FilterModel()
                        .setIdsCampaign(Collections.singletonList(idCampaign))
                        .setIdsStructure(Collections.singletonList(idStructure))
                        .setIdsUser(Collections.singletonList(user.getUserId()))
                        .setIdsBasket(basketIdList)
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .setStatus(statusList)
                        .setOrderByForOrderList(DefaultOrderService.OrderByOrderListEnum.PRESCRIBER_VALIDATION_DATE)
                        .setOrderDescForOrderList(false);

                OrderHelper.listOrderAndCalculatePrice(filterModel, request)
                        .onSuccess(orderUniversalModels -> {
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


    @Post("/orders/amount/structure/:idStructure")
    @ApiDoc("Get the total amount of orders")
    // To avoid errors of "request has already been" ex: https://pastebin.com/J1HUWtE9, we call the resource filter
    // later (new ValidatorInStructureRight().authorize)
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listOrdersAmountLicences(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.ORDERSAMOUNT, filter -> {
            UserHelper.getUserInfos(eb, request)
                    .compose(userInfos -> new ValidatorInStructureRight().authorize(request, null, userInfos))
                    .compose(asAccess -> {
                        if (asAccess) {
                            FilterModel filters = new FilterModel(filter);
                            filters.setIdsStructure(Collections.singletonList(request.getParam(Field.IDSTRUCTURE)))
                                    .setOrderByForOrderList(DefaultOrderService.OrderByOrderListEnum.PRESCRIBER_VALIDATION_DATE)
                                    .setOrderDescForOrderList(false);
                            return OrderHelper.listOrderAndCalculatePrice(filters, request);
                        } else {
                            return Future.failedFuture(new SecurityException());
                        }
                    })
                    .onSuccess(orderUniversalModels -> {
                        int nbItem = 0;
                        int nbLicence = 0;
                        int nbConsumableLicence = 0;
                        double priceCredit = 0.0;
                        double priceConsumableCredit = 0.0;
                        for (OrderUniversalModel orderUniversalModel : orderUniversalModels) {
                            nbItem += orderUniversalModel.getAmount();
                            switch (CreditTypeEnum.getValue(orderUniversalModel.getCampaign().getUseCredit(), CreditTypeEnum.NONE)) {
                                case LICENCES:
                                    nbLicence += orderUniversalModel.getAmount();
                                    break;
                                case CONSUMABLE_LICENCES:
                                    nbConsumableLicence += orderUniversalModel.getAmount();
                                    break;
                                case CREDITS:
                                    priceCredit += orderUniversalModel.getTotalPriceTTC();
                                    break;
                                case CONSUMABLE_CREDITS:
                                    priceConsumableCredit += orderUniversalModel.getTotalPriceTTC();
                                    break;
                            }
                        }

                        OrderSearchAmountModel orderSearchAmountModel = new OrderSearchAmountModel()
                                .setNbItem(nbItem)
                                .setNbLicence(nbLicence)
                                .setNbConsumableLicence(nbConsumableLicence)
                                .setPriceCredit(priceCredit)
                                .setPriceConsumableCredit(priceConsumableCredit);

                        Renders.render(request, orderSearchAmountModel.toJson());
                    })
                    .onFailure(error -> {
                        if (error instanceof SecurityException) {
                            Renders.unauthorized(request);
                            return;
                        }
                        Renders.renderError(request);
                        log.error(String.format("[CRRE@%s::listOrdersAmountLicences] Fail to Get the total amount: %s::%s",
                                this.getClass().getSimpleName(), error.getClass().getSimpleName(), error.getMessage()));
                    });
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

    @Post("/orders/exports")
    @ApiDoc("Export list of customer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ExportPrescriberRight.class)
    public void export(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + Field.EXPORTORDER, filterOrder -> {
            UserUtils.getUserInfos(eb, request, user -> {
                FilterModel filters = new FilterModel(filterOrder)
                        .setOrderByForOrderList(DefaultOrderService.OrderByOrderListEnum.PRESCRIBER_VALIDATION_DATE)
                        .setOrderDescForOrderList(false);
                OrderHelper.listOrderAndCalculatePrice(filters, request)
                        .onSuccess(orderUniversalModels -> {
                            request.response()
                                    .putHeader("Content-Type", "text/csv; charset=utf-8")
                                    .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                    .end(generateExport(request, orderUniversalModels));
                        })
                        .onFailure(error -> {
                            Renders.renderError(request);
                            log.error(String.format("[CRRE@%s::export] Fail to export %s", this.getClass().getSimpleName(), error.getMessage()));
                        });
            });
        });
    }

    private static String generateExport(HttpServerRequest request, List<OrderUniversalModel> orders) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(UTF8_BOM).append(getExportHeader(request));
        orders.forEach(orderUniversalModel -> {
            report.append(generateExportLine(request, orderUniversalModel));
            orderUniversalModel.getOffers().forEach(orderUniversalOfferModel -> report.append(generateExportLine(orderUniversalOfferModel)));
        });
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

    private static String generateExportLine(OrderUniversalOfferModel orderUniversalOfferModel) {
        return (orderUniversalOfferModel.getOrderUniversalModel().getPrescriberValidationDateFormat() != null ? orderUniversalOfferModel.getOrderUniversalModel().getPrescriberValidationDateFormat() : "") + ";" +
                (orderUniversalOfferModel.getOrderUniversalModel().getBasket().getName() != null ? orderUniversalOfferModel.getOrderUniversalModel().getBasket().getName() : "") + ";" +
                (orderUniversalOfferModel.getOrderUniversalModel().getEquipmentName() != null ? orderUniversalOfferModel.getOrderUniversalModel().getEquipmentName() : "") + ";" +
                (orderUniversalOfferModel.getOrderUniversalModel().getEquipmentKey() != null ? orderUniversalOfferModel.getOrderUniversalModel().getEquipmentKey() : "") + ";" +
                exportPriceComment(orderUniversalOfferModel) +
                ";"
                + "\n";
    }

    public static String exportPriceComment(OrderUniversalModel orderUniversalModel) {
        return (orderUniversalModel.getAmount() != null ? orderUniversalModel.getAmount() : "") + ";" +
                (orderUniversalModel.getEquipmentPriceht() != null ? Double.parseDouble(OrderUtils.df2.format(orderUniversalModel.getEquipmentPriceht())) : "") + ";" +
                (orderUniversalModel.getEquipmentTva5() != null ? orderUniversalModel.getEquipmentTva5() : "") + ";" +
                (orderUniversalModel.getEquipmentTva20() != null ? orderUniversalModel.getEquipmentTva20() : "") + ";" +
                (orderUniversalModel.getUnitedPriceTTC() != null ? convertPriceString(orderUniversalModel.getUnitedPriceTTC()) : "") + ";" +
                (orderUniversalModel.getTotalPriceHT() != null ? convertPriceString(orderUniversalModel.getTotalPriceHT()) : "") + ";" +
                (orderUniversalModel.getTotalPriceTTC() != null ? convertPriceString(orderUniversalModel.getTotalPriceTTC()) : "") + ";" +
                (orderUniversalModel.getComment() != null ? orderUniversalModel.getComment().replaceAll("\n", "").replaceAll("\r", "") : "") + ";";
    }

    public static String exportPriceComment(OrderUniversalOfferModel orderUniversalOfferModel) {
        return (orderUniversalOfferModel.getAmount() != null ? orderUniversalOfferModel.getAmount() : "") + ";" +
                ";" +
                ";" +
                ";" +
                (orderUniversalOfferModel.getUnitedPriceTTC() != null ? convertPriceString(orderUniversalOfferModel.getUnitedPriceTTC()) : "") + ";" +
                (orderUniversalOfferModel.getTotalPriceHT() != null ? convertPriceString(orderUniversalOfferModel.getTotalPriceHT()) : "") + ";" +
                (orderUniversalOfferModel.getTotalPriceTTC() != null ? convertPriceString(orderUniversalOfferModel.getTotalPriceTTC()) : "") + ";" +
                (orderUniversalOfferModel.getOrderUniversalModel().getEquipmentKey() != null ? orderUniversalOfferModel.getOrderUniversalModel().getEquipmentKey() : "") + ";";
    }

    /**
     * @deprecated Use {@link #exportPriceComment(OrderUniversalModel)}
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

    public static String exportStudents(OrderUniversalModel orderUniversalModel) {
        return (orderUniversalModel.getStudents().getSeconde() != null ? orderUniversalModel.getStudents().getSeconde() : "") + ";" +
                (orderUniversalModel.getStudents().getPremiere() != null ? orderUniversalModel.getStudents().getPremiere() : "") + ";" +
                (orderUniversalModel.getStudents().getTerminale() != null ? orderUniversalModel.getStudents().getTerminale() : "") + ";" +
                (orderUniversalModel.getStudents().getSecondetechno() != null ? orderUniversalModel.getStudents().getSecondetechno() : "") + ";" +
                (orderUniversalModel.getStudents().getPremieretechno() != null ? orderUniversalModel.getStudents().getPremieretechno() : "") + ";" +
                (orderUniversalModel.getStudents().getTerminaletechno() != null ? orderUniversalModel.getStudents().getTerminaletechno() : "") + ";" +
                (orderUniversalModel.getStudents().getSecondepro() != null ? orderUniversalModel.getStudents().getSecondepro() : "") + ";" +
                (orderUniversalModel.getStudents().getPremierepro() != null ? orderUniversalModel.getStudents().getPremierepro() : "") + ";" +
                (orderUniversalModel.getStudents().getTerminalepro() != null ? orderUniversalModel.getStudents().getTerminalepro() : "") + ";" +
                (orderUniversalModel.getStudents().getBma1() != null ? orderUniversalModel.getStudents().getBma1() : "") + ";" +
                (orderUniversalModel.getStudents().getBma2() != null ? orderUniversalModel.getStudents().getBma2() : "") + ";" +
                (orderUniversalModel.getStudents().getCap1() != null ? orderUniversalModel.getStudents().getCap1() : "") + ";" +
                (orderUniversalModel.getStudents().getCap2() != null ? orderUniversalModel.getStudents().getCap2() : "") + ";" +
                (orderUniversalModel.getStudents().getCap3() != null ? orderUniversalModel.getStudents().getCap3() : "");
    }

    /**
     * @deprecated Use {@link #exportStudents(OrderUniversalModel)}
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
                Integer amount = order.getInteger("amount", 0);
                if(amount > 0) {
                    orderService.updateAmount(id, amount, defaultResponseHandler(request));
                } else {
                    badRequest(request, "Amount is under 0");
                }
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
