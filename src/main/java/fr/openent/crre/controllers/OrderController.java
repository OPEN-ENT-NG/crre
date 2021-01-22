package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.export.ExportTypes;
import fr.openent.crre.export.validOrders.PDF_OrderHElper;
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
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
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
import java.text.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static fr.openent.crre.helpers.ElasticSearchHelper.searchByIds;
import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;
import static fr.openent.crre.utils.OrderUtils.*;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static java.lang.Integer.parseInt;


public class OrderController extends ControllerHelper {

    private final Storage storage;
    private final OrderService orderService;
    private final StructureService structureService;
    private final SupplierService supplierService;
    private final ContractService contractService;
    private final AgentService agentService;
    private final ExportService exportService;

    public static final String UTF8_BOM = "\uFEFF";

    private static final DecimalFormat decimals = new DecimalFormat("0.00");

    public OrderController (Storage storage, Vertx vertx, JsonObject config, EventBus eb) {
        this.storage = storage;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.orderService = new DefaultOrderService(Crre.crreSchema, "order_client_equipment", emailSender);
        this.structureService = new DefaultStructureService(Crre.crreSchema);
        this.supplierService = new DefaultSupplierService(Crre.crreSchema, "supplier");
        this.contractService = new DefaultContractService(Crre.crreSchema, "contract");
        this.agentService = new DefaultAgentService(Crre.crreSchema, "agent");
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

    @Put("/order/rank/move")
    @ApiDoc("Update the rank of tow orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void updatePriority(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, campaign -> {
            if (!campaign.containsKey("orders")) {
                badRequest(request);
                return;
            }
            JsonArray orders = campaign.getJsonArray("orders");
            try{
                orderService.updateRank(orders, defaultResponseHandler(request));
            }catch(Exception e){
                log.error(" An error occurred when casting campaign id", e);
            }
        });
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
                        orderService.getOrdersDetailsIndexedByValidationNumber(statusList, event1 -> {
                            if (event1.isRight()) {
                                JsonArray equipments = event1.right().getValue();
                                JsonObject mapNumberEquipments = initNumbersMap(orders);
                                mapNumbersEquipments(equipments, mapNumberEquipments);
                                JsonObject order;
                                for (int i = 0; i < orders.size(); i++) {
                                    order = orders.getJsonObject(i);
                                    order.put("price",
                                            decimals.format(
                                                    roundWith2Decimals(getTotalOrder(mapNumberEquipments.getJsonArray(order.getString("number_validation")))))
                                                    .replace(".", ","));
                                }
                                renderJson(request, orders);
                            } else {
                                badRequest(request);
                            }
                        });
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

    @Get("/order/struct")
    @ApiDoc("Get the pdf of orders by structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrderPDFStruct (final HttpServerRequest request) {
        final String orderNumber = request.params().get("bc_number");
        try {
            if(!request.getParam("bc_number").isEmpty()) {
                ExportHelper.makeExport(request, eb, exportService, Crre.ORDERSSENT, Crre.PDF, ExportTypes.BC_AFTER_VALIDATION_STRUCT, "_STRUCTURES_BC_" + orderNumber);
            }

        }catch (NullPointerException e){
            ExportHelper.makeExport(request,eb,exportService, Crre.ORDERSSENT,  Crre.PDF,ExportTypes.BC_BEFORE_VALIDATION_STRUCT, "_STRUCTURES_BC" );
        }
//
    }
    /**
     * Init map with numbers validation
     * @param orders order list containing numbers
     * @return Map containing numbers validation as key and an empty array as value
     */
    private JsonObject initNumbersMap (JsonArray orders) {
        JsonObject map = new JsonObject();
        JsonObject item;
        for (int i = 0; i < orders.size(); i++) {
            item = orders.getJsonObject(i);
            try {
                map.put(item.getString("number_validation"), new fr.wseduc.webutils.collections.JsonArray());
            }catch (NullPointerException e){
                log.error("Number validation is null");
            }
        }
        return map;
    }

    /**
     * Map equipments with numbers validation
     * @param equipments Equipments list
     * @param numbers Numbers maps
     * @return Map containing number validations as key and an array containing equipments as value
     */
    private JsonObject mapNumbersEquipments (JsonArray equipments, JsonObject numbers) {
        JsonObject equipment;
        JsonArray equipmentList;
        for (int i = 0; i < equipments.size(); i++) {
            equipment = equipments.getJsonObject(i);
            equipmentList = numbers.getJsonArray(equipment.getString("number_validation"));
            numbers.put(equipment.getString("number_validation"), equipmentList.add(equipment));
        }
        return numbers;
    }

