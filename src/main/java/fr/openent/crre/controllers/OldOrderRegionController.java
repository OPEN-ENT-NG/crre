package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.security.ValidatorRight;
import fr.openent.crre.service.OldOrderService;
import fr.openent.crre.service.OrderRegionService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.impl.DefaultOldOrderService;
import fr.openent.crre.service.impl.DefaultOrderRegionService;
import fr.openent.crre.service.impl.DefaultStructureService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.controllers.LogController.UTF8_BOM;
import static fr.openent.crre.controllers.OrderRegionController.generateExportLine;
import static fr.openent.crre.controllers.OrderRegionController.getExportHeader;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.utils.OrderUtils.dealWithPriceTTC_HT;
import static fr.openent.crre.utils.OrderUtils.extractedEquipmentInfo;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class OldOrderRegionController extends BaseController {


    private final OldOrderService oldOrderService;
    private final StructureService structureService;
    private final OrderRegionService orderRegionService;


    public OldOrderRegionController() {
        this.oldOrderService = new DefaultOldOrderService("equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema);
        this.orderRegionService = new DefaultOrderRegionService("equipment");
    }

    @Get("/orderRegion/projects/old")
    @ApiDoc("get all projects ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getAllProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            Integer page = request.getParam("page") != null ? Integer.parseInt(request.getParam("page")) : 0;
            String startDate = request.getParam("startDate");
            String endDate = request.getParam("endDate");
            String idStructure = request.getParam("idStructure");
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            orderRegionService.getAllProjects(user, startDate, endDate, page, filterRejectedSentOrders, idStructure, true, arrayResponseHandler(request));
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
            String idStructure = request.getParam("idStructure");
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
                        !request.params().entries().get(i).getKey().equals("idStructure"))
                    filters.add(new JsonObject().put(request.params().entries().get(i).getKey(),
                            request.params().entries().get(i).getValue()));
            }
            String finalQuery = query;
            if (params.size() > 0) {
                if (request.params().contains("q")) {
                    oldOrderService.filterSearch(user, finalQuery, startDate,
                            endDate, idStructure, filters, page, arrayResponseHandler(request));
                } else {
                    oldOrderService.filter_only(user, startDate,
                            endDate, idStructure, filters, page, arrayResponseHandler(request));
                }
            } else {
                oldOrderService.search(user, finalQuery, startDate,
                        endDate, idStructure, filters, page, arrayResponseHandler(request));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Get("/ordersRegion/projects/old/search_filter")
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
                structureService.getStructuresByTypeAndFilter(request.getParam("type"), null, handlerJsonArray(listeUAIFuture));
            } else {
                getOrders(query, filters, user, page, request);
            }
        });
    }

    @Get("/ordersRegion/orders/old")
    @ApiDoc("get all orders of each project")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOrdersByProjects(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null
                    && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            List<String> projectIds = request.params().getAll("project_id");
            List<Future> futures = new ArrayList<>();
            for(String id : projectIds){
                Future<JsonArray> projectIdFuture = Future.future();
                futures.add(projectIdFuture);
                int idProject = Integer.parseInt(id);
                oldOrderService.getAllOrderRegionByProject(idProject, filterRejectedSentOrders, handlerJsonArray(projectIdFuture));
            }
            getCompositeFutureAllOrderRegionByProject(request, futures);
        });
    }

    private void getCompositeFutureAllOrderRegionByProject(HttpServerRequest request, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                List<JsonArray> resultsList = event.result().list();
                getSearchByIds(request, resultsList);
            } else {
                log.error(event.cause());
                badRequest(request);
            }
        });
    }

    private void getSearchByIds(HttpServerRequest request, List<JsonArray> resultsList) {
        JsonArray finalResult = new JsonArray();
        for (JsonArray orders : resultsList) {
            finalResult.add(orders);
            for (Object order : orders) {
                JsonObject orderJson = (JsonObject) order;
                double price = Double.parseDouble(orderJson.getString("price")) * orderJson.getInteger("amount");
                orderJson.put("price", price);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderJson.getString("creation_date"), formatter);
                String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                orderJson.put("creation_date",creation_date);
            }
        }
        renderJson(request, finalResult);
    }

    @Get("region/orders/old/exports")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void export (final HttpServerRequest request){
        List<String> params = request.params().getAll("id");
        List<String> params3 = request.params().getAll("id_structure");
        generateLogs(request, params, params3);
    }

    private void generateLogs(HttpServerRequest request, List<String> params, List<String> params3) {
        JsonArray idStructures = new JsonArray();
        for(String structureId : params3){
            idStructures.add(structureId);
        }
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        Future<JsonArray> structureFuture = Future.future();
        Future<JsonArray> orderRegionFuture = Future.future();

        CompositeFuture.all(structureFuture, orderRegionFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray structures = structureFuture.result();
                JsonArray orderRegion = orderRegionFuture.result();
                JsonObject order, structure;
                JsonArray ordersClient = new JsonArray(), ordersRegion = new JsonArray();
                for (int i = 0; i < orderRegion.size(); i++) {
                    order = orderRegion.getJsonObject(i);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(order.getString("creation_date"), formatter);
                    String creation_date = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(zonedDateTime);
                    order.put("creation_date", creation_date);
                    ordersRegion.add(order.getLong("id"));
                    ordersClient.add(order.getLong("id_order_client_equipment"));
                    dealWithPriceTTC_HT(order);
                    order.put("name", order.getString("equipment_name"));
                    order.put("image", order.getString("urlcouverture"));
                    order.put("ean", order.getString("equipment_key"));
                    order.put("editor", order.getString("editeur"));
                    order.put("diffusor", order.getString("distributeur"));
                    order.put("type", order.getString("equipment_format"));
                    for (int j = 0; j < structures.size(); j++) {
                        structure = structures.getJsonObject(j);
                        if (structure.getString("id").equals(order.getString("id_structure"))) {
                            order.put("uai_structure", structure.getString("uai"));
                            order.put("name_structure", structure.getString("name"));
                        }
                    }
                }
                request.response()
                        .putHeader("Content-Type", "text/csv; charset=utf-8")
                        .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                        .end(generateExport(request, orderRegion));

            };
        });
        orderRegionService.getOrdersRegionById(idsOrders, true, handlerJsonArray(orderRegionFuture));
        structureService.getStructureById(idStructures,handlerJsonArray(structureFuture));
    }

    private static String generateExport(HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
            report.append(generateExportLine(logs.getJsonObject(i)));
        }
        return report.toString();
    }
}
