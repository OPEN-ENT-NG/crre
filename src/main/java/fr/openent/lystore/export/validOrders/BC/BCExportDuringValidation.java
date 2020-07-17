package fr.openent.lystore.export.validOrders.BC;


import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;

import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.service.ProgramService;
import fr.openent.lystore.service.impl.DefaultProgramService;
import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;


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
        final Number programId = params.getInteger("programId");
        getOrdersData(exportHandler,nbrBc, nbrEngagement, dateGeneration, supplierId, ids,false,
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject data) {
                        data.put("print_order", true);
                        data.put("print_certificates", false);

                        generatePDF( exportHandler,data,
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
    }
}

