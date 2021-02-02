package fr.openent.crre.export.validOrders;

import fr.openent.crre.export.ExportObject;
import fr.openent.crre.export.validOrders.listLycee.ListLycee;
import fr.openent.crre.export.validOrders.listLycee.RecapListLycee;
import fr.openent.crre.helpers.ExportHelper;
import fr.openent.crre.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class ValidOrders extends ExportObject {
    private String numberValidation="";

    public ValidOrders(ExportService exportService, String idNewFile){
        super(exportService,idNewFile);

    }
    public ValidOrders(ExportService exportService, String param, String idNewFile,boolean HasNumberValidation) {
        this(exportService,idNewFile);
        if(HasNumberValidation)
            this.numberValidation = param;
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
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }

    public void exportBCDuringValidation(Handler<Either<String, Buffer>> handler) {
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }
    public void exportBCAfterValidationByStructures(Handler<Either<String, Buffer>> handler) {
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }
    public void exportBCBeforeValidationByStructures(Handler<Either<String, Buffer>> handler) {
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }
    public void exportBCAfterValidation(Handler<Either<String, Buffer>> handler) {
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }
}