    @Delete("/order/:idOrder/:idStructure/:idCampaign")
    @ApiDoc("Delete a order item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void deleteOrder(final HttpServerRequest request){
        try {
            final Integer idOrder = Integer.parseInt(request.params().get("idOrder"));
            final String idStructure = request.params().get("idStructure");
            orderService.deletableOrder(idOrder, deletableEvent -> {
                if (deletableEvent.isRight() && deletableEvent.right().getValue().getInteger("count") == 0) {
                    orderService.orderForDelete(idOrder, order -> {
                        if (order.isRight()) {
                            UserUtils.getUserInfos(eb, request, user -> {
                                orderService.deleteOrder(idOrder, order.right().getValue(), idStructure, user,
                                        Logging.defaultResponseHandler(eb, request, Contexts.ORDER.toString(),
                                                Actions.DELETE.toString(), "idOrder", order.right().getValue()));
                            });
                        }
                    });
                } else {
                    badRequest(request);
                }
            });
        } catch (ClassCastException e){
            log.error("An error occurred when casting order id", e);
            badRequest(request);
        }
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
                                .end(LogController.generateExport(request, orders));
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


    private void logSendingOrder(JsonArray ids, final HttpServerRequest request) {
        orderService.getOrderByValidatioNumber(ids, event -> {
            if (event.isRight()) {
                JsonArray orders = event.right().getValue();
                JsonObject order;
                for (int i = 0; i < orders.size(); i++) {
                    order = orders.getJsonObject(i);
                    Logging.insert(eb, request, Contexts.ORDER.toString(), Actions.UPDATE.toString(),
                            order.getInteger("id").toString(), order);
                }
            }
        });
    }



    private void sentOrders(HttpServerRequest request,
                            final JsonArray ids, final String engagementNumber, final Number programId, final String dateCreation,
                            final String orderNumber) {
/*        programService.getProgramById(programId, (Handler<Either<String, JsonObject>>) programEvent -> {
            if (programEvent.isRight()) {
                JsonObject program = programEvent.right().getValue();
                orderService.updateStatusToSent(ids.getList(), "SENT", engagementNumber, program.getString("name"),
                        dateCreation, orderNumber,  new Handler<Either<String, JsonObject>>() {
                            @Override
                            public void handle(Either<String, JsonObject> event) {
                                if (event.isRight()) {
                                    logSendingOrder(ids,request);
                                    ExportHelper.makeExport(request,eb,exportService, Crre.ORDERSSENT,  Crre.PDF,ExportTypes.BC_DURING_VALIDATION, "_BC");
                                } else {
                                    badRequest(request);
                                }
                            }
                        });
            } else {
                badRequest(request);
            }
        });*/
        badRequest(request);
    }
    @Put("/orders/sent")
    @ApiDoc("send orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void sendOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", orders -> {
            final JsonArray ids = orders.getJsonArray("ids");
            final String nbrBc = orders.getString("bc_number");
            final String nbrEngagement = orders.getString("engagement_number");
            final String dateGeneration = orders.getString("dateGeneration");
            Number supplierId = orders.getInteger("supplierId");
            final Number programId = orders.getInteger("id_program");
            getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId, ids,
                    data -> {
                        data.put("print_order", true);
                        sentOrders(request,ids,nbrEngagement,programId,dateGeneration,nbrBc);
                    });
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

    @Get("/orders/valid/export/:file")
    @ApiDoc("Export valid orders based on validation number and type file")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void csvExport(final HttpServerRequest request) {
        if (request.params().contains("number_validation")) {
            List<String> validationNumbers = request.params().getAll("number_validation");
            switch (request.params().get("file")) {
                case "structure_list": {
                    exportStructuresList(request);
                    break;
                }
                case "certificates": {
                    exportDocuments(request, false, true, validationNumbers);
                    break;
                }
                case "order": {
                    exportDocuments(request, true, false, validationNumbers);
                    break;
                }
                default: {
                    badRequest(request);
                }
            }
        } else {
            badRequest(request);
        }
    }

    private void exportDocuments(final HttpServerRequest request, final Boolean printOrder,
                                 final Boolean printCertificates, final List<String> validationNumbers) {
        if(printOrder){
            ExportHelper.makeExport(request,eb,exportService, Crre.ORDERS,  Crre.PDF, ExportTypes.BC_BEFORE_VALIDATION, "_BC");
        }else {
            supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), event -> {
                if (event.isRight()) {
                    JsonObject supplier = event.right().getValue();
                    getOrdersData(request, "", "", "", supplier.getInteger("id"), new fr.wseduc.webutils.collections.JsonArray(validationNumbers),
                            data -> {
                                data.put("print_certificates", printCertificates);
                                new PDF_OrderHElper(eb,vertx,config).generatePDF(null, request, data,
                                        "BC_CSF.xhtml",
                                        pdf -> request.response()
                                                .putHeader("Content-Type", "application/pdf; charset=utf-8")
                                                .putHeader("Content-Disposition", "attachment; filename="
                                                        + generateExportName(validationNumbers, "" +
                                                        (printCertificates ? "CSF" : "")) + ".pdf")
                                                .end(pdf)
                                );
                            });
                } else {
                    log.error("An error occurred when collecting supplier Id", new Throwable(event.left().getValue()));
                    badRequest(request);
                }
            });
        }
    }

