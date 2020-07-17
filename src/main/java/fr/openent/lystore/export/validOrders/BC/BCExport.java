package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.controllers.OrderController;
//import fr.openent.lystore.helpers.RendersHelper;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.openent.lystore.helpers.RendersHelper;
import fr.openent.lystore.service.AgentService;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http2ServerRequestImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;

import static fr.wseduc.webutils.http.Renders.badRequest;


public class BCExport extends PDF_OrderHElper {
    private String node;
    private Logger log = LoggerFactory.getLogger(BCExport.class);


    public BCExport(EventBus eb, Vertx vertx, JsonObject config)

    {
        super(eb,vertx,config);

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
                                            "BC.xhtml", "CSF_",
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
}
