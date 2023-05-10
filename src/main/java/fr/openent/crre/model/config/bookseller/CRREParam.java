package fr.openent.crre.model.config.bookseller;

import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;
import fr.openent.crre.core.constants.Field;

public class CRREParam implements IBooksellerConfigModel {
    private String email;
    private String apiUrl;

    public CRREParam() {
    }

    public CRREParam(JsonObject jsonObject) {
        this.email = jsonObject.getString(Field.EMAIL);
        this.apiUrl = jsonObject.getString(Field.APIURL);
    }

    public String getEmail() {
        return email;
    }

    public CRREParam setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public CRREParam setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }
}
