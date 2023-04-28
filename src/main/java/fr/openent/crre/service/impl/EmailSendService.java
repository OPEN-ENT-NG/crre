package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.FutureHelper;
import fr.openent.crre.model.MailAttachment;
import fr.openent.crre.model.config.ConfigModel;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class EmailSendService {
    private static final Logger log = LoggerFactory.getLogger(EmailSendService.class);

    private final EmailSender emailSender;
    private final ConfigModel config;

    public EmailSendService(EmailSender emailSender, ConfigModel config) {
        this.emailSender = emailSender;
        this.config = config;
    }

    /**
     * Send mail with attachments
     *
     * @param request Http request
     * @param eMail Recipient email
     * @param object Object email
     * @param body Body email
     * @param attachment Attachment email
     */
    public Future<JsonObject> sendMail(HttpServerRequest request, String eMail, String object, String body, MailAttachment attachment) {
        Promise<JsonObject> promise = Promise.promise();
        if (config.isEncodeEmailContent()) {
            attachment.setContent(Base64.getEncoder().encodeToString(attachment.getContent().getBytes(StandardCharsets.UTF_8)));
        }
        emailSender.sendEmail(request,
                eMail,
                null,
                null,
                object,
                new JsonArray().add(attachment.toJson()),
                body,
                null,
                false,
                FutureHelper.handlerAsyncResultPromise(promise));
        return promise.future();

    }
}
