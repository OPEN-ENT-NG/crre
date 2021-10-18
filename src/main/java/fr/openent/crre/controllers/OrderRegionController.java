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
import io.vertx.core.AsyncResult;
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
import java.util.*;

import static fr.openent.crre.controllers.LogController.UTF8_BOM;
import static fr.openent.crre.controllers.OrderController.exportPriceComment;
import static fr.openent.crre.helpers.ElasticSearchHelper.*;
import static fr.openent.crre.helpers.ElasticSearchHelper.searchfilter;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.extractedEquipmentInfo;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private final OrderRegionService orderRegionService;
    private final PurseService purseService;
    private final StructureService structureService;
    private final QuoteService quoteService;
    private final StatisticsService statisticsService;
    private final EmailSendService emailSender;
    private final JsonObject mail;
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OrderRegionController(Vertx vertx, JsonObject config, JsonObject mail) {
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.emailSender = new EmailSendService(emailSender);
        this.mail = mail;
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.statisticsService = new DefaultStatisticsService(Crre.crreSchema);
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService("equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

    @Post("/region/orders/:useCredit")
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
                                            String use_credit = request.getParam("useCredit");
                                            for(int i = 0 ; i<ordersList.size() ; i++){
                                                List<Future> futures = new ArrayList<>();
                                                JsonObject newOrder = ordersList.getJsonObject(i);
                                                Future<JsonObject> createOrdersRegionFuture = Future.future();
                                                futures.add(createOrdersRegionFuture);
                                                Double price = Double.parseDouble(newOrder.getDouble("price").toString())
                                                        *newOrder.getInteger("amount");
                                                updatePurseLicence(futures, newOrder,"-",price,use_credit);
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
            boolean old = Boolean.parseBoolean(request.getParam("old"));
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

                filters(params, handlerJsonArray(equipmentFilterFuture));

                if (StringUtils.isEmpty(query)) {
                    equipmentFilterAndQFuture.complete(new JsonArray());
                } else {
                    searchfilter(params, query, handlerJsonArray(equipmentFilterAndQFuture));
                }

                CompositeFuture.all(equipmentFilterFuture, equipmentFilterAndQFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonArray equipmentsGrade = equipmentFilterFuture.result(); // Tout les équipements correspondant aux grades
                        JsonArray equipmentsGradeAndQ = equipmentFilterAndQFuture.result();
                        JsonArray allEquipments = new JsonArray();
                        allEquipments.addAll(equipmentsGrade);
                        allEquipments.addAll(equipmentsGradeAndQ);
                        orderRegionService.search(user, allEquipments, query, startDate,
                                endDate, idStructure, filters, page, old, arrayResponseHandler(request));
                    }
                });
            }
        } else {
            plainTextSearchName(query, equipments -> {
                orderRegionService.search(user, equipments.right().getValue(), query, startDate,
                        endDate, idStructure, filters, page, old, arrayResponseHandler(request));
            });
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
                                            updatePurseLicence(futures, newOrder, "-",price,
                                                    newOrder.getString("use_credit"));
                                        }
                                    }else{
                                        if (status.equals("rejected")) {
                                            updatePurseLicence(futures, newOrder, "+",price,
                                                    newOrder.getString("use_credit"));
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
                                        LOGGER.error("An error when you want get id after create order region ",
                                                event.cause());
                                        request.response().setStatusCode(400).end();
                                    }
                                });
                            } catch (ClassCastException e) {
                                log.error("An error occurred when casting order id", e);
                            }
                        }));

    }

    private void updatePurseLicence(List<Future> futures, JsonObject newOrder,String operation, Double price, String use_credit) {
        Future<JsonObject> updateFuture = Future.future();
        futures.add(updateFuture);
        switch (use_credit) {
            case "licences": {
                structureService.updateAmountLicence(newOrder.getString("id_structure"), operation,
                        newOrder.getInteger("amount"),
                        handlerJsonObject(updateFuture));
                break;
            }
            case "consumable_licences": {
                structureService.updateAmountConsumableLicence(newOrder.getString("id_structure"), operation,
                        newOrder.getInteger("amount"),
                        handlerJsonObject(updateFuture));
                break;
            }
            case "credits": {
                purseService.updatePurseAmount(price,
                        newOrder.getString("id_structure"), operation,
                        handlerJsonObject(updateFuture));
                break;
            }
        }
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

    @Get("region/orders/old/status")
    @ApiDoc("Update status of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void updateStatusOrders (final HttpServerRequest request){
        // LDE function that returns status widh order id
        orderRegionService.getStatusByOrderId(event -> {
            if(event.isRight()) {
                JsonArray listIdOrders = event.right().getValue();
                for(int i = 0; i < listIdOrders.size(); i++) {
                    listIdOrders.getJsonObject(i).put("status", randomStatus());
                }
                // Update status in sql base
                orderRegionService.updateStatus(listIdOrders, event2 -> {
                    if(event2.isRight()) {
                        renderJson(request, event2.right().getValue());
                    }
                });
            }
        });
    }

    int randomStatus() {
        int tab[] = {1,2,3,4,6,7,9,10,14,15,20,35,55,57,58,59};
        Random rn = new Random();
        int range = 15 - 0 + 1;
        int randomNum =  rn.nextInt(range) + 0;
        return tab[randomNum];
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

    @Get("region/statistics")
    @ApiDoc("Get statistics")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getStatistics (final HttpServerRequest request){
        HashMap<String, ArrayList<String>> params = new HashMap<>();
        if (request.params().contains("year")) {
            params.put("year", new ArrayList<>(request.params().getAll("year")));
        }
        if (request.params().contains("catalog")) {
            params.put("catalog", new ArrayList<>(request.params().getAll("catalog")));
        }
        if (request.params().contains("public")) {
            params.put("public", new ArrayList<>(request.params().getAll("public")));
        }
        if (request.params().contains("reassort")) {
            params.put("reassort", new ArrayList<>(request.params().getAll("reassort")));
        }

        List<Future> futures = new ArrayList<>();
        Future<JsonObject> getNumericRessourcesFuture = Future.future();
        Future<JsonObject> getPaperRessourcesFuture = Future.future();
        Future<JsonObject> getRessourcesFuture = Future.future();
        Future<JsonObject> getOrdersFuture = Future.future();
        Future<JsonObject> getLicencesFuture = Future.future();
        Future<JsonObject> getStructuresMoreOneOrderFuture = Future.future();
        Future<JsonObject> getAllStructuresFuture = Future.future();
        futures.add(getNumericRessourcesFuture);
        futures.add(getPaperRessourcesFuture);
        futures.add(getRessourcesFuture);
        futures.add(getOrdersFuture);
        futures.add(getLicencesFuture);
        futures.add(getStructuresMoreOneOrderFuture);
        futures.add(getAllStructuresFuture);

        CompositeFuture.all(futures).setHandler(event -> {
                    if (event.succeeded()) {
                        JsonObject allNumericRessources = new JsonObject().put("allNumericRessources", getNumericRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch"));
                        JsonObject allPaperRessources = new JsonObject().put("allPaperRessources", getPaperRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch"));
                        JsonObject ressources = new JsonObject().put("ressources", getRessourcesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch").getJsonObject(0).getDouble("total"));
                        JsonObject orders = new JsonObject().put("orders", getOrdersFuture.result().getJsonObject("cursor").getJsonArray("firstBatch").getJsonObject(0).getInteger("total"));
                        JsonObject licencesResult = getLicencesFuture.result().getJsonObject("cursor").getJsonArray("firstBatch").getJsonObject(0);
                        JsonArray structuresMoreOneOrderResult = getStructuresMoreOneOrderFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");
                        JsonArray structuresResult = getAllStructuresFuture.result().getJsonObject("cursor").getJsonArray("firstBatch");

                        JsonObject concatStats = allNumericRessources.mergeIn(allPaperRessources).mergeIn(ressources).mergeIn(orders).mergeIn(new JsonObject().put("licences", licencesResult))
                                .mergeIn(new JsonObject().put("structuresMoreOneOrder", structuresMoreOneOrderResult))
                                .mergeIn(new JsonObject().put("structures", structuresResult));
                        JsonArray stats = new JsonArray().add(concatStats);
                        renderJson(request, stats);
                        log.info("ok");
                    }

                });

        statisticsService.getOrdersCompute("numeric_ressources", params, true, handlerJsonObject(getNumericRessourcesFuture));
        statisticsService.getOrdersCompute("paper_ressources", params, true, handlerJsonObject(getPaperRessourcesFuture));
        statisticsService.getOrdersCompute("ressources_total", params, false, handlerJsonObject(getRessourcesFuture));
        statisticsService.getOrdersCompute("order_year", params, false, handlerJsonObject(getOrdersFuture));

        statisticsService.getLicencesCompute(params, handlerJsonObject(getLicencesFuture));

        statisticsService.getStructureCompute(params, true, handlerJsonObject(getStructuresMoreOneOrderFuture));
        statisticsService.getStructureCompute(params, false, handlerJsonObject(getAllStructuresFuture));


    }

    @Get("region/orders/mongo")
    @ApiDoc("Generate and send mail to library")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void exportMongo (final HttpServerRequest request){
        List<Future> futures = new ArrayList<>();
        structureService.getAllStructuresDetail(event -> {
            if(event.isRight()) {
                JsonArray structures = event.right().getValue();
                for (int i = 0; i < structures.size(); i++) {
                    Future<JsonObject> addStructureStatFuture = Future.future();
                    futures.add(addStructureStatFuture);
                    JsonObject structure = structures.getJsonObject(i);
                    // All compute with structure object
                    structure.put("date", LocalDate.now().toString());
                    List<Future> futuresStat = new ArrayList<>();
                    Future<JsonArray> licencesFuture = Future.future();
                    Future<JsonArray> freeLicenceFuture = Future.future();
                    Future<JsonArray> totalRessourcesFuture = Future.future();
                    Future<JsonArray> ordersByYearFuture = Future.future();
                    Future<JsonArray> ordersByCampaignFuture = Future.future();
                    Future<JsonArray> numeriqueRessourceFuture = Future.future();
                    Future<JsonArray> papierRessourceFuture = Future.future();
                    futuresStat.add(licencesFuture);
                    futuresStat.add(freeLicenceFuture);
                    futuresStat.add(ordersByCampaignFuture);
                    futuresStat.add(ordersByYearFuture);
                    futuresStat.add(totalRessourcesFuture);
                    futuresStat.add(numeriqueRessourceFuture);
                    futuresStat.add(papierRessourceFuture);

                    CompositeFuture.all(futuresStat).setHandler(event2 -> {
                        if (event2.succeeded()) {
                            JsonArray licences = licencesFuture.result();
                            JsonArray free_total = freeLicenceFuture.result();
                            JsonArray ressources_total = totalRessourcesFuture.result();
                            JsonArray order_year = ordersByYearFuture.result();
                            JsonArray order_campaign = ordersByCampaignFuture.result();
                            JsonArray numeric_ressources = numeriqueRessourceFuture.result();
                            JsonArray paper_ressources = papierRessourceFuture.result();

                            if(free_total.size() > 0 || ressources_total.size() > 0) {
                                JsonObject stats = new JsonObject();
                                // Every 30/04, add a new line in licences mongo db with new year
                                if(LocalDate.now().getMonthValue() == 5 && LocalDate.now().getDayOfMonth() == 1) {
                                    licences.add(licences.getJsonObject(0));
                                }
                                // Put a n+1 year if after 30/04
                                if(LocalDate.now().getMonthValue() < 5) {
                                    licences.getJsonObject(licences.size() - 1).put("year", LocalDate.now().getYear() + "");
                                } else {
                                    licences.getJsonObject(licences.size() - 1).put("year", LocalDate.now().getYear() + 1 + "");
                                }
                                double licencesPercentage = (licences.getJsonObject(licences.size() - 1).getInteger("amount") / licences.getJsonObject(0).getInteger("initial_amount")) * 100;
                                licences.getJsonObject(licences.size() - 1).put("percentage", 100 - licencesPercentage);
                                stats.put("licences", licences);
                                stats.put("free_total", free_total);
                                stats.put("ressources_total", ressources_total);
                                stats.put("order_year", order_year);
                                stats.put("order_campaign", order_campaign);
                                stats.put("numeric_ressources", numeric_ressources);
                                stats.put("paper_ressources", paper_ressources);
                            }
                                String JSON_DATA = "{ \"licences\":[ { \"amount\":5000, \"initial_amount\":10000, \"year\":\"2021\", \"percentage\":50 }, { \"amount\":2000, \"initial_amount\":10000, \"year\":\"2020\", \"percentage\":20 }, { \"amount\":3000, \"initial_amount\":10000, \"year\":\"2019\", \"percentage\":30 } ], \"free_total\":[ { \"total\":150, \"reassort\":false, \"year\":\"2021\" }, { \"total\":200, \"reassort\":false, \"year\":\"2020\" }, { \"total\":175, \"reassort\":false, \"year\":\"2019\" }, { \"total\":15, \"reassort\":true, \"year\":\"2021\" }, { \"total\":20, \"reassort\":true, \"year\":\"2020\" }, { \"total\":17, \"reassort\":true, \"year\":\"2019\" } ], \"ressources_total\":[ { \"total\":200000, \"reassort\":false, \"year\":\"2021\" }, { \"total\":250000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":225000, \"reassort\":false, \"year\":\"2019\" }, { \"total\":2000, \"reassort\":true, \"year\":\"2021\" }, { \"total\":2500, \"reassort\":true, \"year\":\"2020\" }, { \"total\":2250, \"reassort\":true, \"year\":\"2019\" } ], \"order_year\":[ { \"total\":1000, \"reassort\":false, \"year\":\"2021\" }, { \"total\":1500, \"reassort\":false, \"year\":\"2020\" }, { \"total\":1250, \"reassort\":false, \"year\":\"2019\" }, { \"total\":100, \"reassort\":true, \"year\":\"2021\" }, { \"total\":150, \"reassort\":true, \"year\":\"2020\" }, { \"total\":125, \"reassort\":true, \"year\":\"2019\" } ], \"order_campaign\":[ { \"name\":\"Campagne papier\", \"id\":2, \"total\":750 }, { \"name\":\"Campagne numérique\", \"id\":1, \"total\":250 } ], \"numeric_ressources\":[ { \"total\":1500, \"reassort\":false, \"year\":\"2021\" }, { \"total\":2000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":1750, \"reassort\":false, \"year\":\"2019\" }, { \"total\":150, \"reassort\":true, \"year\":\"2021\" }, { \"total\":200, \"reassort\":true, \"year\":\"2020\" }, { \"total\":175, \"reassort\":true, \"year\":\"2019\" } ], \"paper_ressources\":[ { \"total\":500, \"reassort\":false, \"year\":\"2021\" }, { \"total\":1000, \"reassort\":false, \"year\":\"2020\" }, { \"total\":750, \"reassort\":false, \"year\":\"2019\" }, { \"total\":50, \"reassort\":true, \"year\":\"2021\" }, { \"total\":100, \"reassort\":true, \"year\":\"2020\" }, { \"total\":75, \"reassort\":true, \"year\":\"2019\" } ] }";
                                JsonObject statss = new JsonObject(JSON_DATA);
                                structure.put("stats", statss);
                                statisticsService.exportMongo(structure, handlerJsonObject(addStructureStatFuture));
                            }
                    });

                    // Licences

                    statisticsService.getLicences(structure.getString("id_structure"), handlerJsonArray(licencesFuture));

                    // Commandes

                    statisticsService.getOrdersByYear(structure.getString("id_structure"), handlerJsonArray(ordersByYearFuture));
                    statisticsService.getOrdersByCampaign(structure.getString("id_structure"), handlerJsonArray(ordersByCampaignFuture));

                    // Licences

                    statisticsService.getFreeLicences(structure.getString("id_structure"), handlerJsonArray(freeLicenceFuture));

                    // Finances

                    statisticsService.getTotalRessources(structure.getString("id_structure"), handlerJsonArray(totalRessourcesFuture));

                    // Ressources

                    statisticsService.getRessources(structure.getString("id_structure"), "articlenumerique", handlerJsonArray(numeriqueRessourceFuture));
                    statisticsService.getRessources(structure.getString("id_structure"), "articlepapier", handlerJsonArray(papierRessourceFuture));

                }
                CompositeFuture.all(futures).setHandler(event2 -> {
                    if (event2.succeeded()) {
                        log.info("export mongo ok");
                    }
                });
            }
        });
    }

    @Get("/region/statistics/years")
    @ApiDoc("get all years from statistics ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getAllYears(HttpServerRequest request) {
            statisticsService.getAllYears(event -> {
                JsonArray yearsResult = event.right().getValue().getJsonObject("cursor").getJsonArray("firstBatch");
                renderJson(request, yearsResult);
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
        structureService.getStructureById(idStructures, null, handlerJsonArray(structureFuture));
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
/*                            emailSender.sendMail(request, mail, "Test",
                                    "Bonjour", attachment, message -> {
                                        if(!message.isRight()) {
                                            log.error("[CRRE@OrderRegionController.generateLogs] An error has occurred " + message.left());
                                        }
                                    });*/
                            renderJson(request, response2.right().getValue());
                        }
                    });
                    orderRegionService.deletedOrders(ordersClient, "order_client_equipment", handlerJsonObject(deleteOldOrderClientFuture));
                    orderRegionService.deletedOrders(ordersRegion, "order-region-equipment", handlerJsonObject(deleteOldOrderRegionFuture));
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
            // Skip offers
            if (!order.containsKey("totalPriceTTC")) {
                if (order.containsKey("owner_name")) {
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
                            //order.put("cible", equipment.)
                            JsonArray offers = computeOffers(equipment, order);
                            if (offers.size() > 0) {
                                JsonArray orderOfferArray = new JsonArray();
                                int freeAmount = 0;
                                for (int k = 0; k < offers.size(); k++) {
                                    JsonObject orderOffer = new JsonObject();
                                    orderOffer.put("name", offers.getJsonObject(k).getString("name"));
                                    orderOffer.put("titre", offers.getJsonObject(k).getString("titre"));
                                    orderOffer.put("amount", offers.getJsonObject(k).getLong("value"));
                                    freeAmount += offers.getJsonObject(k).getLong("value");
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
                                }
                                order.put("total_free", freeAmount);
                                order.put("offers", orderOfferArray);
                            }
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
