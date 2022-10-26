package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class EmailSendService {
    private static final Logger log = LoggerFactory.getLogger(EmailSendService.class);

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
                         Handler<Either<String, JsonObject>> handler) {
        String message = "[CRRE@EmailSendService@sendMail] Parameters : ";
        message += "\n request : " + request.toString();
        message += "\n eMail : " + eMail;
        message += "\n object : " + object;
        message += "\n attachment : " + attachment.toString();
        message += "\n body : " + body;
        log.info(message);
        emailSender.sendEmail(request,
                eMail,
                null,
                null,
                object,
                attachment,
                body,
                null,
                false,
                handlerToAsyncHandler(jsonObjectMessage -> {
                    if(Field.ERROR.equals(jsonObjectMessage.body().getString(Field.STATUS, Field.ERROR))){
                        String error = "[CRRE@EmailSendService@sendMail] Error while sending mails : " + jsonObjectMessage.body().toString();
                        log.error(error);
                        handler.handle(new Either.Left<>(error));
                    } else {
                        handler.handle(new Either.Right<>(jsonObjectMessage.body()));
                    }
                }));
    }
}
