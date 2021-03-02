package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.PrescriptorRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.PurseService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.*;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.controllers.LogController.UTF8_BOM;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;
    private final PurseService purseService;
    private final StructureService structureService;
    private final EmailSendService emailSender;
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OrderRegionController(Vertx vertx, JsonObject config) {
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.emailSender = new EmailSendService(emailSender);
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.purseService = new DefaultPurseService();
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Post("/region/orders/")
    @ApiDoc("Create orders for region")
    @SecuredAction(Crre.VALIDATOR_RIGHT)
    @ResourceFilter(ValidatorRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try{
            UserUtils.getUserInfos(eb, request, user ->
                    RequestUtils.bodyToJson(request, orders -> {
                        if (!orders.isEmpty()) {
                            JsonArray ordersList = orders.getJsonArray("orders");
                            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                            orderRegionService.getLastProject(user, lastProject -> {
                                if(lastProject.isRight()) {
                                    String last = lastProject.right().getValue().getString("title");
                                    String title = "Commande_" + date;
                                    if(last != null) {
                                        if(title.equals(last.substring(0, last.length() - 2))) {
                                            title = title + "_" + (Integer.parseInt(last.substring(last.length() - 1)) + 1);
                                        } else {
                                            title += "_1";
                                        }
                                    } else {
                                        title += "_1";
                                    }

                                    String finalTitle = title;
                                    orderRegionService.createProject(title, idProject -> {
                                        if(idProject.isRight()){
                                            Integer idProjectRight = idProject.right().getValue().getInteger("id");
                                            Logging.insert(eb,
                                                    request,
                                                    null,
                                                    Actions.CREATE.toString(),
                                                    idProjectRight.toString(),
                                                    new JsonObject().put("id", idProjectRight).put("title", finalTitle));
                                            for(int i = 0 ; i<ordersList.size() ; i++){
                                                List<Future> futures = new ArrayList<>();
                                                JsonObject newOrder = ordersList.getJsonObject(i);
                                                Future<JsonObject> createOrdersRegionFuture = Future.future();
                                                futures.add(createOrdersRegionFuture);
                                                updatePurseLicence(futures, newOrder,"-");
                                                orderRegionService.createOrdersRegion(newOrder, user, idProjectRight,
                                                        handlerJsonObject(createOrdersRegionFuture));
                                                int finalI = i;
                                                CompositeFuture.all(futures).setHandler(event -> {
                                                    if (event.succeeded()) {
                                                        Number idReturning = createOrdersRegionFuture.result().getInteger("id");
                                                        Logging.insert(eb,
                                                                request,
                                                                Contexts.ORDERREGION.toString(),
                                                                Actions.CREATE.toString(),
                                                                idReturning.toString(),
                                                                new JsonObject().put("order region", newOrder));
                                                        if(finalI == ordersList.size()-1){
                                                            request.response().setStatusCode(201).end();
                                                        }
                                                    } else {
                                                        LOGGER.error("An error when you want get id after create order region ",
                                                                event.cause());
                                                        request.response().setStatusCode(400).end();
                                                    }
                                                });
                                            }
                                        } else {
                                            LOGGER.error("An error when you want get id after create project " + idProject.left());
                                            request.response().setStatusCode(400).end();
                                        }
                                    });
                                }
                            });
                        }
                    }));

        } catch( Exception e){
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    };

    @Delete("/region/:id/order")
    @ApiDoc("delete order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void deleteOrderRegion(final HttpServerRequest request) {
        int idRegion = Integer.parseInt(request.getParam("id"));
        orderRegionService.deleteOneOrderRegion(idRegion, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDERREGION.toString(),
                Actions.DELETE.toString(),
                Integer.toString(idRegion),
                new JsonObject().put("idRegion", idRegion)));
    }

    @Get("/orderRegion/:id/order")
    @ApiDoc("get order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getOneOrder(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        orderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
    }

    @Get("/orderRegion/orders")
    @ApiDoc("get all orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllOrder(HttpServerRequest request) {
        orderRegionService.getAllOrderRegion(arrayResponseHandler(request));
    }

    @Get("/orderRegion/orders/:id")
    @ApiDoc("get all orders by project ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllOrderByProject(HttpServerRequest request) {
        int idProject = Integer.parseInt(request.getParam("id"));
        orderRegionService.getAllOrderRegionByProject(idProject, arrayResponseHandler(request));
    }

    @Get("/orderRegion/projects")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
            orderRegionService.getAllProjects(user, page, arrayResponseHandler(request));
        });
    }

    private void getOrders (String query, JsonArray filters, UserInfos user, Integer page, HttpServerRequest request) {
        try {
            // On verifie si on a bien une query, si oui on la décode pour éviter les problèmes d'accents
            if (request.params().contains("q")) {
                query = URLDecoder.decode(request.getParam("q"), "UTF-8");
            }
            HashMap<String, ArrayList<String>> params = new HashMap<String, ArrayList<String>>();
            if (request.params().contains("editeur")) {
                params.put("editeur", new ArrayList<>(request.params().getAll("editeur")));
            }
            if (request.params().contains("distributeur")) {
                params.put("distributeur", new ArrayList<>(request.params().getAll("distributeur")));
            }
            if (request.params().contains("_index")) {
                params.put("_index", new ArrayList<>(request.params().getAll("_index")));
            }

            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            int length = request.params().entries().size();
            for (int i = 0; i < length; i++) {
                if (!request.params().entries().get(i).getKey().equals("q") &&
                        !request.params().entries().get(i).getKey().equals("startDate") &&
                        !request.params().entries().get(i).getKey().equals("distributeur") &&
                        !request.params().entries().get(i).getKey().equals("editeur") &&
                        !request.params().entries().get(i).getKey().equals("_index") &&
                        !request.params().entries().get(i).getKey().equals("type") &&
                        !request.params().entries().get(i).getKey().equals("endDate") &&
                        !request.params().entries().get(i).getKey().equals("page"))
                    filters.add(new JsonObject().put(request.params().entries().get(i).getKey(),
                            request.params().entries().get(i).getValue()));
            }
            String finalQuery = query;
            if (params.size() > 0) {
                Future<JsonArray> equipmentFilterFuture = Future.future();
                Future<JsonArray> equipmentFilterAndQFuture = Future.future();

                CompositeFuture.all(equipmentFilterFuture, equipmentFilterAndQFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray equipmentsGrade = equipmentFilterFuture.result(); // Tout les équipements correspondant aux grades
                        JsonArray equipmentsGradeAndQ = equipmentFilterAndQFuture.result();
                        JsonArray allEquipments = new JsonArray();
                        allEquipments.add(equipmentsGrade);
                        allEquipments.add(equipmentsGradeAndQ);
                        if (request.params().contains("q")) {
                            orderRegionService.filterSearch(user, allEquipments, finalQuery, startDate,
                                    endDate, filters, page, arrayResponseHandler(request));
                        } else {
                            orderRegionService.filter_only(user, equipmentsGrade, startDate,
                                    endDate, filters, page, arrayResponseHandler(request));
                        }
                    }
                });
                orderRegionService.filterES(params, null, handlerJsonArray(equipmentFilterFuture));
                orderRegionService.filterES(params, query, handlerJsonArray(equipmentFilterAndQFuture));
            } else {
                orderRegionService.searchName(query, equipments -> {
                    if (equipments.right().getValue().size() > 0) {
                        orderRegionService.search(user, equipments.right().getValue(), finalQuery, startDate,
                                endDate, filters, page, arrayResponseHandler(request));
                    } else {
                        orderRegionService.filterSearchWithoutEquip(user, finalQuery, startDate, endDate, filters, page,
                                arrayResponseHandler(request));
                    }
                });
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Get("/ordersRegion/projects/search_filter")
    @ApiDoc("get all projects search and filter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getProjectsDateSearch(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String query = "";
            JsonArray filters = new JsonArray();
            Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
            if (request.params().contains("type")) {
                Future<JsonArray> listeUAIFuture = Future.future();
                listeUAIFuture.setHandler(event -> {
                    if(event.succeeded()) {
                        JsonArray listeUAI = listeUAIFuture.result();
                        filters.add(new JsonObject().put("id_structure", listeUAI));
                        getOrders(query, filters, user, page, request);
                    }
                });
                structureService.getStructuresByType(request.getParam("type"), handlerJsonArray(listeUAIFuture));
            } else {
                getOrders(query, filters, user, page, request);
            }
        });
    }

    @Get("/ordersRegion/orders")
    @ApiDoc("get all orders of each project")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOrdersByProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            List<String> projectIds = request.params().getAll("project_id");
            List<Future> futures = new ArrayList<>();
            for(String id : projectIds){
                Future<JsonArray> projectIdFuture = Future.future();
                futures.add(projectIdFuture);
                int idProject = Integer.parseInt(id);
                orderRegionService.getAllOrderRegionByProject(idProject, handlerJsonArray(projectIdFuture));
            }
            getCompositeFutureAllOrderRegionByProject(request, futures);
        });
    }

    private void getCompositeFutureAllOrderRegionByProject(HttpServerRequest request, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                List<JsonArray> resultsList = event.result().list();
                List<String> listIdsEquipment = new ArrayList<>();
                for (JsonArray orders : resultsList) {
                    for (Object order : orders) {
                        listIdsEquipment.add(((JsonObject) order).getString("equipment_key"));
                    }
                }
                getSearchByIds(request, resultsList, listIdsEquipment);
            } else {
                log.error(event.cause());
                badRequest(request);
            }
        });
    }

    private void getSearchByIds(HttpServerRequest request, List<JsonArray> resultsList, List<String> listIdsEquipment) {
        searchByIds(listIdsEquipment, equipments -> {
            if (equipments.isRight()) {
                JsonArray finalResult = new JsonArray();
                for (JsonArray orders : resultsList) {
                    finalResult.add(orders);
                    for (Object order : orders) {
                        JsonObject orderJson = (JsonObject) order;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date"), formatter);
                        String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                        orderJson.put("creation_date",creation_date);
                        String idEquipment = orderJson.getString("equipment_key");
                        for (Object equipment : equipments.right().getValue()) {
                            JsonObject equipmentJson = (JsonObject) equipment;
                            if (idEquipment.equals(equipmentJson.getString("id"))) {
                                JsonObject priceDetails = getPriceTtc(equipmentJson);
                                double price = priceDetails.getDouble("priceTTC") * orderJson.getInteger("amount");
                                orderJson.put("price", price);
                                orderJson.put("name", equipmentJson.getString("titre"));
                                orderJson.put("image", equipmentJson.getString("urlcouverture"));
                                orderJson.put("ean", equipmentJson.getString("ean"));
                                orderJson.put("_index", equipmentJson.getString("type"));
                                orderJson.put("editeur", equipmentJson.getString("editeur"));
                                orderJson.put("distributeur", equipmentJson.getString("distributeur"));
                            }
                        }
                    }
                }
                renderJson(request, finalResult);
            } else {
                log.error(equipments.left());
                badRequest(request);
            }
        });
    }

    @Put("/region/orders/:status")
    @ApiDoc("update region orders with status")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void validateOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds",
                orders -> UserUtils.getUserInfos(eb, request,
                        userInfos -> {
                            try {
                                String status = request.getParam("status");
                                List<String> params = new ArrayList<>();
                                for (Object id: orders.getJsonArray("ids") ) {
                                    params.add( id.toString());
                                }
                                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                                String justification = orders.getString("justification");
                                JsonArray ordersList = orders.getJsonArray("orders");
                                List<Future> futures = new ArrayList<>();
                                if(status.equals("rejected")){
                                    for(int i = 0 ; i<ordersList.size() ; i++) {
                                        JsonObject newOrder = ordersList.getJsonObject(i);
                                        if(!newOrder.getString("status").equals("REJECTED"))
                                            updatePurseLicence(futures, newOrder,"+");
                                    }
                                    CompositeFuture.all(futures).setHandler(event -> {
                                        if (event.succeeded()) {
                                            orderRegionService.updateOrders(ids,status,justification,
                                                    Logging.defaultResponsesHandler(eb,
                                                            request,
                                                            Contexts.ORDERREGION.toString(),
                                                            Actions.UPDATE.toString(),
                                                            params,
                                                            null));
                                        } else {
                                            LOGGER.error("An error when you want get id after create order region ", event.cause());
                                            request.response().setStatusCode(400).end();
                                        }
                                    });
                                }else{
                                    orderRegionService.updateOrders(ids,status,justification,
                                            Logging.defaultResponsesHandler(eb,
                                                    request,
                                                    Contexts.ORDERREGION.toString(),
                                                    Actions.UPDATE.toString(),
                                                    params,
                                                    null));
                                }
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));

    }

    private void updatePurseLicence(List<Future> futures, JsonObject newOrder,String operation) {
        Future<JsonObject> purseUpdateFuture = Future.future();
        futures.add(purseUpdateFuture);
        Future<JsonObject> purseUpdateLicencesFuture = Future.future();
        futures.add(purseUpdateLicencesFuture);
        Double price = Double.parseDouble(newOrder.getDouble("price").toString()) * newOrder.getInteger("amount");
        purseService.updatePurseAmount(price,
                newOrder.getString("id_structure"), operation,
                handlerJsonObject(purseUpdateFuture));
        structureService.updateAmountLicence(newOrder.getString("id_structure"), operation,
                newOrder.getInteger("amount"),
                handlerJsonObject(purseUpdateLicencesFuture));
    }

    @Get("region/orders/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void export (final HttpServerRequest request){
        List<String> params = request.params().getAll("id");
        List<String> idsEquipment = request.params().getAll("equipment_key");
        List<String> params3 = request.params().getAll("id_structure");
        generateLogs(request, params, idsEquipment, params3, false);
    }

    @Get("region/orders/library")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PrescriptorRight.class)
    public void exportLibrary (final HttpServerRequest request){
        List<String> params = request.params().getAll("id");
        List<String> idsEquipment = request.params().getAll("equipment_key");
        List<String> params3 = request.params().getAll("id_structure");
        generateLogs(request, params, idsEquipment, params3, true);
    }

    private void generateLogs(HttpServerRequest request, List<String> params, List<String> idsEquipment, List<String> params3, boolean library) {
        JsonArray idStructures = new JsonArray();
        for(String structureId : params3){
            idStructures.add(structureId);
        }
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> orderRegionFuture = Future.future();
        Future<JsonArray> equipmentsFuture = Future.future();

        CompositeFuture.all(structureFuture, orderRegionFuture, equipmentsFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray orderRegion = orderRegionFuture.result();
                JsonArray equipments = equipmentsFuture.result();
                JsonObject order,equipment,structure;
                for (int i = 0; i < orderRegion.size(); i++) {
                    order = orderRegion.getJsonObject(i);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
                    String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                    order.put("creation_date",creation_date);

                    for (int j = 0; j < equipments.size(); j++) {
                        equipment = equipments.getJsonObject(j);
                        if (equipment.getString("id").equals(order.getString("equipment_key"))) {
                            JsonObject priceDetails = getPriceTtc(equipment);
                            DecimalFormat df2 = new DecimalFormat("#.##");
                            double priceTTC = priceDetails.getDouble("priceTTC") * order.getInteger("amount");
                            double priceHT = priceDetails.getDouble("prixht") * order.getInteger("amount");
                            order.put("priceht", priceDetails.getDouble("prixht"));
                            order.put("tva5",priceDetails.getDouble("partTVA5"));
                            order.put("tva20",priceDetails.getDouble("partTVA20"));
                            order.put("unitedPriceTTC", priceDetails.getDouble("priceTTC"));
                            order.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
                            order.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
                            order.put("name", equipment.getString("titre"));
                            order.put("image", equipment.getString("image"));
                            order.put("ean", equipment.getString("ean"));
                        }
                    }

                    for (int j = 0; j < structures.size(); j++) {
                        structure = structures.getJsonObject(j);
                        if (structure.getString("id").equals(order.getString("id_structure"))) {
                            order.put("uai_structure", structure.getString("uai"));
                            order.put("name_structure", structure.getString("name"));
                        }
                    }
                }
                if(library) {
                    emailSender.sendMail(request, "sofianebernoussi@gmail.com", "Test", "Bonjour",
                            new JsonArray().add(new JsonObject().put("name", "orders.csv")
                                                                .put("content", generateExport(request, orderRegion))), message -> {
                        if(message.failed()) {
                            log.error("[CRRE@OrderRegionController.generateLogs] An error has occurred " + message.cause().getMessage());
                        }
                            });
                } else {
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                            .end(generateExport(request, orderRegion));
                }
            }
        });
        orderRegionService.getOrdersRegionById(idsOrders, handlerJsonArray(orderRegionFuture));
        structureService.getStructureById(idStructures,handlerJsonArray(structureFuture));
        searchByIds(idsEquipment, handlerJsonArray(equipmentsFuture));
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
                I18n.getInstance().translate("crre.structure", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("UAI", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.request", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("CAMPAIGN", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("resource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("ean", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.reassort", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("crre.number.licences", getHost(request), I18n.acceptLanguage(request)) + ";" +
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
                (log.getString("name_structure") != null ? log.getString("name_structure") : "") + ";" +
                (log.getString("uai_structure") != null ? log.getString("uai_structure") : "") + ";" +
                (log.getString("title") != null ? log.getString("title") : "") + ";" +
                (log.getString("campaign_name") != null ? log.getString("campaign_name") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getBoolean("reassort") != null ? (log.getBoolean("reassort") ? "Oui" : "Non")  : "") + ";" +
                (log.getInteger("amount") != null ? log.getInteger("amount").toString() : "") + ";" +
                (log.getDouble("priceht") != null ? log.getDouble("priceht").toString() : "") + ";" +
                (log.getDouble("tva5") != null ? log.getDouble("tva5").toString() : "") + ";" +
                (log.getDouble("tva20") != null ? log.getDouble("tva20").toString() : "") + ";" +
                (log.getDouble("unitedPriceTTC") != null ? log.getDouble("unitedPriceTTC").toString() : "") + ";" +
                (log.getDouble("totalPriceHT") != null ? log.getDouble("totalPriceHT").toString() : "") + ";" +
                (log.getDouble("totalPriceTTC") != null ? log.getDouble("totalPriceTTC").toString() : "") + ";" +
                (log.getString("comment") != null ? log.getString("comment") : "") + ";" +
                (log.getString("status") != null ?
                        I18n.getInstance().translate(log.getString("status"), getHost(request), I18n.acceptLanguage(request)) : "")
                + "\n";
    }
}
