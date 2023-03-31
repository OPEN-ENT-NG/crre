package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class Campaign implements Cloneable, IModel<Campaign> {

    private Integer id;
    private String name;
    private String description;
    private String image;
    private boolean accessible;
    private boolean purseEnabled;
    private boolean priorityEnabled;
    private String priorityField;
    private String catalog;
    private Boolean reassort;
    private String startDate;
    private String endDate;
    private Boolean automaticClose;
    private String useCredit;
    private Integer idType;

    public Campaign(JsonObject campaign) {
        this.id = campaign.getInteger(Field.ID, null);
        this.name = campaign.getString(Field.NAME, null);
        this.description = campaign.getString(Field.DESCRIPTION, null);
        this.image = campaign.getString(Field.IMAGE, null);
        this.accessible = campaign.getBoolean(Field.ACCESSIBLE, null);
        this.purseEnabled = campaign.getBoolean(Field.PURSE_ENABLED, null);
        this.priorityEnabled = campaign.getBoolean(Field.PRIORITY_ENABLED, null);
        this.priorityField = campaign.getString(Field.PRIORITY_FIELD, null);
        this.catalog = campaign.getString(Field.CATALOG, null);
        this.reassort = campaign.getBoolean(Field.REASSORT, null);
        this.startDate = campaign.getString(Field.START_DATE, null);
        this.endDate = campaign.getString(Field.END_DATE, null);
        this.automaticClose = campaign.getBoolean(Field.AUTOMATIC_CLOSE, null);
        this.useCredit = campaign.getString(Field.USE_CREDIT, null);
        this.idType = campaign.getInteger(Field.ID_TYPE, null);
    }

    public Campaign() {
    }

    public Integer getId() {
        return id;
    }

    public Campaign setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Campaign setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Campaign setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getImage() {
        return image;
    }

    public Campaign setImage(String image) {
        this.image = image;
        return this;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public Campaign setAccessible(boolean accessible) {
        this.accessible = accessible;
        return this;
    }

    public boolean isPurseEnabled() {
        return purseEnabled;
    }

    public Campaign setPurseEnabled(boolean purseEnabled) {
        this.purseEnabled = purseEnabled;
        return this;
    }

    public boolean isPriorityEnabled() {
        return priorityEnabled;
    }

    public Campaign setPriorityEnabled(boolean priorityEnabled) {
        this.priorityEnabled = priorityEnabled;
        return this;
    }

    public String getPriorityField() {
        return priorityField;
    }

    public Campaign setPriorityField(String priorityField) {
        this.priorityField = priorityField;
        return this;
    }

    public String getCatalog() {
        return catalog;
    }

    public Campaign setCatalog(String catalog) {
        this.catalog = catalog;
        return this;
    }

    public Boolean getReassort() {
        return reassort;
    }

    public Campaign setReassort(Boolean reassort) {
        this.reassort = reassort;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public Campaign setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public Campaign setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public Boolean getAutomaticClose() {
        return automaticClose;
    }

    public Campaign setAutomaticClose(Boolean automaticClose) {
        this.automaticClose = automaticClose;
        return this;
    }

    public String getUseCredit() {
        return useCredit;
    }

    public Campaign setUseCredit(String useCredit) {
        this.useCredit = useCredit;
        return this;
    }

    public Integer getIdType() {
        return idType;
    }

    public Campaign setIdType(Integer idType) {
        this.idType = idType;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    @Override
    public Campaign clone() {
        try {
            return (Campaign) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

