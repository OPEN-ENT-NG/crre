package fr.openent.crre.export.validOrders;

import fr.openent.crre.Crre;
import fr.openent.crre.controllers.OrderController;
import fr.openent.crre.export.validOrders.BC.BCExport;
import fr.openent.crre.helpers.RendersHelper;
import fr.openent.crre.service.AgentService;
import fr.openent.crre.service.OrderService;
import fr.openent.crre.service.StructureService;
import fr.openent.crre.service.SupplierService;
import fr.openent.crre.service.impl.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;

import static fr.openent.crre.utils.OrderUtils.*;
import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.renderError;

public class PDF_OrderHElper {
    protected SupplierService supplierService;
    protected JsonObject config;
    protected Vertx vertx;
    protected EventBus eb;
    protected String node;
    protected Logger log = LoggerFactory.getLogger(BCExport.class);
    protected OrderService orderService;
    protected DefaultContractService contractService;
    protected StructureService structureService;
    protected AgentService agentService;
    protected RendersHelper renders ;

    public PDF_OrderHElper(EventBus eb, Vertx vertx, JsonObject config){
        this.vertx = vertx;
        this.config = config;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Crre.crreSchema, "supplier");
        this.orderService = new DefaultOrderService(Crre.crreSchema, "order_client_equipment", emailSender);
        this.structureService = new DefaultStructureService(Crre.crreSchema);
        this.supplierService = new DefaultSupplierService(Crre.crreSchema, "supplier");
        this.contractService = new DefaultContractService(Crre.crreSchema, "contract");
        this.agentService = new DefaultAgentService(Crre.crreSchema, "agent");
        this.renders = new RendersHelper(this.vertx, config);

    }

    protected void retrieveOrderDataForCertificate(final Handler<Either<String, Buffer>> exportHandler, final JsonObject structures,
                                                   final Handler<JsonArray> handler) {
        JsonObject structure;
        String structureId;
        Iterator<String> structureIds = structures.fieldNames().iterator();
        final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
        if(!structureIds.hasNext()){
            exportHandler.handle(new Either.Left<>("no structure get"));
        }
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
                                    .put("orders", OrderController.formatOrders(event.right().getValue()))
                            );
                            if (result.size() == structures.size()) {
                                handler.handle(result);
                            }
                        } else {

                            log.error("An error occurred when collecting orders for certificates");
                            exportHandler.handle(new Either.Left<>("An error occured when collecting orders for certificates"));
                        }
                    });
        }
    }

    protected void retrieveContract(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                    final Handler<JsonObject> handler) {
        contractService.getContract(ids, event -> {
            if (event.isRight() && event.right().getValue().size() == 1) {
                handler.handle(event.right().getValue().getJsonObject(0));
            } else {
                exportHandler.handle(new Either.Left<>("An error occured when collecting contract data"));
                log.error("An error occured when collecting contract data");
            }
        });
    }

    public void retrieveStructures(final Handler<Either<String, Buffer>> exportHandler, final HttpServerRequest request, JsonArray ids,
                                   final Handler<JsonObject> handler) {
        orderService.getStructuresId(ids, event -> {
            if (event.isRight()) {
                JsonArray structures = event.right().getValue();
                JsonArray structuresList = new fr.wseduc.webutils.collections.JsonArray();
                final JsonObject structureMapping = new JsonObject();
                JsonObject structure;
                JsonObject structureInfo;
                JsonArray orderIds;
                for (int i = 0; i < structures.size(); i++) {
                    structure = structures.getJsonObject(i);
                    if (!structuresList.contains(structure.getString("id_structure"))) {
                        structuresList.add(structure.getString("id_structure"));
                        structureInfo = new JsonObject();
                        structureInfo.put("orderIds", new fr.wseduc.webutils.collections.JsonArray());
                    } else {
                        structureInfo = structureMapping.getJsonObject(structure.getString("id_structure"));
                    }
                    orderIds = structureInfo.getJsonArray("orderIds");
                    orderIds.add(structure.getInteger("id"));
                    structureMapping.put(structure.getString("id_structure"), structureInfo);
                }
                structureService.getStructureById(structuresList, event1 -> {
                    if (event1.isRight()) {
                        JsonArray structures1 = event1.right().getValue();
                        JsonObject structure1;
                        for (int i = 0; i < structures1.size(); i++) {
                            structure1 = structures1.getJsonObject(i);
                            JsonObject structureObject = structureMapping.getJsonObject(structure1.getString("id"));
                            structureObject.put("structureInfo", structure1);
                        }
                        handler.handle(structureMapping);
                    } else {
                        log.error("An error occurred when collecting structures based on ids");
                        if(exportHandler != null)
                            exportHandler.handle(new Either.Left<>("An error occurred when collecting structures based on ids"));
                        else
                            badRequest(request);
                    }
                });
            } else {
                log.error("An error occurred when getting structures id based on order ids.");
                if(exportHandler != null)
                    exportHandler.handle(new Either.Left<>("An error occurred when getting structures id based on order ids."));
                else
                    renderError(request);

            }
        });
    }

    protected void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,boolean groupByStructure,
                                     final Handler<JsonObject> handler) {
        orderService.getOrders(ids, null, true, groupByStructure, event -> {
            if (event.isRight()) {
                JsonObject order = new JsonObject();
                JsonArray orders = OrderController.formatOrders(event.right().getValue());
                order.put("orders", orders);
                Double sumWithoutTaxes = getSumWithoutTaxes(orders);
                Double taxTotal = getTaxesTotal(orders);
                order.put("sumLocale",
                        OrderController.getReadableNumber(OrderController.roundWith2Decimals(sumWithoutTaxes)));
                order.put("totalTaxesLocale",
                        OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal)));
                order.put("totalPriceTaxeIncludedLocal",
                        OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal + sumWithoutTaxes)));
                handler.handle(order);

            } else {
                log.error("An error occurred when retrieving order data");
                exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
            }
        });
    }
    protected void retrieveManagementInfo(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
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
                        exportHandler.handle(new Either.Left<>("An error occurred when collecting supplier data"));
                    }
                });
            } else {
                log.error("An error occured when collecting user information");
                exportHandler.handle(new Either.Left<>("An error occured when collecting user information"));
            }
        });
    }
    protected void getOrdersData(final Handler<Either<String, Buffer>> exportHandler, final String nbrBc,
                                 final String nbrEngagement, final String dateGeneration,
                                 final Number supplierId, final JsonArray ids, boolean groupByStructure,
                                 final Handler<JsonObject> handler) {
        SimpleDateFormat formatterDatePDF = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        final JsonObject data = new JsonObject();

        retrieveManagementInfo(exportHandler, ids, supplierId,
                managmentInfo -> retrieveStructures(exportHandler, null, ids,
                        structures -> retrieveOrderData(exportHandler, ids,groupByStructure,
                                order -> retrieveOrderDataForCertificate(exportHandler, structures,
                                        certificates -> retrieveContract(exportHandler, ids,
                                                contract -> {
                                                    putInfosInData(nbrBc, nbrEngagement, data, managmentInfo, order, certificates, contract);
                                                    Date orderDate = null;
                                                    try {
                                                        orderDate = formatterDate.parse(dateGeneration);
                                                    } catch (ParseException e) {
                                                        log.error("Incorrect date format");
                                                    }
                                                    String date ;
                                                    try{
                                                        date =  formatterDatePDF.format(orderDate);
                                                    }catch (NullPointerException e){
                                                        date = dateGeneration;
                                                    }
                                                    data.put("date_generation",date);
                                                    handler.handle(data);
                                                })))));
    }

    public void create(String nbrBc, Boolean isStructure, Handler<Either<String, Buffer>> exportHandler) {
        getOrdersDataSql(nbrBc, event -> {
                    if (event.isRight()) {
                        JsonArray paramstemp = event.right().getValue();
                        JsonObject params = paramstemp.getJsonObject(0);
                        final JsonArray ids = new JsonArray();
                        JsonArray idsArray =  new JsonArray(params.getString("ids"));
                        for(int i = 0 ; i < idsArray.size();i++){
                            ids.add(idsArray.getValue(i).toString());
                        }
                        final String nbrEngagement = params.getString("nbr_engagement");
                        final String dateGeneration = params.getString("date_generation");
                        Number supplierId = params.getInteger("supplier_id");
                        getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids,true,
                                data -> {
                                    data.put("print_order", true);
                                    data.put("print_certificates", false);
                                    String templateName;
                                    if(isStructure){
                                        templateName = "BC_Struct.xhtml";
                                    }else{
                                        templateName = "BC.xhtml";
                                    }
                                    generatePDF(exportHandler, null, data, templateName,
                                            pdf -> exportHandler.handle(new Either.Right<>(pdf))
                                    );
                                });
                    }else{
                        exportHandler.handle(new Either.Left<>("sql failed"));
                    }
                }

        );

    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String,JsonArray>> handler) {
        String query = "SELECT ord.engagement_number AS nbr_engagement, " +
                "       ord.date_creation     AS date_generation, " +
                "       supplier.id           AS supplier_id, " +
                "       array_to_json(Array_agg(DISTINCT oce.number_validation)) as ids " +
                "FROM   crre.order ord " +
                "       INNER JOIN crre.order_client_equipment oce " +
                "               ON oce.id_order = ord.id " +
                "       LEFT JOIN crre.contract " +
                "              ON contract.id = oce.id_contract " +
                "       INNER JOIN crre.supplier " +
                "               ON contract.id_supplier = supplier.id " +
                "WHERE  ord.order_number = ? " +
                "GROUP  BY ord.engagement_number, " +
                "          ord.date_creation, " +
                "          supplier_id "
                ;

        Sql.getInstance().prepared(query, new JsonArray().add(nbrbc), new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }

    public void generatePDF(Handler<Either<String, Buffer>> exportHandler, final HttpServerRequest request,
                            final JsonObject templateProps, final String templateName,
                            final Handler<Buffer> handler) {

        final JsonObject exportConfig = config.getJsonObject("exports");
        final String templatePath = exportConfig.getString("template-path");
        final String baseUrl = config.getString("host") +
                config.getString("app-address") + "/public/";
        final String logo = exportConfig.getString("logo-path");
        node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }
        final String path = FileResolver.absolutePath(templatePath + templateName);
        final String logoPath = FileResolver.absolutePath(logo);
        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                if(exportHandler == null)
                    badRequest(request);
                return;
            }
            Buffer logoBuffer = vertx.fileSystem().readFileBlocking(logoPath);
            String encodedLogo = "";
            encodedLogo = new String(Base64.getMimeEncoder().encode(logoBuffer.getBytes()), StandardCharsets.UTF_8);
            templateProps.put("logo-data", encodedLogo);
            StringReader reader = new StringReader(result.result().toString("UTF-8"));
            renders.processTemplate(exportHandler, templateProps, templateName, reader, writer -> {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                JsonObject actionObject = new JsonObject();
                byte[] bytes;
                bytes = processedTemplate.getBytes(StandardCharsets.UTF_8);
                actionObject
                        .put("content", bytes)
                        .put("baseUrl", baseUrl);
                eb.send(node + "entcore.pdf.generator", actionObject, (Handler<AsyncResult<Message<JsonObject>>>) reply -> {
                    JsonObject pdfResponse = reply.result().body();
                    if (!"ok".equals(pdfResponse.getString("status"))) {
                        if(exportHandler == null)
                            badRequest(request, pdfResponse.getString("message"));
                        else
                            exportHandler.handle(new Either.Left<>("wrong status when calling bus (pdf) "));
                        return;
                    }
                    byte[] pdf = pdfResponse.getBinary("content");
                    Buffer either = Buffer.buffer(pdf);
                    handler.handle(either);
                });
            });
        });

    }
}
