package fr.openent.lystore.helpers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportTypes;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ExportHelper {

    protected static Logger log = LoggerFactory.getLogger(ExportHelper.class);
    public static String makeTheNameExport(String nameFile,String extension) {
        return getDate() + nameFile + "." +extension;
    }

    public static String makeTheNameExport(String nameFile,String extension, String type) {
        return getDate() + nameFile + type + "." +extension;
    }

    public static void catchError(ExportService exportService, String idFile, Exception errorCatch) {
        exportService.updateWhenError(idFile, makeError -> {
            if (makeError.isLeft()) {
                log.info("1");

                log.error("Error for create file export excel " + makeError.left()+ " " + errorCatch.getMessage());
            }
        });
        log.error("Error for create file export excel " + errorCatch.getMessage());
    }
    public static void catchError(ExportService exportService, String idFile, String errorCatchTextOutput) {
        exportService.updateWhenError(idFile, makeError -> {
            if (makeError.isLeft()) {
                log.info("2");
                log.error("Error for create file export excel " + makeError.left().getValue() + errorCatchTextOutput);
            }
        });
        log.error("Error for create file export excel " + errorCatchTextOutput);
    }
    public static void catchError(ExportService exportService, String idFile, String errorCatchTextOutput, Handler<Either<String,Boolean>> handler) {
        exportService.updateWhenError(idFile,handler);

        log.error("Error for create file export excel :\n" + errorCatchTextOutput);
    }

    private static String getDate() {
        java.util.Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return formatter.format(date);
    }

    public static void makeExport(HttpServerRequest request, EventBus eb, ExportService exportService, String typeObject, String extension, String
            action, String name) {
        String id="-1";
        JsonObject params = new JsonObject();
        Boolean multipleExport = false;
        boolean withType = request.getParam("type") != null;

        GetIdAndMutliExport getIdAndMutliExport = new GetIdAndMutliExport(request, typeObject, id, params, multipleExport).invoke();
        id = getIdAndMutliExport.getId();
        multipleExport = getIdAndMutliExport.getMultipleExport();

        String type = "";
        JsonObject infoFile = new JsonObject();

        if (withType) {
            type = request.getParam("type");
            infoFile.put("type", type);
        }

        log.info("makeExportExcel");
        String finalId = id;


        JsonObject finalParams = params;


        if(action.equals("exportBCOrdersDuringValidation")){
            getParamsDuringBCValidation(eb,request,exportService,typeObject,extension,action,infoFile,finalId);
        }
        else{
            String titleFile = withType ? makeTheNameExport(name,extension, type) : makeTheNameExport(name,extension);
            if(multipleExport){
                titleFile = name;
            }
            sendExportRequest(eb,request,exportService,typeObject,extension,action,infoFile,finalId,titleFile,finalParams,multipleExport);
        }
    }


    private static void sendExportRequest(EventBus eb, HttpServerRequest request, ExportService exportService, String typeObject, String extension, String
            action, JsonObject infoFile, String finalId, String titleFile, JsonObject finalParams, boolean isMultipleExport){
        UserUtils.getUserInfos(eb, request, user -> {
            if (!isMultipleExport){
                soloExport(eb, request, exportService, typeObject, extension, action, infoFile, finalId, titleFile, finalParams, user);
            }else{
                mutliExport(eb, request, exportService, typeObject, extension, action, infoFile, finalId, titleFile, finalParams, user);
            }
        });
    }

    private static void soloExport(EventBus eb, HttpServerRequest request, ExportService exportService, String typeObject, String extension, String action, JsonObject infoFile, String finalId, String titleFile, JsonObject finalParams, UserInfos user) {
        exportService.createWhenStart(typeObject, extension, infoFile, finalId, titleFile, user.getUserId(), action, finalParams, newExport -> {
            if (newExport.isRight()) {
                String idExport = newExport.right().getValue().getString("id");
                try {
                    Logging.insert(eb,
                            request,
                            Contexts.EXPORT.toString(),
                            Actions.CREATE.toString(),
                            idExport.toString(),
                            new JsonObject().put("ids", idExport).put("fileName", titleFile));
                    log.info("Send export request");
                    Lystore.launchWorker(eb);
                    request.response().setStatusCode(201).end("Import started " + idExport);
                } catch (Exception error) {
                    catchError(exportService, idExport, error);
                }
            } else {
                log.error("Fail to insert file in SQL " + newExport.left());
            }
        });
    }

    private static void mutliExport(EventBus eb, HttpServerRequest request, ExportService exportService, String typeObject, String extension, String action, JsonObject infoFile, String finalId, String titleFile, JsonObject finalParams, UserInfos user) {
        String [] objectsId = finalId.split(",");
        for (String currentId : objectsId) {
            String nameFile = getFileNameMultiExport(extension, action, currentId);

            exportService.createWhenStart(typeObject, extension, infoFile, currentId, nameFile, user.getUserId(), action, finalParams, newExport -> {
                if (newExport.isRight()) {
                    String idExport = newExport.right().getValue().getString("id");
                    try {
                        Logging.insert(eb,
                                request,
                                Contexts.EXPORT.toString(),
                                Actions.CREATE.toString(),
                                idExport.toString(),
                                new JsonObject().put("ids", idExport).put("fileName", titleFile));
                        log.info("Send export request");
                        Lystore.launchWorker(eb);
                    } catch (Exception error) {
                        catchError(exportService, idExport, error);
                    }
                } else {
                    log.error("Fail to insert file in SQL " + newExport.left());
                }
            });
        }
        request.response().setStatusCode(201).end("Multi Import started ");
    }

    private static String getFileNameMultiExport(String extension, String action, String currentId) {
        String nameFile;
        switch (action){
            case ExportTypes.BC_BEFORE_VALIDATION:
                nameFile= makeTheNameExport("_BC"  ,extension);
                break;
            case ExportTypes.BC_AFTER_VALIDATION:
                nameFile  = makeTheNameExport("_BC_" + currentId ,extension);
                break;
            case ExportTypes.BC_AFTER_VALIDATION_STRUCT:
                nameFile  = makeTheNameExport("_STRUCTURES_BC_" + currentId ,extension);
                break;
            case ExportTypes.BC_BEFORE_VALIDATION_STRUCT:
                nameFile  = makeTheNameExport("_STRUCTURES_BC" ,extension);
                break;
            default:
                nameFile  = makeTheNameExport("_default_",extension);

                break;
        }
        return nameFile;
    }

    private static void getParamsDuringBCValidation(EventBus eb, HttpServerRequest request, ExportService exportService, String typeObject, String extension, String
            action,JsonObject infoFile, String finalId) {
        JsonObject params = new JsonObject();
        RequestUtils.bodyToJson(request,  Server.getPathPrefix(Lystore.CONFIG) + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                final JsonArray ids = orders.getJsonArray("ids");
                final String nbrBc = orders.getString("bc_number");
                final String nbrEngagement = orders.getString("engagement_number");
                final String dateGeneration = orders.getString("dateGeneration");
                Number supplierId = orders.getInteger("supplierId");
                final Number programId = orders.getInteger("id_program");

                params.put("ids",ids)
                        .put("nbrBc",nbrBc)
                        .put("nbrEngagement",nbrEngagement)
                        .put("dateGeneration",dateGeneration)
                        .put("supplierId",supplierId)
                        .put("programId",programId)


                ;
                String titleFile = makeTheNameExport("_BC_" + nbrBc,extension);
                sendExportRequest(eb,request,exportService,typeObject,extension,action,infoFile,nbrBc, titleFile,params, false);

            }
        });
    }

    private static class GetIdAndMutliExport {
        private HttpServerRequest request;
        private String typeObject;
        private String id;
        private JsonObject params;
        private Boolean multipleExport;

        public GetIdAndMutliExport(HttpServerRequest request, String typeObject, String id, JsonObject params, Boolean multipleExport) {
            this.request = request;
            this.typeObject = typeObject;
            this.id = id;
            this.params = params;
            this.multipleExport = multipleExport;
        }

        public String getId() {
            return id;
        }

        public Boolean getMultipleExport() {
            return multipleExport;
        }

        public GetIdAndMutliExport invoke() {
            if(request.getParam("id")!=null && typeObject.equals(Lystore.INSTRUCTIONS))
                id = request.getParam("id");

            if(request.params().getAll("number_validation") != null && !request.params().getAll("number_validation").isEmpty() ){
                if( request.params().getAll("number_validation").size() == 1)
                    id = request.params().getAll("number_validation").get(0);
                if(request.params().getAll("number_validation").size()> 1){
                    multipleExport = true;
                    id = "";
                    for (String  number : request.params().getAll("number_validation")){
                        id += number +",";
                    }
                    id =  id.substring(0,id.length()-1);
                }
                params.put("numberValidations",new JsonArray(request.params().getAll("number_validation")));
            }

            if(request.params().getAll("bc_number") != null && !request.params().getAll("bc_number").isEmpty() ){
                id = request.params().getAll("bc_number").get(0);
                if(id.contains(","))
                    multipleExport = true;
                params.put("bc_number",new JsonArray(request.params().getAll("bc_number")));
            }
            return this;
        }
    }
}
