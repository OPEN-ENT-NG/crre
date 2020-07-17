package fr.openent.lystore.helpers;

import fr.openent.lystore.security.WorkflowActionUtils;
import fr.openent.lystore.security.WorkflowActions;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.File;
import java.util.List;

import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class ImportCSVHelper {

    private Vertx vertx;
    private EventBus eb;
    private static final Logger log = LoggerFactory.getLogger(ImportCSVHelper.class);

    public ImportCSVHelper(Vertx vertx, EventBus eb) {
        this.vertx = vertx;
        this.eb = eb;
    }

    public void getParsedCSV(final HttpServerRequest request, final String path, final Handler<Either<String, Buffer>> handler) {
        getParsedCSV(request, path, true, handler);
    }

    /**
     * Get Parsed CSV
     *
     * @param request Http request
     * @param path    File path
     * @param deletePath Delete path after reading
     * @param handler Function handler returning data
     */
    public void getParsedCSV(final HttpServerRequest request, final String path, Boolean deletePath, final Handler<Either<String, Buffer>> handler) {
        uploadImport(request, path, new Handler<AsyncResult>() {
            @Override
            public void handle(AsyncResult event) {
                if (event.succeeded()) {
                    readCsv(request, path, deletePath, handler);
                } else {
                    handler.handle(new Either.Left<>("Can not upload import"));
                }
            }
        });
    }

    /**
     * Upload import with multipart
     *
     * @param request Http request containing files
     * @param handler Function handler returning data
     */
    public void uploadImport(final HttpServerRequest request, final String path, final Handler<AsyncResult> handler) {
        request.pause();
        request.setExpectMultipart(true);
        request.endHandler(getEndHandler(request, path, handler));
        request.exceptionHandler(getExceptionHandler(path, handler));
        request.uploadHandler(getUploadHandler(path, handler));
        vertx.fileSystem().mkdir(path, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.succeeded()) {
                    request.resume();
                } else {
                    handler.handle(new DefaultAsyncResult(new RuntimeException("mkdir.error", event.cause())));
                }
            }
        });
    }

    /**
     * Get end upload handler
     *
     * @param request Http Server Request
     * @param path    Upload directory path
     * @param handler Function handler
     * @return Handler<Void>
     */
    private Handler<Void> getEndHandler(final HttpServerRequest request, final String path,
                                        final Handler<AsyncResult> handler) {
        return new Handler<Void>() {
            @Override
            public void handle(Void v) {
                UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                    @Override
                    public void handle(UserInfos user) {
                        if (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())) {
                            handler.handle(new DefaultAsyncResult(null));
                        } else {
                            handler.handle(new DefaultAsyncResult(new RuntimeException("invalid.admin")));
                            deleteImportPath(vertx, path);
                        }
                    }
                });
            }
        };
    }

    /**
     * Get exception handler. It return a handler that catch error while the request upload the file.
     * In case of exception, the handler delete the directory.
     *
     * @param path    Temp directory path
     * @param handler Function handler
     * @return Handler<Throwable>
     */
    private Handler<Throwable> getExceptionHandler(final String path, final Handler<AsyncResult> handler) {
        return new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                handler.handle(new DefaultAsyncResult(event));
                deleteImportPath(vertx, path);
            }
        };
    }

    /**
     * Get chunk upload handler
     *
     * @param path    Upload directory path
     * @param handler Function handler
     * @return Upload handler
     */
    private static Handler<HttpServerFileUpload> getUploadHandler(final String path,
                                                                  final Handler<AsyncResult> handler) {
        return new Handler<HttpServerFileUpload>() {
            @Override
            public void handle(final HttpServerFileUpload upload) {
                if (!upload.filename().toLowerCase().endsWith(".csv")) {
                    handler.handle(new DefaultAsyncResult(
                            new RuntimeException("invalid.file.extension")
                    ));
                    return;
                }

                final String filename = path + File.separator + upload.filename();
                upload.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        log.info("File " + upload.filename() + " uploaded as " + upload.filename());
                    }
                });
                upload.streamToFileSystem(filename);
            }
        };
    }

    /**
     * Read CSV file
     *
     * @param request Http request
     * @param deletePath Delete path after reading
     * @param path    Temp directory path
     */
    public void readCsv(final HttpServerRequest request, final String path, Boolean deletePath, final Handler<Either<String, Buffer>> handler) {
        vertx.fileSystem().readDir(path, new Handler<AsyncResult<List<String>>>() {
            @Override
            public void handle(final AsyncResult<List<String>> event) {
                if (event.succeeded()) {
                    String file = event.result().get(0);
                    vertx.fileSystem().readFile(file, new Handler<AsyncResult<Buffer>>() {
                        @Override
                        public void handle(AsyncResult<Buffer> eventBuffer) {
                            if (eventBuffer.succeeded()) {
                                handler.handle(new Either.Right<>(eventBuffer.result()));
                            } else {
                                handler.handle(new Either.Left<>("Can not read the file"));
                            }
                            if (deletePath) {
                                deleteImportPath(vertx, path);
                            }
                        }
                    });
                } else {
                    handler.handle(new Either.Left<>("Can not read the file"));
                    deleteImportPath(vertx, path);
                }
            }
        });
    }
}
