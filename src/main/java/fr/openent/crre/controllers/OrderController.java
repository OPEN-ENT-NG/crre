package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.openent.crre.utils.OrderUtils;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.plainTextSearchName;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.*;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;


public class OrderController extends ControllerHelper {

    private final OrderService orderService;
    private final OrderRegionService orderRegionService;
    private final DefaultStructureService structureService;

    public static final String UTF8_BOM = "\uFEFF";

    public OrderController() {
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.orderService = new DefaultOrderService(Crre.crreSchema, "order_client_equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema, null);
    }

    @Get("/orders/mine/:idCampaign/:idStructure")
    @ApiDoc("Get my list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listMyOrdersByCampaignByStructure(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user -> {
                Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
                String idStructure = request.params().get("idStructure");
                List<String> ordersIds = request.params().getAll("order_id");
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                Boolean old = Boolean.valueOf(request.getParam("old"));

                orderService.listOrder(idCampaign, idStructure, user, ordersIds, startDate, endDate, old, orders -> {
                    if (orders.isRight()) {
                        if (!old) {
                            List<String> idEquipments = new ArrayList<>();
                            for (Object order : orders.right().getValue()) {
                                String idEquipment = ((JsonObject) order).getString("equipment_key");
                                idEquipments.add(idEquipment);
                            }
                            searchByIds(idEquipments, equipments -> {
                                if (equipments.isRight()) {
                                    for (Object order : orders.right().getValue()) {
                                        JsonObject orderJson = ((JsonObject) order);
                                        String idEquipment = orderJson.getString("equipment_key");
                                        JsonArray equipmentsArray = equipments.right().getValue();
                                        if (equipmentsArray.size() > 0) {
                                            for (int i = 0; i < equipmentsArray.size(); i++) {
                                                JsonObject equipment = equipmentsArray.getJsonObject(i);
                                                if (idEquipment.equals(equipment.getString("id"))) {
                                                    orderJson.put("price", getPriceTtc(equipment).getDouble("priceTTC"));
                                                    orderJson.put("name", equipment.getString("titre"));
                                                    orderJson.put("image", equipment.getString("urlcouverture"));
                                                    break;
                                                } else if (equipmentsArray.size() - 1 == i) {
                                                    orderJson.put("price", 0.0);
                                                    orderJson.put("name", "Manuel introuvable dans le catalogue");
                                                    orderJson.put("image", "/crre/public/img/pages-default.png");
                                                }
                                            }
                                        } else {
                                            orderJson.put("price", 0.0);
                                            orderJson.put("name", "Manuel introuvable dans le catalogue");
                                            orderJson.put("image", "/crre/public/img/pages-default.png");
                                        }
                                    }
                                    final JsonArray finalResult = orders.right().getValue();
                                    renderJson(request, finalResult);
                                } else {
                                    badRequest(request);
                                    log.error("Problem when catching equipments");
                                }
                            });
                        } else {
                            renderJson(request, orders.right().getValue());
                        }
                    } else {
                        badRequest(request);
                        log.error("Problem when catching orders");
                    }
                });
            });
        } catch (ClassCastException e) {
            log.error("An error occured when casting campaign id ", e);
        }
    }

    @Get("/orders/old/mine/:idCampaign/:idStructure")
    @ApiDoc("Get my list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listMyOrdersOldByCampaignByStructure(final HttpServerRequest request) {
        try {
            UserUtils.getUserInfos(eb, request, user -> {
                Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
                String idStructure = request.params().get("idStructure");
                List<String> ordersIds = request.params().getAll("order_id");
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                Boolean old = Boolean.valueOf(request.getParam("old"));

                orderService.listOrder(idCampaign, idStructure, user, ordersIds, startDate, endDate, old, orders -> {
                    if (orders.isRight()) {
                        final JsonArray finalResult = orders.right().getValue();
                        renderJson(request, finalResult);
                    } else {
                        badRequest(request);
                        log.error("Problem when catching orders");
                    }
                });
            });
        } catch (ClassCastException e) {
            log.error("An error occured when casting campaign id ", e);
        }
    }

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void listOrders(final HttpServerRequest request) {
        if (request.params().contains("status")) {
            final String status = request.params().get("status");
            String idStructure = request.getParam("idStructure");
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            Integer page = OrderUtils.formatPage(request);
            orderService.listOrder(status, idStructure, page, startDate, endDate, arrayResponseHandler(request));
        } else {
            badRequest(request);
        }
    }


    @Get("/orders/amount")
    @ApiDoc("Get the total amount of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void listOrdersAmountLicences(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("status")) {
                final String status = request.params().get("status");
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
                    if (!key.equals("id") && !key.equals("q") && !key.equals("idStructure") && !key.equals("page") &&
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
                        result.put("licence", amount);
                        amount = 0;
                        if (getOrderAmountConsumable.future().result().getString("nb_licences") != null) {
                            amount = Integer.parseInt(getOrderAmountConsumable.future().result().getString("nb_licences"));
                        }
                        result.put("consumable_licence", amount);
                        JsonArray totalAmount = getTotalAmount.future().result();
                        JsonArray order_credit = getOrderCredit.future().result();
                        if (order_credit.size() > 0) {
                            List<String> idsEquipment = new ArrayList<>();
                            List<Long> idsOrderFiltered = new ArrayList<>();
                            int total_amount = 0;
                            for (int i = 0; i < order_credit.size(); i++) {
                                idsEquipment.add(order_credit.getJsonObject(i).getString("equipment_key"));
                            }
                            for (int i = 0; i < totalAmount.size(); i++) {
                                idsOrderFiltered.add(totalAmount.getJsonObject(i).getLong("id"));
                                total_amount += totalAmount.getJsonObject(i).getLong("amount");
                            }
                            int finalTotal_amount = total_amount;
                            searchByIds(idsEquipment, equipments -> {
                                if (equipments.isRight()) {
                                    JsonArray equipmentsArray = equipments.right().getValue();
                                    double total = 0;
                                    double totalFiltered = 0;
                                    double total_consumable = 0;
                                    for (int i = 0; i < order_credit.size(); i++) {
                                        JsonObject order = order_credit.getJsonObject(i);
                                        String idEquipment = order.getString("equipment_key");
                                        Long idOrder = order.getLong("id");
                                        String credit = order.getString("use_credit");
                                        if (equipmentsArray.size() > 0) {
                                            for (int j = 0; j < equipmentsArray.size(); j++) {
                                                JsonObject equipment = equipmentsArray.getJsonObject(j);
                                                if (idEquipment.equals(equipment.getString("id"))) {
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
                                }
                            });
                        } else {
                            renderJson(request, result);
                        }
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
    @ResourceFilter(ValidatorRight.class)
    public void listUsers(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("status")) {
                final String status = request.params().get("status");
                final String idStructure = request.params().get("idStructure");
                orderService.listUsers(status, idStructure, user, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/search_filter")
    @ApiDoc("Filter order")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void filter(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            try {
                Integer page = OrderUtils.formatPage(request);
                String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                String idStructure = request.getParam("idStructure");

                // Récupération de tout les filtres
                JsonArray filters = new JsonArray();
                boolean exist;
                int length = request.params().entries().size();
                for (int i = 0; i < length; i++) {
                    String key = request.params().entries().get(i).getKey();
                    exist = false;
                    if (!key.equals("id") && !key.equals("q") && !key.equals("idStructure") && !key.equals("page") &&
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
                // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                if (request.params().contains("q")) {
                    q = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                }
                Integer id_campaign = null;
                if (request.getParam("id") != null) {
                    id_campaign = parseInt(request.getParam("id"));
                }
                String finalQ = q;
                Integer finalId_campaign = id_campaign;
                Integer finalPage = page;
                plainTextSearchName(finalQ, equipments -> {
                    orderService.search(finalQ, filters, idStructure, equipments.right().getValue(), finalId_campaign, startDate, endDate, finalPage, arrayResponseHandler(request));
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }

    @Get("/orders/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void export(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request,
                userInfos -> {
                    List<String> params = request.params().getAll("id");
                    String idCampaign = request.params().contains("idCampaign") ? request.params().get("idCampaign") : null;
                    String idStructure = request.params().contains("idStructure") ? request.params().get("idStructure") : null;
                    String statut = request.params().contains("statut") ? request.params().get("statut") : null;
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    List<Integer> idsOrders = params == null ? new ArrayList<>() : SqlQueryUtils.getIntegerIds(params);
                    getOrderEquipment(idsOrders, userInfos, idStructure, idCampaign, statut, startDate, endDate, event -> {
                        if (event.isRight()) {
                            JsonArray orderClients = event.right().getValue();
                            List<String> idsEquipment = new ArrayList<>();
                            for (int j = 0; j < orderClients.size(); j++) {
                                idsEquipment.add(orderClients.getJsonObject(j).getString("equipment_key"));
                            }
                            getEquipment(idsEquipment, event1 -> {
                                JsonObject orderMap;
                                JsonArray equipments = event1.right().getValue();
                                JsonArray orders = new JsonArray();
                                JsonObject order, equipment;
                                boolean check;
                                int j;
                                for (int i = 0; i < orderClients.size(); i++) {
                                    order = orderClients.getJsonObject(i);
                                    check = true;
                                    j = 0;
                                    while (check && j < equipments.size()) {
                                        if (equipments.getJsonObject(j).getString("ean").equals(order.getString("equipment_key"))) {
                                            orderMap = new JsonObject();
                                            equipment = equipments.getJsonObject(j);
                                            orderMap.put("name", equipment.getString("titre"));
                                            orderMap.put("ean", equipment.getString("ean"));
                                            setOrderMap(orders, orderMap, order, equipment, false);
                                            check = false;
                                        }
                                        j++;
                                    }
                                }
                                request.response()
                                        .putHeader("Content-Type", "text/csv; charset=utf-8")
                                        .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                        .end(generateExport(request, orders));
                            });
                        }
                    });
                });
    }

    @Get("/orders/old/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void exportOld(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request,
                userInfos -> {
                    List<String> params = request.params().getAll("id");
                    String idCampaign = request.params().contains("idCampaign") ? request.params().get("idCampaign") : null;
                    String idStructure = request.params().contains("idStructure") ? request.params().get("idStructure") : null;
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    List<Integer> idsOrders = params == null ? new ArrayList<>() : SqlQueryUtils.getIntegerIds(params);
                    getOrderEquipmentOld(idsOrders, userInfos, idStructure, idCampaign, startDate, endDate, event -> {
                        if (event.isRight()) {
                            JsonArray orders = new JsonArray();
                            JsonArray orderClients = event.right().getValue();
                            JsonObject orderMap;
                            JsonObject order;
                            for (int i = 0; i < orderClients.size(); i++) {
                                order = orderClients.getJsonObject(i);
                                orderMap = new JsonObject();
                                orderMap.put("name", order.getString("equipment_name"));
                                orderMap.put("ean", order.getString("equipment_key"));
                                setOrderMap(orders, orderMap, order, null, true);
                            }
                            request.response()
                                    .putHeader("Content-Type", "text/csv; charset=utf-8")
                                    .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                    .end(generateExport(request, orders));
                        }
                    });
                });
    }

    private void setOrderMap(JsonArray orders, JsonObject orderMap, JsonObject order, JsonObject equipment,
                             boolean old) {
        orderMap.put("id", order.getInteger("id"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
        String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
        orderMap.put("creation_date", creation_date);
        orderMap.put("status", order.getString("status"));
        orderMap.put("basket_name", order.getString("basket_name"));
        orderMap.put("comment", order.getString("comment"));
        orderMap.put("amount", order.getInteger("amount"));
        dealWithPriceTTC_HT(orderMap, equipment, order, old);
        orders.add(orderMap);
    }

    private void getOrderEquipment
            (List<Integer> idsOrders, UserInfos user, String idStructure, String idCampaign, String statut, String startDate, String endDate, Handler<Either<String, JsonArray>> handlerJsonArray) {
        orderService.listExport(idsOrders, user, idStructure, idCampaign, statut, startDate, endDate, false, handlerJsonArray);
    }

    private void getOrderEquipmentOld
            (List<Integer> idsOrders, UserInfos user, String idStructure, String idCampaign, String startDate, String endDate, Handler<Either<String, JsonArray>> handlerJsonArray) {
        orderService.listExport(idsOrders, user, idStructure, idCampaign, null, startDate, endDate, true, handlerJsonArray);
    }

    private void getEquipment
            (List<String> idsEquipment, Handler<Either<String, JsonArray>> handlerJsonArray) {
        searchByIds(idsEquipment, handlerJsonArray);
    }

    private static String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
            report.append(generateExportLine(request, logs.getJsonObject(i)));
        }
        return report.toString();
    }

    private static String getExportHeader(HttpServerRequest request) {
        return I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("basket", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("name.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("ean", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.unit.price.ht", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.5", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.20", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.unit.price.ttc", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountHT", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.amountTTC", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("csv.comment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("status", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    private static String generateExportLine(HttpServerRequest request, JsonObject log) {
        return (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("basket_name") != null ? log.getString("basket_name") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                exportPriceComment(log) + ";" +
                (log.getString("status") != null ? I18n.getInstance().translate(log.getString("status"), getHost(request), I18n.acceptLanguage(request)) : "")
                + "\n";
    }

    public static String exportPriceComment(JsonObject log) {
        return (log.getInteger("amount") != null ? log.getInteger("amount").toString() : "") + ";" +
                (log.getDouble("priceht") != null ? log.getDouble("priceht").toString() : "") + ";" +
                (log.getDouble("tva5") != null ? log.getDouble("tva5").toString() : "") + ";" +
                (log.getDouble("tva20") != null ? log.getDouble("tva20").toString() : "") + ";" +
                (log.getDouble("unitedPriceTTC") != null ? convertPriceString(log.getDouble("unitedPriceTTC")) : "") + ";" +
                (log.getDouble("totalPriceHT") != null ? convertPriceString(log.getDouble("totalPriceHT")) : "") + ";" +
                (log.getDouble("totalPriceTTC") != null ? convertPriceString(log.getDouble("totalPriceTTC")) : "") + ";" +
                (log.getString("comment") != null ? log.getString("comment") : "");
    }

    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void validateOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> UserUtils.getUserInfos(eb, request,
                        userInfos -> {
                            try {
                                List<String> params = new ArrayList<>();
                                for (Object id : orders.getJsonArray("ids")) {
                                    params.add(id.toString());
                                }

                                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                                orderService.validateOrders(ids,
                                        Logging.defaultResponsesHandler(eb,
                                                request,
                                                Contexts.ORDER.toString(),
                                                Actions.UPDATE.toString(),
                                                params,
                                                null));
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));

    }

    @Put("/orders/refused")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void refuseOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> {
                    try {
                        List<String> params = new ArrayList<>();
                        for (Object id : orders.getJsonArray("ids")) {
                            params.add(id.toString());
                        }

                        List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                        orderService.rejectOrders(ids,
                                Logging.defaultResponsesHandler(eb,
                                        request,
                                        Contexts.ORDER.toString(),
                                        Actions.UPDATE.toString(),
                                        params,
                                        null));

                    } catch (ClassCastException e) {
                        log.error("An error occurred when casting order id", e);
                    }
                });
    }

    @Put("/orders/inprogress")
    @ApiDoc("send orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void setOrdersInProgress(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", orders -> {
            final JsonArray ids = orders.getJsonArray("ids");
            orderService.setInProgress(ids, defaultResponseHandler(request));
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
            }
        });
    }
}
