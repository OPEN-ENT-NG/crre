package fr.openent.crre.helpers;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;

import java.util.UUID;

public class FileHelper {
    private FileHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new file in the default temporary-file directory.
     * See {@link FileSystem#createTempFile(String, String, Handler)}
     *
     * @param fs file system
     * @return a future completed with the path of the created file
     */
    static public Future<String> createTempFile(FileSystem fs) {
        Promise<String> promise = Promise.promise();
        String prefix = UUID.randomUUID().toString();
        fs.createTempFile(prefix, "", event -> {
            if (event.succeeded()) {
                promise.complete(event.result());
            } else {
                //todo fail log
                promise.fail(event.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Open the file represented by {@code path}, asynchronously.
     * See {@link FileSystem#open(String, OpenOptions, Handler)}
     *
     * @param fs file system
     * @param path  path to the file
     * @return a future completed with the file
     */
    static public Future<AsyncFile> getFile(FileSystem fs, String path) {
        Promise<AsyncFile> promise = Promise.promise();

        fs.open(path, new OpenOptions(), fileResult -> {
            if (fileResult.succeeded()) {
                AsyncFile tmpFile = fileResult.result();
                promise.complete(tmpFile);
            } else {
                promise.fail(fileResult.cause().getMessage());
            }
        });

        return promise.future();
    }
}
