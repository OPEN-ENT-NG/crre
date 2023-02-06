package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class BasketOrderItem implements IModel<BasketOrderItem> {
    private Integer id;
    private Integer amount;
    private String processingDate;
    private String idItem;
    private Integer idCampaign;
    private String idStructure;
    private String comment;
    private Double priceProposal;
    private Integer idType;
    private String ownerId;
    private String ownerName;
    private Boolean reassort;
    //Todo use Model and rename
    private JsonObject equipment;
    private Integer idOrder;

    public BasketOrderItem() {
    }

    public BasketOrderItem(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.processingDate = jsonObject.getString(Field.PROCESSING_DATE);
        this.idItem = jsonObject.getString(Field.ID_ITEM);
        this.idCampaign = jsonObject.getInteger(Field.ID_CAMPAIGN);
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.comment = jsonObject.getString(Field.COMMENT);
        this.priceProposal = jsonObject.getDouble(Field.PRICE_PROPOSAL);
        this.idType = jsonObject.getInteger(Field.ID_TYPE);
        this.ownerId = jsonObject.getString(Field.OWNER_ID);
        this.ownerName = jsonObject.getString(Field.OWNER_NAME);
        this.reassort = jsonObject.getBoolean(Field.REASSORT);
        this.equipment = jsonObject.getJsonObject(Field.EQUIPMENT);
        this.idOrder = jsonObject.getInteger(Field.ID_ORDER);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public BasketOrderItem setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public BasketOrderItem setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public String getProcessingDate() {
        return processingDate;
    }

    public BasketOrderItem setProcessingDate(String processingDate) {
        this.processingDate = processingDate;
        return this;
    }

    public String getIdItem() {
        return idItem;
    }

    public BasketOrderItem setIdItem(String idItem) {
        this.idItem = idItem;
        return this;
    }

    public Integer getIdCampaign() {
        return idCampaign;
    }

    public BasketOrderItem setIdCampaign(Integer idCampaign) {
        this.idCampaign = idCampaign;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public BasketOrderItem setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public BasketOrderItem setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Double getPriceProposal() {
        return priceProposal;
    }

    public BasketOrderItem setPriceProposal(Double priceProposal) {
        this.priceProposal = priceProposal;
        return this;
    }

    public Integer getIdType() {
        return idType;
    }

    public BasketOrderItem setIdType(Integer idType) {
        this.idType = idType;
        return this;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public BasketOrderItem setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BasketOrderItem setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public Boolean getReassort() {
        return reassort;
    }

    public BasketOrderItem setReassort(Boolean reassort) {
        this.reassort = reassort;
        return this;
    }

    public JsonObject getEquipment() {
        return equipment;
    }

    public BasketOrderItem setEquipment(JsonObject equipment) {
        this.equipment = equipment;
        return this;
    }

    public Integer getIdOrder() {
        return idOrder;
    }

    public BasketOrderItem setIdOrder(Integer idOrder) {
        this.idOrder = idOrder;
        return this;
    }
}
