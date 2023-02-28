package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class Notify implements IModel<Notify> {
    private String message;
    private String basketName;
    private String campaignName;
    private String userName;
    private String projectTitle;
    private String structureId;
    private int campaignId;
    private int nbOrder;

    public Notify() {
    }

    public Notify(JsonObject jsonObject) {
        this.message = jsonObject.getString(Field.MESSAGE, null);
        this.basketName = jsonObject.getString(Field.BASKET_NAME, null);
        this.campaignName = jsonObject.getString(Field.NAME_CAMPAIGN, null);
        this.campaignId = jsonObject.getInteger(Field.ID_CAMPAIGN, null);
        this.userName = jsonObject.getString(Field.NAME_USER, null);
        this.projectTitle = jsonObject.getString(Field.PROJECT_TITLE, null);
        this.structureId = jsonObject.getString(Field.ID_STRUCTURE, null);
        this.nbOrder = jsonObject.getInteger(Field.NB_ORDER, null);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }

    public String getMessage() {
        return message;
    }

    public Notify setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getBasketName() {
        return basketName;
    }

    public Notify setBasketName(String basketName) {
        this.basketName = basketName;
        return this;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public Notify setCampaignName(String campaignName) {
        this.campaignName = campaignName;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public Notify setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public int getCampaignId() {
        return campaignId;
    }

    public Notify setCampaignId(int campaignId) {
        this.campaignId = campaignId;
        return this;
    }
    
    public String getProjectTitle() {
        return projectTitle;
    }

    public Notify setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Notify setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }
    public int getNbOrder() {
        return nbOrder;
    }

    public Notify setNbOrder(int nbOrder) {
        this.nbOrder = nbOrder;
        return this;
    }
}
