package fr.openent.crre.exception;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface CRREException {
    JsonObject getMessageResult(HttpServerRequest request);

    int getStatus();
}
