package fr.openent.crre.export.validOrders.BC;


import fr.openent.crre.export.validOrders.PDF_OrderHElper;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class BCExportDuringValidation extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);
    public BCExportDuringValidation(EventBus eb, Vertx vertx, JsonObject config){
        super(eb,vertx,config);
    }





    public void create(JsonObject params, Handler<Either<String, Buffer>> exportHandler){
        final JsonArray ids = params.getJsonArray("ids");
        final String nbrBc = params.getString("nbrBc");
        final String nbrEngagement = params.getString("nbrEngagement");
        final String dateGeneration = params.getString("dateGeneration");
        Number supplierId = params.getInteger("supplierId");
        getOrdersData(exportHandler,nbrBc, nbrEngagement, dateGeneration, supplierId, ids,false,
                data -> {
                    data.put("print_order", true);
                    data.put("print_certificates", false);

                    generatePDF( exportHandler,data,
                            "BC.xhtml", "Bon_Commande_",
                            pdf -> exportHandler.handle(new Either.Right<>(pdf))
                    );
                });
    }
}

