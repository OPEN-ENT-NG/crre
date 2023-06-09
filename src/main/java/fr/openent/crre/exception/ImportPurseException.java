package fr.openent.crre.exception;

import fr.openent.crre.core.constants.Field;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class ImportPurseException extends Throwable implements CRREException {
    private final String i18nError;
    private List<String> uaiErrorList;

    public ImportPurseException(String i18nError) {
        super();
        this.i18nError = i18nError;
    }

    public ImportPurseException(String i18nError, List<String> uaiErrorList) {
        super();
        this.i18nError = i18nError;
        this.uaiErrorList = uaiErrorList;
    }

    @Override
    public JsonObject getMessageResult(HttpServerRequest request) {
        String error = I18n.getInstance().translate(this.i18nError, Renders.getHost(request), I18n.acceptLanguage(request)) + ".";
        if (uaiErrorList != null && !uaiErrorList.isEmpty()) {
            error += " " + "UAI en erreur : " + String.join(", ", uaiErrorList) + ".";
        }
        return new JsonObject().put(Field.MESSAGE, error);
    }

    @Override
    public int getStatus() {
        return 400;
    }

    public void setUAIErrorList(List<String> uaiErrorList) {
        this.uaiErrorList = uaiErrorList;
    }
}
