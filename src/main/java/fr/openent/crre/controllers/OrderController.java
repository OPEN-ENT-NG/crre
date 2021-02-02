package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.export.ExportTypes;
import fr.openent.crre.helpers.ExportHelper;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.*;
import fr.openent.crre.service.impl.*;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.utils.OrderUtils.retrieveUaiNameStructure;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;


public class OrderController extends ControllerHelper {

    private final Storage storage;
    private final OrderService orderService;
    private final StructureService structureService;
    private final ExportService exportService;

    public static final String UTF8_BOM = "\uFEFF";

    public OrderController (Storage storage, Vertx vertx, JsonObject config, EventBus eb) {
        this.storage = storage;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.orderService = new DefaultOrderService(Crre.crreSchema, "order_client_equipment", emailSender);
        this.structureService = new DefaultStructureService(Crre.crreSchema);
        exportService = new DefaultExportServiceService(storage);
    }

    @Get("/orders/mine/:idCampaign/:idStructure")
    @ApiDoc("Get my list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listMyOrdersByCampaignByStructure(final HttpServerRequest request){
        try {
            UserUtils.getUserInfos(eb, request, user -> {
                Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
                String idStructure = request.params().get("idStructure");
                orderService.listOrder(idCampaign,idStructure, user, arrayResponseHandler(request));
            });
        }catch (ClassCastException e ){
            log.error("An error occured when casting campaign id ",e);
        }
    }

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void listOrders (final HttpServerRequest request){
        if (request.params().contains("status")) {
            final String status = request.params().get("status");
            if ("valid".equalsIgnoreCase(status)) {
                final JsonArray statusList = new fr.wseduc.webutils.collections.JsonArray().add(status).add("SENT").add("DONE");
                orderService.getOrdersGroupByValidationNumber(statusList, event -> {
                    if (event.isRight()) {
                        final JsonArray orders = event.right().getValue();
                        renderJson(request, orders);
                    } else {
                        badRequest(request);
                    }
                });
            } else {
                orderService.listOrder(status, arrayResponseHandler(request));
            }
        } else {
            badRequest(request);
        }
    }

    @Get("/order")
    @ApiDoc("Get the pdf of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrderPDF (final HttpServerRequest request) {
        final String orderNumber = request.params().get("bc_number");
        ExportHelper.makeExport(request,eb,exportService, Crre.ORDERSSENT,  Crre.PDF,ExportTypes.BC_AFTER_VALIDATION, "_BC_" + orderNumber);
    }

