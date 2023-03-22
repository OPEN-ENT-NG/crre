package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.service.StorageService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

import java.util.UUID;

public class DefaultStorageService implements StorageService {

    private final Storage storage;

    public DefaultStorageService(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Future<JsonObject> add(JsonObject body, Buffer buff, Buffer contentToAdd) {
        Promise<JsonObject> promise = Promise.promise();

        this.add(body, buff, contentToAdd, FutureHelper.handlerEitherPromise(promise));

        return promise.future();
    }

    @Override
    public void add(JsonObject body, Buffer buff, Buffer contentToAdd, Handler<Either<String, JsonObject>> handler) {
        String contentType;
        String content;
        byte[] byteContent;

        switch (body.getString("format")) {
            case "csv": // Case text created from Jupyter
                contentType = "text/csv";
                break;
            default:
                handler.handle(new Either.Left<>("[DefaultFileService@getFile] File type unknown : " + body.getString("format")));
                return;
        }

        if(contentToAdd != null){
            buff = contentToAdd.appendBuffer(buff);
        }

        storage.writeBuffer(UUID.randomUUID().toString(), buff, contentType, body.getString(Field.NAME), file -> {
            if (Field.OK.equals(file.getString(Field.STATUS))) {
                handler.handle(new Either.Right<>(file));
            }
            else {
                handler.handle(new Either.Left<>("[DefaultFileService@add] Failed to upload file from http request"));
            }
        });
    }

    @Override
    public void remove(String storageId, Handler<Either<String, JsonObject>> handler) {
        storage.removeFile(storageId, removeEvent -> {
            if (Field.OK.equals(removeEvent.getString(Field.STATUS))) {
                handler.handle(new Either.Right<>(removeEvent));
            }
            else {
                handler.handle(new Either.Left<>("[DefaultFileService@remove] Failed to remove file from storage : " + removeEvent.getString("message")));
            }
        });
    }
}