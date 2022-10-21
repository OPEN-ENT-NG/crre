package fr.openent.crre.helpers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.codec.BodyCodec;

public class HttpRequestHelper {
    private HttpRequestHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Store the response of a request in a file. Use Stream methode and it's asynchronous
     *
     * @param request http request we want to stream response
     * @param asyncFile file where we store response
     * @param close if we want close file after finish to read response
     * @return a future completed by the {@code asyncFile}
     */
    public static Future<AsyncFile> getHttpRequestResponseAsStream(HttpRequest<Buffer> request, AsyncFile asyncFile, boolean close) {
        Promise<AsyncFile> promise = Promise.promise();

        request.as(BodyCodec.pipe(asyncFile, close))
                .send(event -> {
                    if (event.succeeded() && event.result().statusCode() >= 200 && event.result().statusCode() < 300) {
                        promise.complete(asyncFile);
                    } else if (event.failed()) {
                        promise.fail(event.cause().getMessage());
                    } else {
                        String message = "Fail to call request";
                        promise.fail(message);
                    }
                });

        return promise.future();
    }
}