    @Get("/orders/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && request.params().get("q").trim() != "") {
                try {
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8");
                    int id_campaign = parseInt(request.getParam("id"));
                    orderService.searchName(query, equipments -> {
                        if(equipments.right().getValue().size() > 0) {
                            orderService.search(query, null, user, equipments.right().getValue(), id_campaign, arrayResponseHandler(request));
                        } else {
                            orderService.searchWithoutEquip(query, null, user, id_campaign, arrayResponseHandler(request));
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/filter")
    @ApiDoc("Filter order")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void filter(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
                try {
                    List<String> params = new ArrayList<>();
                    String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
                    if (request.params().contains("grade_name")) {
                        params = request.params().getAll("grade_name");
                    }

                    // Récupération de tout les filtres hors grade
                    JsonArray filters = new JsonArray();
                    int length = request.params().entries().size();
                    for (int i = 0; i < length; i++) {
                        if (!request.params().entries().get(i).getKey().equals("id") && !request.params().entries().get(i).getKey().equals("q") && !request.params().entries().get(i).getKey().equals("grade_name"))
                            filters.add(new JsonObject().put(request.params().entries().get(i).getKey(), request.params().entries().get(i).getValue()));
                    }
                    // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                    if (request.params().contains("q")) {
                        q = URLDecoder.decode(request.getParam("q"), "UTF-8");
                    }
                    int id_campaign = parseInt(request.getParam("id"));
                    String finalQ = q;
                    // Si nous avons des filtres de grade
                    if (params.size() > 0) {
                        Future<JsonArray> equipmentGradeFuture = Future.future();
                        Future<JsonArray> equipmentGradeAndQFuture = Future.future();

                        CompositeFuture.all(equipmentGradeFuture, equipmentGradeAndQFuture).setHandler(event -> {
                            if (event.succeeded()) {
                                JsonArray equipmentsGrade = equipmentGradeFuture.result(); // Tout les équipements correspondant aux grades
                                JsonArray equipmentsGradeAndQ = equipmentGradeAndQFuture.result(); // Tout les équipement correspondant aux grades et à la query
                                JsonArray allEquipments = new JsonArray();
                                allEquipments.add(equipmentsGrade);
                                allEquipments.add(equipmentsGradeAndQ);
                                // Si le tableau trouve des equipements, on recherche avec ou sans query sinon ou cherche sans equipement
                                if (equipmentsGrade.size() > 0) {
                                    if (request.params().contains("q")) {
                                        orderService.searchWithAll(finalQ, filters, user, allEquipments, id_campaign, arrayResponseHandler(request));
                                    } else {
                                        orderService.filter(filters, user, equipmentsGrade, id_campaign, arrayResponseHandler(request));
                                    }
                                } else {
                                    orderService.searchWithoutEquip(finalQ, filters, user, id_campaign, arrayResponseHandler(request));
                                }
                            }
                        });
                        orderService.filterGrade(params, null, handlerJsonArray(equipmentGradeFuture));
                        orderService.filterGrade(params, q, handlerJsonArray(equipmentGradeAndQFuture));
                    } else {
                        // Recherche avec les filtres autres que grade
                        orderService.searchName(finalQ, equipments -> {
                            if (equipments.right().getValue().size() > 0) {
                                orderService.search(finalQ, filters, user, equipments.right().getValue(), id_campaign, arrayResponseHandler(request));
                            } else {
                                orderService.searchWithoutEquip(finalQ, filters, user, id_campaign, arrayResponseHandler(request));
                            }
                        });
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
        });
    }

    @Get("/orders/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void export (final HttpServerRequest request){
        List<String> params = request.params().getAll("id");
        List<String> params2 = request.params().getAll("equipment_key");
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        List<Integer> idsEquipment = SqlQueryUtils.getIntegerIds(params2);
        Future<JsonArray> equipmentFuture = Future.future();
        Future<JsonArray> orderClientFuture = Future.future();

        CompositeFuture.all(equipmentFuture, orderClientFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray equipments = equipmentFuture.result();
                        JsonArray orderClients = orderClientFuture.result();
                        JsonObject orderMap;
                        JsonArray orders = new JsonArray();
                        JsonObject order, equipment;
                        boolean check;
                        int j;
                        for (int i = 0; i < orderClients.size(); i++) {
                            order = orderClients.getJsonObject(i);
                            check = true;
                            j = 0;
                            while(check && j < equipments.size()) {
                                if(equipments.getJsonObject(j).getInteger("id").equals(order.getInteger("equipment_key"))) {
                                    orderMap = new JsonObject();
                                    equipment = equipments.getJsonObject(j);
                                    orderMap.put("name", equipment.getString("name"));
                                    orderMap.put("id", order.getInteger("id"));
                                    DecimalFormat df = new DecimalFormat("0.00");
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
                                    String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                                    orderMap.put("creation_date", creation_date);
                                    orderMap.put("ean", equipment.getString("ean"));
                                    orderMap.put("status", order.getString("status"));
                                    orderMap.put("basket_name", order.getString("basket_name"));
                                    orderMap.put("comment", order.getString("comment"));
                                    orderMap.put("amount", order.getInteger("amount"));
                                    orderMap.put("total_ht", df.format(Float.parseFloat(order.getString("price")) * order.getInteger("amount")));
                                    orderMap.put("total_ttc_20", df.format(Float.parseFloat(order.getString("price")) * (1.2)  * order.getInteger("amount")));
                                    orderMap.put("total_ttc_5_5", df.format(Float.parseFloat(order.getString("price")) * (1.055)  * order.getInteger("amount")));
                                    orders.add(orderMap);
                                    check = false;
                                }
                                j++;
                            }
                        }
                        request.response()
                                .putHeader("Content-Type", "text/csv; charset=utf-8")
                                .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                .end(generateExport(request, orders));
                    }
        });

        getEquipment(idsEquipment, handlerJsonArray(equipmentFuture));
        getOrderEquipmentEquipment(idsOrders, handlerJsonArray(orderClientFuture));

    }

    private void getOrderEquipmentEquipment(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handlerJsonArray) {
        orderService.listExport(idsOrders, handlerJsonArray);
    }

    private void getEquipment(List<Integer> idsEquipment, Handler<Either<String, JsonArray>> handlerJsonArray) {
        searchByIds(idsEquipment, handlerJsonArray);
    }

    private static String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
            report.append(generateExportLine(request, logs.getJsonObject(i)));
        }
        return report.toString();
    }

