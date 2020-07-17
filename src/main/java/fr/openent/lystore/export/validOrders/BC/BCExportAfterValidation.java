package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class BCExportAfterValidation  extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportAfterValidation(EventBus eb, Vertx vertx, JsonObject config) {
        super(eb, vertx, config);
    }


    public void create(String nbrBc, Handler<Either<String, Buffer>> exportHandler) {
        getOrdersDataSql(nbrBc,new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
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
                            getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids,false,
                                    new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject data) {
                                            data.put("print_order", true);
                                            data.put("print_certificates", false);
                                            generatePDF(exportHandler, data,
                                                    "BC.xhtml", "Bon_Commande_",
                                                    new Handler<Buffer>() {
                                                        @Override
                                                        public void handle(final Buffer pdf) {
                                                            exportHandler.handle(new Either.Right<>(pdf));
                                                        }
                                                    }
                                            );
                                        }
                                    });
                        }else{
                            exportHandler.handle(new Either.Left<>("sql failed"));
                        }
                    }
                }

        );

    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String,JsonArray>> handler) {
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
