package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class MailAttachment implements IModel<MailAttachment> {
    private String name;
    private String content;
    private int nbEtab;

    public MailAttachment() {
    }

    public MailAttachment(JsonObject jsonObject) {
        this.name = jsonObject.getString(Field.NAME);
        this.content = jsonObject.getString(Field.CONTENT);
        this.nbEtab = jsonObject.getInteger(Field.NB_ETAB);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }

    public String getName() {
        return name;
    }

    public MailAttachment setName(String name) {
        this.name = name;
        return this;
    }

    public String getContent() {
        return content;
    }

    public MailAttachment setContent(String content) {
        this.content = content;
        return this;
    }

    public int getNbEtab() {
        return nbEtab;
    }

    public MailAttachment setNbEtab(int nbEtab) {
        this.nbEtab = nbEtab;
        return this;
    }
}
