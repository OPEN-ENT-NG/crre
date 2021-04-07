package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.*;
import fr.openent.crre.service.ExportService;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultExportServiceService;
import fr.openent.crre.service.impl.DefaultOrderService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
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
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
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
        this.orderService = new DefaultOrderService(Crre.crreSchema, "order_client_equipment");
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
                List<String> ordersIds = request.params().getAll("order_id");
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                orderService.listOrder(idCampaign,idStructure, user, ordersIds, startDate, endDate, orders -> {
                    if (orders.isRight()) {
                        List<String> idEquipments = new ArrayList<>();
                        for(Object order : orders.right().getValue()){
                            String idEquipment = ((JsonObject)order).getString("equipment_key");
                            idEquipments.add(idEquipment);
                        }
                        searchByIds(idEquipments, equipments -> {
                            if(equipments.isRight()) {
                                for(Object order : orders.right().getValue()){
                                    JsonObject orderJson = ((JsonObject)order);
                                    String idEquipment = orderJson.getString("equipment_key");
                                    for(Object equipment : equipments.right().getValue()){
                                        JsonObject equipmentJson = ((JsonObject)equipment);
                                        if(idEquipment.equals(equipmentJson.getString("id"))){
                                            orderJson.put("price",getPriceTtc(equipmentJson).getDouble("priceTTC"));
                                            orderJson.put("name",equipmentJson.getString("titre"));
                                            orderJson.put("image",equipmentJson.getString("urlcouverture"));
                                        }
                                    }
                                }
                                final JsonArray finalResult = orders.right().getValue();
                                renderJson(request, finalResult);
                            }else{
                                badRequest(request);
                                log.error("Problem when catching equipments");
                            }
                        });
                    } else {
                        badRequest(request);
                        log.error("Problem when catching orders");
                    }
                });
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
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("status")) {
                final String status = request.params().get("status");
                String startDate = request.getParam("startDate");
                String endDate = request.getParam("endDate");
                Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                orderService.listOrder(status, page, user, startDate, endDate, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/users")
    @ApiDoc("Get the list of users who orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void listUsers (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("status")) {
                final String status = request.params().get("status");
                orderService.listUsers(status, user, arrayResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/orders/search")
    @ApiDoc("Search order through name")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void search(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (request.params().contains("q") && !request.params().get("q").trim().isEmpty()) {
                try {
                    String query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                    Integer id_campaign = null;
                    if(request.getParam("id") != null) {
                        id_campaign = parseInt(request.getParam("id"));
                    }
                    Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                    Integer finalId_campaign = id_campaign;
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");
                    orderService.searchName(query, equipments -> {
                        if(equipments.right().getValue().size() > 0) {
                            orderService.search(query, null, user, equipments.right().getValue(), finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
                        } else {
                            orderService.searchWithoutEquip(query, null, user, finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
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
                    Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
                    List<String> params = new ArrayList<>();
                    String q = ""; // Query pour chercher sur le nom du panier, le nom de la ressource ou le nom de l'enseignant
                    if (request.params().contains("niveaux.libelle")) {
                        params = request.params().getAll("niveaux.libelle");
                    }
                    String startDate = request.getParam("startDate");
                    String endDate = request.getParam("endDate");

                    // Récupération de tout les filtres hors grade
                    JsonArray filters = new JsonArray();
                    int length = request.params().entries().size();
                    for (int i = 0; i < length; i++) {
                        String key = request.params().entries().get(i).getKey();
                        if (!key.equals("id") && !key.equals("q") && !key.equals("niveaux.libelle") && !key.equals("page") &&
                            !key.equals("startDate") && !key.equals("endDate")) {
                            filters.add(new JsonObject().put(request.params().entries().get(i).getKey(), request.params().entries().get(i).getValue()));
                        }
                    }
                    // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
                    if (request.params().contains("q")) {
                        q = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                    }
                    Integer id_campaign = null;
                    if(request.getParam("id") != null) {
                        id_campaign = parseInt(request.getParam("id"));
                    }
                    String finalQ = q;
                    // Si nous avons des filtres de grade
                    if (params.size() > 0) {
                        Future<JsonArray> equipmentGradeFuture = Future.future();
                        Future<JsonArray> equipmentGradeAndQFuture = Future.future();

                        Integer finalId_campaign = id_campaign;
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
                                        orderService.searchWithAll(finalQ, filters, user, allEquipments, finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                    } else {
                                        orderService.filter(filters, user, equipmentsGrade, finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                    }
                                } else {
                                    orderService.searchWithoutEquip(finalQ, filters, user, finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
                                }
                            }
                        });
                        orderService.filterGrade(params, null, handlerJsonArray(equipmentGradeFuture));
                        orderService.filterGrade(params, q, handlerJsonArray(equipmentGradeAndQFuture));
                    } else {
                        // Recherche avec les filtres autres que grade
                        Integer finalId_campaign = id_campaign;
                        orderService.searchName(finalQ, equipments -> {
                            if (equipments.right().getValue().size() > 0) {
                                orderService.search(finalQ, filters, user, equipments.right().getValue(), finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
                            } else {
                                orderService.searchWithoutEquip(finalQ, filters, user, finalId_campaign, startDate, endDate, page, arrayResponseHandler(request));
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
        List<String> idsEquipment = request.params().getAll("equipment_key");
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
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
                                if(equipments.getJsonObject(j).getString("ean").equals(order.getString("equipment_key"))) {
                                    orderMap = new JsonObject();
                                    equipment = equipments.getJsonObject(j);
                                    orderMap.put("name", equipment.getString("titre"));
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
                                    JsonObject priceDetails = getPriceTtc(equipment);
                                    DecimalFormat df2 = new DecimalFormat("#.##");
                                    double priceTTC = priceDetails.getDouble("priceTTC") * order.getInteger("amount");
                                    double priceHT = priceDetails.getDouble("prixht") * order.getInteger("amount");
                                    orderMap.put("priceht", priceDetails.getDouble("prixht"));
                                    orderMap.put("tva5",priceDetails.getDouble("partTVA5"));
                                    orderMap.put("tva20",priceDetails.getDouble("partTVA20"));
                                    orderMap.put("unitedPriceTTC", priceDetails.getDouble("priceTTC"));
                                    orderMap.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
                                    orderMap.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
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

    private void getEquipment(List<String> idsEquipment, Handler<Either<String, JsonArray>> handlerJsonArray) {
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

    private static String generateExportLine (HttpServerRequest request, JsonObject log) {
        return  (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("basket_name") != null ? log.getString("basket_name") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getInteger("amount") != null ? log.getInteger("amount").toString() : "") + ";" +
                (log.getDouble("priceht") != null ? log.getDouble("priceht").toString() : "") + ";" +
                (log.getDouble("tva5") != null ? log.getDouble("tva5").toString() : "") + ";" +
                (log.getDouble("tva20") != null ? log.getDouble("tva20").toString() : "") + ";" +
                (log.getDouble("unitedPriceTTC") != null ? log.getDouble("unitedPriceTTC").toString() : "") + ";" +
                (log.getDouble("totalPriceHT") != null ? log.getDouble("totalPriceHT").toString() : "") + ";" +
                (log.getDouble("totalPriceTTC") != null ? log.getDouble("totalPriceTTC").toString() : "") + ";" +
                (log.getString("comment") != null ? log.getString("comment") : "") + ";" +
                (log.getString("status") != null ? I18n.getInstance().translate(log.getString("status"), getHost(request), I18n.acceptLanguage(request)) : "")
                + "\n";
    }

    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
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
    @ResourceFilter(AdministratorRight.class)
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
    @ResourceFilter(AdministratorRight.class)
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
