package fr.openent.crre.service;

import fr.openent.crre.model.MailAttachment;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface QuoteService {
    void getAllQuote(Integer page, Handler<Either<String, JsonArray>> defaultResponseHandler);

    void getQuote(Integer id, Handler<Either<String, JsonObject>> defaultResponseHandler);

    void insertQuote(UserInfos user, Integer nbEtab, String csvFile, String title, Handler<Either<String, JsonObject>> handler);

    void insertQuote(UserInfos user, MailAttachment attachment, Handler<Either<String, JsonObject>> handler);

    void search(String query, Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);
}
