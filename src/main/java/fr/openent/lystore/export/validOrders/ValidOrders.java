package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.validOrders.BC.*;
import fr.openent.lystore.export.validOrders.listLycee.ListLycee;
import fr.openent.lystore.export.validOrders.listLycee.RecapListLycee;
import fr.openent.lystore.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class ValidOrders extends ExportObject {
    private String bcNumber;
    private String numberValidation="";
    private JsonObject params;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(ValidOrders.class);
    private String idFile;
    private SupplierService supplierService;
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;

    public ValidOrders(ExportService exportService, String idNewFile,EventBus eb, Vertx vertx, JsonObject config){
        super(exportService,idNewFile);
        this.vertx = vertx;
        this.config = config;
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");

    }
    public ValidOrders(ExportService exportService, String param, String idNewFile,EventBus eb, Vertx vertx, JsonObject config,boolean HasNumberValidation) {
        this(exportService,idNewFile,eb,vertx,config);
        if(HasNumberValidation)
            this.numberValidation = param;
        else
            this.bcNumber = param;

    }
    public ValidOrders(ExportService exportService, JsonObject params, String idNewFile,EventBus eb, Vertx vertx, JsonObject config) {
        this(exportService,idNewFile,eb,vertx,config);
        this.params = params;
    }

    public void exportListLycee(Handler<Either<String, Buffer>> handler) {
        if (this.numberValidation == null || this.numberValidation.equals("")) {
            ExportHelper.catchError(exportService, idFile, "number validation is not nullable");
            handler.handle(new Either.Left<>("number validation is not nullable"));
        }
        Workbook workbook = new XSSFWorkbook();
        List<Future> futures = new ArrayList<>();
        Future<Boolean> ListLyceeFuture = Future.future();
        Future<Boolean> RecapListLyceeFuture = Future.future();

        futures.add(ListLyceeFuture);
        futures.add(RecapListLyceeFuture);
        futureHandler(handler, workbook, futures);
        new ListLycee(workbook, this.numberValidation).create(getHandler(ListLyceeFuture));
        new RecapListLycee(workbook, this.numberValidation).create(getHandler(RecapListLyceeFuture));

    }





    public void exportBC(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            new BCExport(eb,vertx,config).create(params.getJsonArray("numberValidations"),handler);
        }
    }





    public void exportBCDuringValidation(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            new BCExportDuringValidation(eb,vertx,config).create(params,handler);
        }

    }
    public void exportBCAfterValidationByStructures(Handler<Either<String, Buffer>> handler) {

        if (this.bcNumber == null || this.bcNumber.isEmpty()) {

            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            new BCExportAfterValidationStructure(eb,vertx,config).create(bcNumber,handler);
        }
    }
    public void exportBCBeforeValidationByStructures(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            new BCExportBeforeValidationStructure(eb,vertx,config).create(params.getJsonArray("numberValidations"),handler);
        }
    }
    public void exportBCAfterValidation(Handler<Either<String, Buffer>> handler) {
        if (this.bcNumber == null || this.bcNumber.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            new BCExportAfterValidation(eb,vertx,config).create(bcNumber,handler);
        }
    }
}
