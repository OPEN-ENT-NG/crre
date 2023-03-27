package fr.openent.crre.helpers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

public class UserHelper {
    private static final Logger log = LoggerFactory.getLogger(UserHelper.class);

    private UserHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static Future<UserInfos> getUserInfos(EventBus eb, HttpServerRequest request) {
        Promise<UserInfos> promise = Promise.promise();
        UserUtils.getUserInfos(eb, request, promise::complete);

        return promise.future();
    }

    public static Future<UserInfos> getUserInfos(EventBus eb, String userId) {
        Promise<UserInfos> promise = Promise.promise();
        UserUtils.getUserInfos(eb, userId, promise::complete);

        return promise.future();
    }
}
