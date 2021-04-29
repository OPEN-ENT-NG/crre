package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.*;
import fr.openent.crre.service.impl.*;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
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
import org.apache.commons.lang3.StringUtils;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.controllers.LogController.UTF8_BOM;
import static fr.openent.crre.controllers.OrderController.exportPriceComment;
import static fr.openent.crre.helpers.ElasticSearchHelper.*;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchfilter;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.extractedEquipmentInfo;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;
    private final PurseService purseService;
    private final StructureService structureService;
    private final QuoteService quoteService;
    private final EmailSendService emailSender;
    private final JsonObject mail;
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OrderRegionController(Vertx vertx, JsonObject config, JsonObject mail) {
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.emailSender = new EmailSendService(emailSender);
        this.mail = mail;
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService("equipment");
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
                                                Double price = Double.parseDouble(newOrder.getDouble("price").toString())*newOrder.getInteger("amount");
                                                updatePurseLicence(futures, newOrder,"-",price);
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
    }

    @Get("/orderRegion/projects")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            Boolean old = Boolean.valueOf(request.getParam("old"));
            String idStructure = request.getParam("idStructure");
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null &&
                    Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            orderRegionService.getAllProjects(user, startDate, endDate, page, filterRejectedSentOrders, idStructure, old, arrayResponseHandler(request));
        });
    }

    private void getOrders (String query, JsonArray filters, UserInfos user, Integer page, HttpServerRequest request) {
        HashMap<String, ArrayList<String>> params = new HashMap<>();
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
        String idStructure = request.getParam("idStructure");
        Boolean old = Boolean.valueOf(request.getParam("old"));

        int length = request.params().entries().size();
        for (int i = 0; i < length; i++) {
            if (!request.params().entries().get(i).getKey().equals("q") &&
                    !request.params().entries().get(i).getKey().equals("startDate") &&
                    !request.params().entries().get(i).getKey().equals("distributeur") &&
                    !request.params().entries().get(i).getKey().equals("editeur") &&
                    !request.params().entries().get(i).getKey().equals("_index") &&
                    !request.params().entries().get(i).getKey().equals("type") &&
                    !request.params().entries().get(i).getKey().equals("endDate") &&
                    !request.params().entries().get(i).getKey().equals("page") &&
                    !request.params().entries().get(i).getKey().equals("id_structure") &&
                    !request.params().entries().get(i).getKey().equals("old") &&
                    !request.params().entries().get(i).getKey().equals("idStructure"))
                filters.add(new JsonObject().put(request.params().entries().get(i).getKey(),
                        request.params().entries().get(i).getValue()));
        }
        if (params.size() > 0) {
            if(!old) {
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
                            orderRegionService.filterSearch(user, allEquipments, query, startDate,
                                    endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                        } else {
                            orderRegionService.filter_only(user, equipmentsGrade, startDate,
                                    endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                        }
                    }
                });
                filters(params, handlerJsonArray(equipmentFilterFuture));
                if (StringUtils.isEmpty(query)) {
                    filters(params, handlerJsonArray(equipmentFilterAndQFuture));
                } else {
                    searchfilter(params, query, handlerJsonArray(equipmentFilterAndQFuture));
                }
            } else {
                if (request.params().contains("q")) {
                    orderRegionService.filterSearch(user, null, query, startDate,
                            endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                } else {
                    orderRegionService.filter_only(user, null, startDate,
                            endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                }
            }
        } else {
            if (!old) {
                if (!(query.equals(""))) {
                    plainTextSearchName(query, equipments -> {
                        if (equipments.right().getValue().size() > 0) {
                            orderRegionService.search(user, equipments.right().getValue(), query, startDate,
                                    endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                        } else {
                            orderRegionService.filterSearchWithoutEquip(user, query, startDate, endDate, idStructure, filters, page,
                                    arrayResponseHandler(request));
                        }
                    });
                } else {
                    orderRegionService.filterSearchWithoutEquip(user, query, startDate, endDate, idStructure, filters, page,
                            arrayResponseHandler(request));
                }
            } else {
                orderRegionService.search(user, null, query, startDate,
                        endDate, idStructure, filters, page, old, arrayResponseHandler(request));
            }
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
            if (request.params().contains("q")) {
                try {
                    query = URLDecoder.decode(request.getParam("q"), "UTF-8").toLowerCase();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if(request.params().contains("type") || request.params().contains("id_structure")) {
                String finalQuery = query;
                structureService.getStructuresByTypeAndFilter(request.getParam("type"),
                        request.params().getAll("id_structure"), event -> {
                    if (event.isRight()) {
                        JsonArray listeIdStructure = event.right().getValue();
                        filters.add(new JsonObject().put("id_structure", listeIdStructure));
                        getOrders(finalQuery, filters, user, page, request);
                    } else {
                        log.error(event.left().getValue());
                        badRequest(request);
                    }
                });
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
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null
                    && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            Boolean old = Boolean.valueOf(request.getParam("old"));
            List<String> projectIds = request.params().getAll("project_id");
            List<Future> futures = new ArrayList<>();
            for(String id : projectIds){
                Future<JsonArray> projectIdFuture = Future.future();
                futures.add(projectIdFuture);
                int idProject = Integer.parseInt(id);
                orderRegionService.getAllOrderRegionByProject(idProject, filterRejectedSentOrders, old, handlerJsonArray(projectIdFuture));
            }
            getCompositeFutureAllOrderRegionByProject(request, old, futures);
        });
    }

    private void getCompositeFutureAllOrderRegionByProject(HttpServerRequest request, Boolean old, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                List<JsonArray> resultsList = event.result().list();
                if(!old) {
                    List<String> listIdsEquipment = new ArrayList<>();
                    for (JsonArray orders : resultsList) {
                        for (Object order : orders) {
                            listIdsEquipment.add(((JsonObject) order).getString("equipment_key"));
                        }
                    }
                    getSearchByIds(request, resultsList, listIdsEquipment);
                } else {
                    getSearchByIdsOld(request, resultsList);
                }

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

    private void getSearchByIdsOld(HttpServerRequest request, List<JsonArray> resultsList) {
        JsonArray finalResult = new JsonArray();
        for (JsonArray orders : resultsList) {
            finalResult.add(orders);
            for (Object order : orders) {
                JsonObject orderJson = (JsonObject) order;
                double price = Double.parseDouble(orderJson.getString("equipment_price")) * orderJson.getInteger("amount");
                orderJson.put("price", price);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date"), formatter);
                String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                orderJson.put("creation_date",creation_date);
            }
        }
        renderJson(request, finalResult);
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
                                for(int i = 0 ; i < ordersList.size() ; i++) {
                                    JsonObject newOrder = ordersList.getJsonObject(i);
                                    Double price = Double.parseDouble(newOrder.getDouble("price").toString());
                                    if(newOrder.getString("status").equals("REJECTED")) {
                                        if (status.equals("valid")) {
                                            updatePurseLicence(futures, newOrder, "-",price);
                                        }
                                    }else{
                                        if (status.equals("rejected")) {
                                            updatePurseLicence(futures, newOrder, "+",price);
                                        }
                                    }
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
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));

    }

    private void updatePurseLicence(List<Future> futures, JsonObject newOrder,String operation, Double price) {
        Future<JsonObject> purseUpdateFuture = Future.future();
        futures.add(purseUpdateFuture);
        Future<JsonObject> purseUpdateLicencesFuture = Future.future();
        futures.add(purseUpdateLicencesFuture);
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
    @ResourceFilter(ValidatorRight.class)
    public void export (final HttpServerRequest request){
        List<String> params = request.params().getAll("id");
        List<String> idsEquipment = request.params().getAll("equipment_key");
        List<String> params3 = request.params().getAll("id_structure");
        Boolean old = Boolean.valueOf(request.getParam("old"));
        generateLogs(request, params, idsEquipment, params3, null, old, false);
    }

    @Post("region/orders/library")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void exportLibrary (final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            List<String> params = request.params().getAll("id");
            List<String> idsEquipment = request.params().getAll("equipment_key");
            List<String> params3 = request.params().getAll("id_structure");
            generateLogs(request, params, idsEquipment, params3, user, true, true);
        });
    }

    private void generateLogs(HttpServerRequest request, List<String> params, List<String> idsEquipment, List<String> params3,
                              UserInfos user, Boolean old, boolean library) {
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
                JsonArray ordersClient = new JsonArray(), ordersRegion = new JsonArray();
                beautifyOrders(structures, orderRegion, equipments, ordersClient, ordersRegion);
                if (library) {
                    sendMailLibraryAndRemoveWaitingAdmin(request, user, structures, orderRegion, ordersClient, ordersRegion);
                } else{
                    //Export CSV
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                            .end(generateExport(request, orderRegion));
                    }

            }
        });
        if(library) {
            orderRegionService.getOrdersRegionById(idsOrders, false, handlerJsonArray(orderRegionFuture));
        } else {
            orderRegionService.getOrdersRegionById(idsOrders, old, handlerJsonArray(orderRegionFuture));
        }
        structureService.getStructureById(idStructures,handlerJsonArray(structureFuture));
        searchByIds(idsEquipment, handlerJsonArray(equipmentsFuture));
    }

    private void sendMailLibraryAndRemoveWaitingAdmin(HttpServerRequest request, UserInfos user, JsonArray structures,
                                                      JsonArray orderRegion, JsonArray ordersClient, JsonArray ordersRegion) {
        int nbEtab = structures.size();
        String base64File = Base64.getEncoder().encodeToString(generateExport(request, orderRegion).getBytes(StandardCharsets.UTF_8));
        Future<JsonObject> insertOldOrdersFuture = Future.future();
        Future<JsonObject> deleteOldOrderClientFuture = Future.future();
        Future<JsonObject> deleteOldOrderRegionFuture = Future.future();


        try {
            orderRegionService.insertOldClientOrders(orderRegion, response -> {
                if (response.isRight()) {
                    quoteService.insertQuote(user, nbEtab, base64File, response2 -> {
                        if (response2.isRight()) {
                            JsonArray attachment = new fr.wseduc.webutils.collections.JsonArray();
                            attachment.add(new JsonObject().put("name", "orders.csv").put("content",base64File));
                            String mail = this.mail.getString("address");
                            emailSender.sendMail(request, mail, "Test",
                                    "Bonjour", attachment, message -> {
                                        if(!message.isRight()) {
                                            log.error("[CRRE@OrderRegionController.generateLogs] An error has occurred " + message.left());
                                        }
                                    });
                            renderJson(request, response2.right().getValue());
                        }
                    });
                    orderRegionService.deletedOrderClient(ordersClient, handlerJsonObject(deleteOldOrderClientFuture));
                    orderRegionService.deleteOrderRegion(ordersRegion, handlerJsonObject(deleteOldOrderRegionFuture));
                    try {
                        orderRegionService.insertOldOrders(orderRegion, false, handlerJsonObject(insertOldOrdersFuture));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void beautifyOrders(JsonArray structures, JsonArray orderRegion, JsonArray equipments, JsonArray ordersClient, JsonArray ordersRegion) {
        JsonObject order;
        JsonObject equipment;
        for (int i = 0; i < orderRegion.size(); i++) {
            order = orderRegion.getJsonObject(i);
            if(order.containsKey("owner_name")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
                String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                order.put("creation_date", creation_date);
            }
            ordersRegion.add(order.getLong("id"));
            ordersClient.add(order.getLong("id_order_client_equipment"));

            for (int j = 0; j < equipments.size(); j++) {
                equipment = equipments.getJsonObject(j);
                if (equipment.getString("id").equals(order.getString("equipment_key"))) {
                    JsonObject priceDetails = getPriceTtc(equipment);
                    DecimalFormat df2 = new DecimalFormat("#.##");
                    double priceTTC = priceDetails.getDouble("priceTTC") * order.getInteger("amount");
                    double priceHT = priceDetails.getDouble("prixht") * order.getInteger("amount");
                    order.put("priceht", priceDetails.getDouble("prixht"));
                    order.put("tva5", priceDetails.getDouble("partTVA5"));
                    order.put("tva20", priceDetails.getDouble("partTVA20"));
                    order.put("unitedPriceTTC", priceDetails.getDouble("priceTTC"));
                    order.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
                    order.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
                    extractedEquipmentInfo(order, equipment);
                    order.put("grade", equipment.getJsonArray("disciplines").getJsonObject(0).getString("libelle"));
                    putStructuresNameUAI(structures, order);
                    if (equipment.getString("type").equals("articlenumerique")) {
                        JsonArray offers = computeOffers(equipment, order);
                        if (offers.size() > 0) {
                            JsonArray orderOfferArray = new JsonArray();
                            for (int k = 0; k < offers.size(); k++) {
                                JsonObject orderOffer = new JsonObject();
                                orderOffer.put("name", offers.getJsonObject(k).getString("name"));
                                orderOffer.put("titre", offers.getJsonObject(k).getString("titre"));
                                orderOffer.put("amount", offers.getJsonObject(k).getLong("value"));
                                orderOffer.put("ean", offers.getJsonObject(k).getString("ean"));
                                orderOffer.put("unitedPriceTTC", 0);
                                orderOffer.put("totalPriceHT", 0);
                                orderOffer.put("totalPriceTTC", 0);
                                orderOffer.put("creation_date", order.getString("creation_date"));
                                orderOffer.put("id_structure", order.getString("id_structure"));
                                orderOffer.put("campaign_name", order.getString("campaign_name"));
                                orderOffer.put("id", order.getLong("id"));
                                orderOffer.put("title", order.getString("title"));
                                orderOffer.put("comment", offers.getJsonObject(k).getString("comment"));
                                putStructuresNameUAI(structures, orderOffer);
                                orderOfferArray.add(orderOffer);
                                orderRegion.add(orderOffer);
                                i++;
                            }
                            order.put("offers", orderOfferArray);
                        }
                    }
                }
            }
        }
    }

    private void putStructuresNameUAI(JsonArray structures, JsonObject order) {
        for (int s = 0; s < structures.size(); s++) {
            JsonObject structure = structures.getJsonObject(s);
            if (structure.getString("id").equals(order.getString("id_structure"))) {
                order.put("uai_structure", structure.getString("uai"));
                order.put("name_structure", structure.getString("name"));
            }
        }
    }

    private static JsonArray computeOffers(JsonObject equipment, JsonObject order) {
        JsonArray leps = equipment.getJsonArray("offres").getJsonObject(0).getJsonArray("leps");
        Long amount = order.getLong("amount");
        int gratuit = 0;
        int gratuite = 0;
        JsonArray offers = new JsonArray();
        for (int i = 0; i < leps.size(); i++) {
            JsonObject offer = leps.getJsonObject(i);
            JsonArray conditions = offer.getJsonArray("conditions");
            JsonObject offerObject = new JsonObject().put("titre", "Manuel " +
                    offer.getJsonArray("licence").getJsonObject(0).getString("valeur"));
            if(conditions.size() > 1) {
                for(int j = 0; j < conditions.size(); j++) {
                    int condition = conditions.getJsonObject(j).getInteger("conditionGratuite");
                    if(amount >= condition && gratuit < condition) {
                        gratuit = condition;
                        gratuite = conditions.getJsonObject(j).getInteger("gratuite");
                    }
                }
            } else {
                gratuit = offer.getJsonArray("conditions").getJsonObject(0).getInteger("conditionGratuite");
                gratuite = (int) (offer.getJsonArray("conditions").getJsonObject(0).getInteger("gratuite") * Math.floor(amount / gratuit));
            }
            offerObject.put("value", gratuite);
            offerObject.put("ean", offer.getString("ean"));
            offerObject.put("name", offer.getString("titre"));
            offerObject.put("comment", equipment.getString("ean"));
            if(gratuite > 0) {
                offers.add(offerObject);
            }
        }
        return offers;
    }

    private static String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
            report.append(generateExportLine(logs.getJsonObject(i)));
        }
        return report.toString();
    }

    public static String getExportHeader (HttpServerRequest request) {
        return "Id" + ";" +
                I18n.getInstance().translate("crre.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
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
                I18n.getInstance().translate("csv.comment", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    public static String generateExportLine(JsonObject log) {
        return  (log.getInteger("id") != null ? log.getInteger("id").toString() : "") + ";" +
                (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
                (log.getString("name_structure") != null ? log.getString("name_structure") : "") + ";" +
                (log.getString("uai_structure") != null ? log.getString("uai_structure") : "") + ";" +
                (log.getString("title") != null ? log.getString("title") : "") + ";" +
                (log.getString("campaign_name") != null ? log.getString("campaign_name") : "") + ";" +
                (log.getString("name") != null ? log.getString("name") : "") + ";" +
                (log.getString("ean") != null ? log.getString("ean") : "") + ";" +
                (log.getBoolean("reassort") != null ? (log.getBoolean("reassort") ? "Oui" : "Non")  : "") + ";" +
                exportPriceComment(log)
                + "\n";
    }


}
