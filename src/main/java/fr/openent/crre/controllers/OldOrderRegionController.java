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
import io.vertx.core.json.Json;
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
import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonObject;
import static fr.openent.crre.utils.OrderUtils.getPriceTtc;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OldOrderRegionController extends BaseController {


    private final OldOrderRegionService oldOrderRegionService;
    private final PurseService purseService;
    private final StructureService structureService;
    private final QuoteService quoteService;
    private final EmailSendService emailSender;
    private final JsonObject mail;
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public OldOrderRegionController(Vertx vertx, JsonObject config, JsonObject mail) {
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.emailSender = new EmailSendService(emailSender);
        this.mail = mail;
        this.oldOrderRegionService = new DefaultOldOrderRegionService("equipment");
        this.purseService = new DefaultPurseService();
        this.quoteService = new DefaultQuoteService("equipment");
        this.structureService = new DefaultStructureService(Crre.crreSchema);
    }

/*
    @Delete("/region/:id/order")
    @ApiDoc("delete order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void deleteOrderRegion(final HttpServerRequest request) {
        int idRegion = Integer.parseInt(request.getParam("id"));
        oldOrderRegionService.deleteOneOrderRegion(idRegion, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDERREGION.toString(),
                Actions.DELETE.toString(),
                Integer.toString(idRegion),
                new JsonObject().put("idRegion", idRegion)));
    }
*/

    @Get("/orderRegion/:id/order/old")
    @ApiDoc("get order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ValidatorRight.class)
    public void getOneOrder(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        oldOrderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
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
            boolean filterRejectedSentOrders = request.getParam("filterRejectedSentOrders") != null && Boolean.parseBoolean(request.getParam("filterRejectedSentOrders"));
            oldOrderRegionService.getAllProjects(user, startDate, endDate, page, filterRejectedSentOrders, arrayResponseHandler(request));
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
                        if (request.params().contains("q")) {
                            oldOrderRegionService.filterSearch(user, finalQuery, startDate,
                                    endDate, filters, page, arrayResponseHandler(request));
                        } else {
                            oldOrderRegionService.filter_only(user, startDate,
                                    endDate, filters, page, arrayResponseHandler(request));
                        }
            } else { 
                            oldOrderRegionService.search(user, finalQuery, startDate,
                                endDate, filters, page, arrayResponseHandler(request));
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
                oldOrderRegionService.getAllOrderRegionByProject(idProject, filterRejectedSentOrders, handlerJsonArray(projectIdFuture));
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

  /*  @Put("/region/orders/:status")
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
                                        oldOrderRegionService.updateOrders(ids,status,justification,
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

    }*/

/*    private void updatePurseLicence(List<Future> futures, JsonObject newOrder,String operation, Double price) {
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
    }*/

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

                            DecimalFormat df2 = new DecimalFormat("#.##");
                            Double price = Double.parseDouble(order.getString("equipment_price"));
                            Double priceht = order.getDouble("equipment_priceht");
                            double priceTTC = price * order.getInteger("amount");
                            double priceHT = priceht * order.getInteger("amount");
                            order.put("priceht", priceht);
                            order.put("tva5",order.getDouble("equipment_tva5"));
                            order.put("tva20",order.getDouble("equipment_tva20"));
                            order.put("unitedPriceTTC", price);
                            order.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
                            order.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
                            order.put("name", order.getString("titre"));
                            order.put("image", order.getString("urlcouverture"));
                            order.put("ean", order.getString("ean"));
                            order.put("editor", order.getString("editeur"));
                            order.put("diffusor", order.getString("distributeur"));
                            order.put("type", order.getString("type"));
/*                            if (order.getString("type").equals("articlenumerique")) {
                                JsonArray offers = computeOffers(equipment, order);
                                if (offers.size() > 0) {
                                    for (int k = 0; k < offers.size(); k++) {
                                        JsonObject orderOffer = new JsonObject();
                                        orderOffer.put("name", offers.getJsonObject(k).getString("titre"));
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
                                        for (int s = 0; s < structures.size(); s++) {
                                            structure = structures.getJsonObject(s);
                                            if (structure.getString("id").equals(order.getString("id_structure"))) {
                                                orderOffer.put("uai_structure", structure.getString("uai"));
                                                orderOffer.put("name_structure", structure.getString("name"));
                                            }
                                        }
                                        orderRegion.add(orderOffer);
                                        i++;
                                    }
                                }
                            }*/

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
        oldOrderRegionService.getOrdersRegionById(idsOrders, handlerJsonArray(orderRegionFuture));
        structureService.getStructureById(idStructures,handlerJsonArray(structureFuture));
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
            JsonObject offerObject = new JsonObject().put("name", offer.getJsonArray("licence").getJsonObject(0).getString("valeur"));
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
            offerObject.put("titre", offer.getString("titre"));
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
            report.append(generateExportLine(request, logs.getJsonObject(i)));
        }
        return report.toString();
    }

    private static String getExportHeader (HttpServerRequest request) {
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

    private static String generateExportLine (HttpServerRequest request, JsonObject log) {
        return  (log.getInteger("id") != null ? log.getInteger("id").toString() : "") + ";" +
                (log.getString("creation_date") != null ? log.getString("creation_date") : "") + ";" +
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
                (log.getDouble("unitedPriceTTC") != null ? convertPriceString(log.getDouble("unitedPriceTTC")) : "") + ";" +
                (log.getDouble("totalPriceHT") != null ? convertPriceString(log.getDouble("totalPriceHT")) : "") + ";" +
                (log.getDouble("totalPriceTTC") != null ? convertPriceString(log.getDouble("totalPriceTTC")) : "") + ";" +
                (log.getString("comment") != null ? log.getString("comment") : "")
                + "\n";
    }

    private static String convertPriceString(double price) {
        String priceString = "";
        if(price == 0) {
            priceString = "GRATUIT";
        } else {
            priceString = Double.toString(price);
        }
        return priceString;
    }
}
