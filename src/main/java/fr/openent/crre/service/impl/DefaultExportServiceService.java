package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.helpers.MongoHelper;
import fr.openent.crre.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import io.vertx.core.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DefaultExportServiceService implements ExportService {
    Storage storage;
    private final Logger logger = LoggerFactory.getLogger(DefaultEquipmentService.class);
    MongoHelper mongo;

    public DefaultExportServiceService(Storage storage) {
        this.storage = storage;
        mongo = new MongoHelper(Crre.CRRE_COLLECTION);

    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user) {
        mongo.getExports(handler,user.getUserId());
    }


    @Override
    public void getExport(String fileId, Handler<Buffer> handler) {
        storage.readFile(fileId, handler);

    }

    @Override
    public void getExportName(String fileId, Handler<Either<String, JsonArray>> handler) {
        mongo.getExport(new JsonObject().put("fileId",fileId),handler);
    }

    public void deleteExport(JsonArray filesIds, Handler<JsonObject> handler) {
        storage.removeFiles(filesIds, handler);
    }

    public void deleteExportMongo(JsonArray idsExports, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        for (int i = 0; i < idsExports.size(); i++) {
            values.add(idsExports.getValue(i));
        }
        mongo.deleteExports(values,handler);
    }
    public void createWhenStart (String typeObject, String extension, JsonObject infoFile, String object_id, String nameFile,
                                 String userId, String action, JsonObject requestParams, Handler<Either<String, JsonObject>> handler){
        try {
            JsonArray params = new JsonArray();
            String  nameQuery = getQueryAndParams(typeObject,params,object_id);
            Sql.getInstance().prepared(nameQuery,params, SqlResult.validResultHandler(event -> {
                if(event.isRight()){
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();
                    JsonArray results = event.right().getValue();
                    JsonObject params1 = new JsonObject()
                            .put("filename",nameFile)
                            .put("userId",userId)
                            .put("object_id",object_id)
                            .put("object_name",results.getJsonObject(0).getString("object"))
                            .put("status","WAITING")
                            .put("created",dtf.format(now))
                            .put("action",action)
                            .put("typeObject",typeObject)
                            .put("extension",extension)
                            .put("NbIterationsLeft", Crre.iterationWorker)
                            .put("externalParams",requestParams);

                    if(infoFile.containsKey("type"))
                        params1.put("type",infoFile.getString("type"));

                    mongo.addExport(params1, event1 -> {
                        if(event1.equals("mongoinsertfailed"))
                            handler.handle(new Either.Left<>("Error when inserting mongo"));
                        else{
                            handler.handle(new Either.Right<>(new JsonObject().put("id", event1)));
                        }
                    });
                }else{
                    handler.handle(new Either.Left<>("Error when init export excel in Mongo"));
                    logger.error("Error when init export excel in Mongo");
                }
            }));
        } catch (Exception error){
            logger.error("error when create export" + error);
        }
    }

    private String getQueryAndParams(String typeObject, JsonArray params, String object_id) {
        String nameQuery= "";
        switch (typeObject) {
            case Crre.ORDERSSENT:
                nameQuery = "SELECT 'Bon de commande' as object from  " + Crre.crreSchema + ".order_client_equipment LIMIT 1";

                break;
            case Crre.ORDERS:
                nameQuery = "SELECT 'numéro de validation' as object from " + Crre.crreSchema + ".order_client_equipment LIMIT 1";
                break;
        }
        return nameQuery;
    }

    public void updateWhenError (String idExport, Handler<Either<String, Boolean>> handler){
        try{
            mongo.updateExport(idExport,"ERROR", "", event -> {
                if(event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else{
                    handler.handle(new Either.Right<>(true));
                }

            });
        } catch (Exception error){
            logger.error("error when update ERROR in export" + error);
        }
    }

    public void updateWhenSuccess (String fileId, String idExport, Handler<Either<String, Boolean>> handler) {
        try {
            logger.info("SUCCESS");
            mongo.updateExport(idExport,"SUCCESS",fileId, event -> {
                if (event.equals("mongoinsertfailed"))
                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                else {
                    handler.handle(new Either.Right<>(true));
                }
            });
        } catch (Exception error) {
            logger.error("error when update ERROR in export" + error);

        }
    }

    @Override
    public void getWaitingExport(Handler<Either<String,JsonObject>> handler) {
        mongo.getWaitingExports(handler);
    }
}
