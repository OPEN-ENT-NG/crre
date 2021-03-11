package fr.openent.crre.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class EmailSendService {

    private final EmailSender emailSender;

    public EmailSendService(EmailSender emailSender){
        this.emailSender = emailSender;
    }

    /**
     * Send mail with attachments
     *
     * @param handler Need to not be null if you send mail with attachments
     */
    public void sendMail(HttpServerRequest request, String eMail, String object, String body, JsonArray attachment,
                         Handler<Either.Right<String, JsonObject>> handler) {
        emailSender.sendEmail(request,
                eMail,
                null,
                null,
                object,
                attachment,
                body,
                null,
                false,
                handlerToAsyncHandler(jsonObjectMessage -> handler.handle(new Either.Right<>(jsonObjectMessage.body()))));
    }
}
