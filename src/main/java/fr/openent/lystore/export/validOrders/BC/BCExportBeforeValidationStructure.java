package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.controllers.OrderController;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BCExportBeforeValidationStructure extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExportBeforeValidationStructure.class);

    public BCExportBeforeValidationStructure(EventBus eb, Vertx vertx, JsonObject config) {
        super(eb, vertx, config);
    }



    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler){
        List<String> validationNumbers = validationNumbersArray.getList();
        supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    JsonObject supplier = event.right().getValue();
                    getOrdersData( exportHandler,"", "", "", supplier.getInteger("id"), new fr.wseduc.webutils.collections.JsonArray(validationNumbers),false,
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject data) {
                                    data.put("print_order", true);
                                    data.put("print_certificates", false);
                                    generatePDF(exportHandler, data,
                                            "BC_Struct.xhtml", "CSF_",
                                            new Handler<Buffer>() {
                                                @Override
                                                public void handle(final Buffer pdf) {
                                                    exportHandler.handle(new Either.Right(pdf));
                                                }
                                            }
                                    );
                                }
                            });
                }else {
                    log.error("error when getting supplier");
                }
            }
        });
    }



    @Override
    protected void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,boolean groupByStructure,
                                     final Handler<JsonObject> handler) {
        orderService.getOrders(ids, null, true, true, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject order = new JsonObject();
                    ArrayList<String> listStruct = new ArrayList<>();
                    JsonArray orders = OrderController.formatOrders(event.right().getValue());
                    orders = sortByUai(orders);

                    sortOrdersBySturcuture(order, listStruct, orders);

                    getSubtotalByStructure(order, listStruct);

                    structureService.getStructureById(new JsonArray(listStruct), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray structures = event.right().getValue();
                                JsonObject structure ;
                                for (int i = 0; i < structures.size(); i++) {
                                    structure = structures.getJsonObject(i);
                                    JsonObject ordersByStructure = order.getJsonObject(structure.getString("id"));
                                    ordersByStructure.put("name",structure.getString("name"));
                                    ordersByStructure.put("uai",structure.getString("uai"));
                                    ordersByStructure.put("address",structure.getString("address"));
                                    ordersByStructure.put("phone",structure.getString("phone"));
                                    order.put(structure.getString("id"),ordersByStructure);

                                }
                                JsonArray ordersArray = new JsonArray();
                                setOrdersToArray(ordersArray, listStruct, order);
                                handler.handle(order);
                            } else {
                                log.error("An error occurred when collecting structures based on ids");
                                exportHandler.handle(new Either.Left<>("An error occurred when collecting structures based on ids"));

                            }
                        }
                    });

                } else {
                    log.error("An error occurred when retrieving order data");
                    exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
                }
            }
        });
    }

    private void sortOrdersBySturcuture(JsonObject order, ArrayList<String> listStruct, io.vertx.core.json.JsonArray
            orders) {
        for(int i=0;i<orders.size();i++){
            JsonObject orderSorted = orders.getJsonObject(i);
            String idStruct = orderSorted.getString("id_structure");
            if(order.containsKey(idStruct)){
                JsonArray tempOrders = order.getJsonObject(idStruct).getJsonArray("orders").add(orderSorted);
                order.put(orderSorted.getString("id_structure"),new JsonObject().put("orders",tempOrders));
            }else{
                listStruct.add(idStruct);
                order.put(orderSorted.getString("id_structure"),new JsonObject().put("orders", new JsonArray().add(orderSorted)));
            }
        }
    }

    private void getSubtotalByStructure(JsonObject order, ArrayList<String> listStruct) {
        for (String s : listStruct) {
            JsonObject ordersByStructure = order.getJsonObject(s);
            Double sumWithoutTaxes = getSumWithoutTaxes(ordersByStructure.getJsonArray("orders"));
            Double taxTotal = getTaxesTotal(ordersByStructure.getJsonArray("orders"));

            ordersByStructure.put("sumLocale",
                    OrderController.getReadableNumber(OrderController.roundWith2Decimals(sumWithoutTaxes)));
            ordersByStructure.put("totalTaxesLocale",
                    OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal)));
            ordersByStructure.put("totalPriceTaxeIncludedLocal",
                    OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal + sumWithoutTaxes)));
            order.put(s, ordersByStructure);
        }
    }

    private void setOrdersToArray(io.vertx.core.json.JsonArray ordersArray, ArrayList<String> listStruct, JsonObject order) {
        for (String idStruct : listStruct) {
            JsonObject ordersJsonObject = order.getJsonObject(idStruct);
            ordersArray.add(ordersJsonObject);
            order.remove(idStruct);
        }
        order.put("orderArray",ordersArray);
    }

    private JsonArray sortByUai(io.vertx.core.json.JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < values.size(); i++) {
            jsonValues.add(values.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "id_structure";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                        valA = a.getString(KEY_NAME);
                    }
                    if (b.containsKey(KEY_NAME)) {
                        valB = b.getString(KEY_NAME);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting structures during export");
                }

                return valA.compareTo(valB);
            }
        });

        for (int i = 0; i < values.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String, io.vertx.core.json.JsonArray >> handler) {
        String query = "SELECT ord.engagement_number AS nbr_engagement, " +
                "       ord.date_creation     AS date_generation, " +
                "       supplier.id           AS supplier_id, " +
                "       array_to_json(Array_agg(DISTINCT oce.number_validation)) as ids " +
                "FROM   lystore.order ord " +
                "       INNER JOIN lystore.order_client_equipment oce " +
                "               ON oce.id_order = ord.id " +
                "       LEFT JOIN lystore.contract " +
                "              ON contract.id = oce.id_contract " +
                "       INNER JOIN lystore.supplier " +
                "               ON contract.id_supplier = supplier.id " +
                "WHERE  ord.order_number = ? " +
                "GROUP  BY ord.engagement_number, " +
                "          ord.date_creation, " +
                "          supplier_id "
                ;

        Sql.getInstance().prepared(query, new JsonArray().add(nbrbc), new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }
}