    private static String getExportHeader (HttpServerRequest request) {
        return I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("basket", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("name.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("ean", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.ht", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.5", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment.20", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("csv.comment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("status", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    private static String generateExportLine (HttpServerRequest request, JsonObject log) {
        return  (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("basket_name") != null ? log.getString("basket_name") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getInteger("amount") != null ? log.getInteger("amount").toString() : "") + ";" +
                (log.getString("total_ht") != null ? log.getString("total_ht") : "") + ";" +
                (log.getString("total_ttc_5_5") != null ? log.getString("total_ttc_5_5") : "") + ";" +
                (log.getString("total_ttc_20") != null ? log.getString("total_ttc_20") : "") + ";" +
                (log.getString("comment") != null ? log.getString("comment") : "") + ";" +
                (log.getString("status") != null ? I18n.getInstance().translate(log.getString("status"), getHost(request), I18n.acceptLanguage(request)) : "")
                + "\n";
    }

    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void validateOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> UserUtils.getUserInfos(eb, request,
                        userInfos -> {
                            try {
                                List<String> params = new ArrayList<>();
                                for (Object id: orders.getJsonArray("ids") ) {
                                    params.add( id.toString());
                                }

                                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                                final String url = request.headers().get("Referer");
                                orderService.validateOrders(request, userInfos , ids, url,
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
    public void refuseOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> {
                            try {
                                List<String> params = new ArrayList<>();
                                for (Object id: orders.getJsonArray("ids") ) {
                                    params.add( id.toString());
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
    @ResourceFilter(ManagerRight.class)
    public void setOrdersInProgress(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", orders -> {
            final JsonArray ids = orders.getJsonArray("ids");
            orderService.setInProgress(ids, defaultResponseHandler(request));
        });
    }

    @Put("/order/:idOrder/comment")
    @ApiDoc("Update an order's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, order -> {
            if (!order.containsKey("comment")) {
                badRequest(request);
                return;
            }
            try {
                Integer id = Integer.parseInt(request.params().get("idOrder"));
                String comment = order.getString("comment");
                orderService.updateComment(id, comment, defaultResponseHandler(request));
            } catch (NumberFormatException e) {
                e.printStackTrace();
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

    public static String getReadableNumber(Double number) {
        DecimalFormat instance = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.FRENCH);
        DecimalFormatSymbols symbols = instance.getDecimalFormatSymbols();
        symbols.setCurrencySymbol("");
        instance.setDecimalFormatSymbols(symbols);
        return instance.format(number);
    }

    public static JsonArray formatOrders(JsonArray orders) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("priceLocale",
                    getReadableNumber(roundWith2Decimals(Double.parseDouble(order.getString("price")))));
            order.put("unitPriceTaxIncluded",
                    getReadableNumber(roundWith2Decimals(getTaxIncludedPrice(Double.parseDouble(order.getString("price")),
                            Double.parseDouble(order.getString("tax_amount"))))));
            order.put("unitPriceTaxIncludedLocale",
                    getReadableNumber(roundWith2Decimals(getTaxIncludedPrice(Double.parseDouble(order.getString("price")),
                            Double.parseDouble(order.getString("tax_amount"))))));
            order.put("totalPrice",
                    roundWith2Decimals(getTotalPrice(Double.parseDouble(order.getString("price")),
                            Double.parseDouble(order.getString("amount")))));
            order.put("totalPriceLocale",
                    getReadableNumber(roundWith2Decimals(Double.parseDouble(order.getDouble("totalPrice").toString()))));
            order.put("totalPriceTaxIncluded",
                    getReadableNumber(roundWith2Decimals(getTaxIncludedPrice(order.getDouble("totalPrice"),
                            Double.parseDouble(order.getString("tax_amount"))))));
        }
        return orders;
    }

    public static Double getTotalPrice(Double price, Double amount) {
        return price * amount;
    }

    public static Double getTaxIncludedPrice(Double price, Double taxAmount) {
        Double multiplier = taxAmount / 100 + 1;
        return roundWith2Decimals(price) * multiplier;
    }

    public static Double roundWith2Decimals(Double numberToRound) {
        BigDecimal bd = new BigDecimal(numberToRound);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @Get("/orders/export")
    @ApiDoc("Export list of waiting orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void exportCSVordersSelected(final HttpServerRequest request) {
        List<String> params = request.params().getAll("id");
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        if (!idsOrders.isEmpty()) {
            orderService.getExportCsvOrdersAdmin(idsOrders, ordersWithIdStructure -> {
                if (ordersWithIdStructure.isRight()) {
                    final JsonArray orders = ordersWithIdStructure.right().getValue();
                    JsonArray idsStructures = new fr.wseduc.webutils.collections.JsonArray();
                    for (int i = 0; i < orders.size(); i++) {
                        JsonObject order = orders.getJsonObject(i);
                        idsStructures.add(order.getString("idstructure"));
                    }
                    structureService.getStructureById(idsStructures, repStructures -> {
                        if (repStructures.isRight()) {
                            JsonArray structures = repStructures.right().getValue();

                            Map<String, String> structuresMap = retrieveUaiNameStructure(structures);
                            for (int i = 0; i < orders.size(); i++) {
                                JsonObject order = orders.getJsonObject(i);
                                order.put("uaiNameStructure", structuresMap.get(order.getString("idstructure")));
                            }

                            request.response()
                                    .putHeader("Content-Type", "text/csv; charset=utf-8")
                                    .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                    .end(generateExport(request, orders));

                        } else {
                            log.error("An error occured when collecting StructureById");
                            renderError(request);
                        }
                    });
                } else {
                    log.error("An error occurred when collecting ordersSqlwithIdStructure");
                    renderError(request);
                }
            });
        } else {
            badRequest(request);
        }
    }

    @Get("/order/:id/file/:fileId")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFile(HttpServerRequest request) {
        Integer orderId = Integer.parseInt(request.getParam("id"));
        String fileId = request.getParam("fileId");
        orderService.getFile(orderId, fileId, event -> {
            if (event.isRight()) {
                storage.sendFile(fileId, event.right().getValue().getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    @Put("/order/:idOrder")
    @ApiDoc("update status in orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateStatusOrder(final HttpServerRequest request) {
        final Integer idOrder = Integer.parseInt(request.params().get("idOrder"));
        RequestUtils.bodyToJson(request, statusEdit -> orderService.updateStatusOrder(idOrder, statusEdit, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOrder.toString(),
                statusEdit)));
    }

    @Get("/order/:idOrder")
    @ApiDoc("get an order")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOrder(HttpServerRequest request) {
        try {
            Integer orderId = Integer.parseInt(request.getParam("idOrder"));
            orderService.getOrder(orderId, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting order id", e);
        }

    }
}