    private String generateExportName(List<String> validationNumbers, String prefix) {
        StringBuilder exportName = new StringBuilder(prefix);
        for (String validationNumber : validationNumbers) {
            exportName.append("_").append(validationNumber);
        }
        return exportName.toString();
    }

    private void exportStructuresList(final HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService, Crre.ORDERS,  Crre.XLSX,ExportTypes.LIST_LYCEE, "_list_bdc");
    }

    @Delete("/orders/valid")
    @ApiDoc("Delete valid orders. Cancel validation. All orders are back to validation state")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void cancelValidOrder(HttpServerRequest request) {
        if (request.params().contains("number_validation")) {
            List<String> numbers = request.params().getAll("number_validation");
            orderService.cancelValidation(new fr.wseduc.webutils.collections.JsonArray(numbers), defaultResponseHandler(request));
        } else {
            badRequest(request);
        }
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

    private void retrieveContract(final HttpServerRequest request, JsonArray ids,
                                  final Handler<JsonObject> handler) {
        contractService.getContract(ids, event -> {
            if (event.isRight() && event.right().getValue().size() == 1) {
                handler.handle(event.right().getValue().getJsonObject(0));
            } else {
                log.error("An error occured when collecting contract data");
                badRequest(request);
            }
        });
    }

    private void retrieveStructures(final HttpServerRequest request, JsonArray ids,
                                    final Handler<JsonObject> handler) {
        new PDF_OrderHElper(eb,vertx,config).retrieveStructures(null, request,ids,handler);
    }

    private void retrieveOrderData(final HttpServerRequest request, JsonArray ids,
                                   final Handler<JsonObject> handler) {
        orderService.getOrders(ids, null, true, false, event -> {
            if (event.isRight()) {
                JsonObject order = new JsonObject();
                JsonArray orders = formatOrders(event.right().getValue());
                order.put("orders", orders);
                Double sumWithoutTaxes = getSumWithoutTaxes(orders);
                Double taxTotal = getTaxesTotal(orders);
                order.put("sumLocale",
                        getReadableNumber(roundWith2Decimals(sumWithoutTaxes)));
                order.put("totalTaxesLocale",
                        getReadableNumber(roundWith2Decimals(taxTotal)));
                order.put("totalPriceTaxeIncludedLocal",
                        getReadableNumber(roundWith2Decimals(taxTotal + sumWithoutTaxes)));
                handler.handle(order);
            } else {
                log.error("An error occurred when retrieving order data");
                badRequest(request);
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

    private Double getTotalOrder(JsonArray orders) {
        double sum = 0D;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += (Double.parseDouble(order.getString("price")) * Integer.parseInt(order.getString("amount"))
                    * (Double.parseDouble(order.getString("tax_amount")) / 100 + 1));
        }

        return sum;
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


    @Get("/orders/preview")
    @ApiDoc("Get orders preview data")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrdersPreviewData(final HttpServerRequest request) {
        MultiMap params = request.params();
        if (!params.contains("ids") && !params.contains("bc_number")
                && !params.contains("engagement_number") && !params.contains("dateGeneration")
                && !params.contains("supplierId")) {
            badRequest(request);
        } else {
            final List<String> ids = params.getAll("ids");
            final String nbrBc = params.get("bc_number");
            final String nbrEngagement = params.get("engagement_number");
            final String dateGeneration = params.get("dateGeneration");
            Number supplierId = Integer.parseInt(params.get("supplierId"));

            getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId,
                    new fr.wseduc.webutils.collections.JsonArray(ids), data -> renderJson(request, data));
        }
    }

    private void getOrdersData(final HttpServerRequest request, final String nbrBc,
                               final String nbrEngagement, final String dateGeneration,
                               final Number supplierId, final JsonArray ids,
                               final Handler<JsonObject> handler) {
        final JsonObject data = new JsonObject();
        retrieveManagementInfo(request, ids, supplierId,
                managmentInfo -> retrieveStructures(request, ids,
                        structures -> retrieveOrderData(request, ids,
                                order -> retrieveOrderDataForCertificate(request, structures,
                                        certificates -> retrieveContract(request, ids,
                                                contract -> retrieveOrderParam(ids,
                                                        event -> {
                                                            putInfosInData(nbrBc, nbrEngagement, data, managmentInfo, order, certificates, contract);
                                                            data.put("date_generation", dateGeneration);
                                                            if(nbrBc.equals("")){
                                                                data.put("nbr_bc",  event.getString("order_number"))
                                                                        .put("nbr_engagement", event.getString("engagement_number"));
                                                            }
                                                            handler.handle(data);
                                                        }))))));
    }

    private void retrieveOrderParam(JsonArray validationNumbers, Handler<JsonObject> jsonObjectHandler) {
        orderService.getOrderBCParams(validationNumbers, event -> {
            if(event.isRight()) {
                jsonObjectHandler.handle(event.right().getValue());
            }

        });
    }

    private void retrieveOrderDataForCertificate(final HttpServerRequest request, final JsonObject structures,
                                                 final Handler<JsonArray> handler) {
        JsonObject structure;
        String structureId;
        Iterator<String> structureIds = structures.fieldNames().iterator();
        final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
        while (structureIds.hasNext()) {
            structureId = structureIds.next();
            structure = structures.getJsonObject(structureId);
            orderService.getOrders(structure.getJsonArray("orderIds"), structureId, false, true,
                    event -> {
                        if (event.isRight() && event.right().getValue().size() > 0) {
                            JsonObject order = event.right().getValue().getJsonObject(0);
                            result.add(new JsonObject()
                                    .put("id_structure", order.getString("id_structure"))
                                    .put("structure", structures.getJsonObject(order.getString("id_structure"))
                                            .getJsonObject("structureInfo"))
                                    .put("orders", formatOrders(event.right().getValue()))
                            );
                            if (result.size() == structures.size()) {
                                handler.handle(result);
                            }
                        } else {
                            log.error("An error occurred when collecting orders for certificates");
                            badRequest(request);
                        }
                    });
        }
    }

    private void retrieveManagementInfo(final HttpServerRequest request, JsonArray ids,
                                        final Number supplierId, final Handler<JsonObject> handler) {
        agentService.getAgentByOrderIds(ids, user -> {
            if (user.isRight()) {
                final JsonObject userObject = user.right().getValue();
                supplierService.getSupplier(supplierId.toString(), supplier -> {
                    if (supplier.isRight()) {
                        JsonObject supplierObject = supplier.right().getValue();
                        handler.handle(
                                new JsonObject()
                                        .put("userInfo", userObject)
                                        .put("supplierInfo", supplierObject)
                        );
                    } else {
                        log.error("An error occurred when collecting supplier data");
                        badRequest(request);
                    }
                });
            } else {
                log.error("An error occured when collecting user information");
                badRequest(request);
            }
        });
    }

    @Put("/orders/done")
    @ApiDoc("Wind up orders ")
    @ResourceFilter(ManagerRight.class)
    public void windUpOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", orders -> {
            try {
                List<String> params = new ArrayList<>();
                for (Object id : orders.getJsonArray("ids")) {
                    params.add(id.toString());
                }
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                orderService.windUpOrders(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.ORDER.toString(),
                        Actions.UPDATE.toString(),
                        params,
                        null)

                );
            } catch (ClassCastException e) {
                log.error("An error occurred when casting order id", e);
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
                                    .end(LogController.generateExport(request, orders));

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

    @Put("/orders/operation/in-progress/:idOperation")
    @ApiDoc("update operation in orders with status in progress")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperationInProgress(final HttpServerRequest request) {
        badRequest(request);
    }

    @Put("/orders/operation/:idOperation")
    @ApiDoc("update operation in orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        badRequest(request);
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

    @Get("/orderClient/:id/order/progress")
    @ApiDoc("get order by id order client ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrderProgress(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        orderService.getOneOrderClient(idOrder,"IN PROGRESS" ,defaultResponseHandler(request));
    }

    @Get("/orderClient/:id/order/waiting")
    @ApiDoc("get order by id order client ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrderWaiting(HttpServerRequest request) {
        int idOrder = Integer.parseInt(request.getParam("id"));
        orderService.getOneOrderClient(idOrder,"WAITING" ,defaultResponseHandler(request));
    }
}
