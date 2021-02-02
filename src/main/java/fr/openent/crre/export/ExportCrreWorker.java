package fr.openent.crre.export;

import fr.openent.crre.export.validOrders.ValidOrders;
import fr.openent.crre.helpers.ExportHelper;
import fr.openent.crre.service.ExportService;
import fr.openent.crre.service.impl.DefaultExportServiceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import static fr.openent.crre.Crre.*;

public class ExportCrreWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private ValidOrders validOrders;
    private Storage storage;
    private final ExportService exportService = new DefaultExportServiceService(storage);
    private String idNewFile;
    private boolean isSleeping = true;
    private final String XLSXHEADER= "application/vnd.ms-excel";
    private final String PDFHEADER = "application/pdf";

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportCrreWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.storage = STORAGE;

    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));
        if (isSleeping) {
            logger.info("Calling Worker");
            isSleeping = false;
            processExport();
        }
    }




    private void processExport(){

        Handler<Either<String,Boolean>> exportHandler = event -> {
            logger.info("exportHandler");
            if (event.isRight()) {
                logger.info("export to Waiting");
                processExport();
            } else {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx " + event.left().getValue());
            }
        };
        exportService.getWaitingExport(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    JsonObject waitingOrder = event.right().getValue();
                    chooseExport( waitingOrder,exportHandler);
                }else{
                    isSleeping = true;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    processExport();
                                }
                            },
                            3600*1000
                    );
                }
            }
        });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        String fileName = body.getString("filename");
        idNewFile = body.getString("_id");
        Integer object_id = -1;
        String string_object_id ="";
        JsonObject params = body.getJsonObject("externalParams");
        try {
            object_id = Integer.parseInt(body.getString("object_id"));
            string_object_id = object_id.toString();
        }catch (ClassCastException ce){
            object_id = body.getInteger("object_id");
            string_object_id = object_id.toString();
        }catch (NumberFormatException ce){
            string_object_id = body.getString("object_id");

        }
        switch (action) {
            case ExportTypes.LIST_LYCEE:
                exportListLycOrders(string_object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_BEFORE_VALIDATION:
                exportBCOrders(
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_DURING_VALIDATION:
                exportBCOrdersDuringValidation(fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_AFTER_VALIDATION:
                exportBCOrdersAfterValidation(string_object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_AFTER_VALIDATION_STRUCT:
                exportBCOrdersAfterValidationStruct(string_object_id,fileName,exportHandler);
                break;
            case ExportTypes.BC_BEFORE_VALIDATION_STRUCT:
                exportBCOrdersBeforeValidationStruct(fileName,exportHandler);
                break;
            default:
                ExportHelper.catchError(exportService, idNewFile, "Invalid action in worker : " + action,exportHandler);
                break;
        }
    }

    private void exportBCOrdersBeforeValidationStruct( String titleFile, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export BC per structures from Orders before validation started : ");

        this.validOrders = new ValidOrders(exportService,idNewFile);
        this.validOrders.exportBCBeforeValidationByStructures(event1 -> {
            saveExportHandler(titleFile, exportHandler, event1, "error when creating BCOrdersBeforeValidationStruct PDF ", PDFHEADER);
        });
    }
    private void exportBCOrdersAfterValidationStruct(String object_id, String titleFile, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export BC per structures from Orders after validation started BC : "+ object_id);

        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,false);
        this.validOrders.exportBCAfterValidationByStructures(event1 -> {
            saveExportHandler(titleFile, exportHandler, event1, "error when creating BCOrdersAfterValidationStruct PDF ", PDFHEADER);
        });
    }

    private void exportBCOrdersAfterValidation(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC per structures from Orders after validation started BC : "+ object_id);

        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,false);
        this.validOrders.exportBCAfterValidation(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCOrdersAfterValidation PDF ", PDFHEADER);
        });
    }

    private void exportBCOrdersDuringValidation(String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders during validation started");
        this.validOrders = new ValidOrders(exportService,idNewFile);
        this.validOrders.exportBCDuringValidation(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCOrdersDuringValidation PDF ", PDFHEADER);
        });
    }

    private void exportBCOrders(String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders started");
        this.validOrders = new ValidOrders(exportService,idNewFile);
        this.validOrders.exportBC(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCorders PDF ", PDFHEADER);
        });
    }

    private void exportListLycOrders(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export list lycee from Orders started");
        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,true);
        this.validOrders.exportListLycee(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating ListLycOrder xlsx :", XLSXHEADER);
        });

    }

    private void saveBuffer(Buffer buff, String fileName,Handler<Either<String,Boolean>> handler,String fileType) {
        storage.writeBuffer(buff, fileType, fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                ExportHelper.catchError(exportService, idNewFile, "An error occurred when inserting xlsx ",handler);
                handler.handle(new Either.Left<>("An error occurred when inserting xlsx"));
            } else {
                logger.info(fileName + " insert in storage");
                exportService.updateWhenSuccess(file.getString("_id"), idNewFile,handler);
            }
        });
    }

    private void saveExportHandler(String titleFile, Handler<Either<String, Boolean>> handler, Either<String, Buffer> event1, String errorMessage, String fileType) {
        if (event1.isLeft()) {
            ExportHelper.catchError(exportService, idNewFile, errorMessage +"\n" + event1.left().getValue(), handler);
        } else {
            logger.info(titleFile + " created ");
            Buffer buffer = event1.right().getValue();
            saveBuffer(buffer, titleFile, handler, fileType);
        }
    }

}
