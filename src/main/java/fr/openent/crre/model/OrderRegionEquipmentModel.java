package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.CreditTypeEnum;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class OrderRegionEquipmentModel implements IModel<OrderRegionEquipmentModel> {
    private Integer id;
    private Integer amount;
    private String creationDate;
    private String modificationDate;
    private String ownerName;
    private String ownerId;
    private String status;
    private String equipmentKey;
    private Integer idCampaign;
    private String idStructure;
    private String causeStatus;
    private String comment;
    private Integer idProject;
    private Integer idOrderClientEquipment;
    private Boolean reassort;
    private String idOfferEquipment;

    private Double price;
    private CreditTypeEnum useCredit;

    public OrderRegionEquipmentModel() {
    }

    public OrderRegionEquipmentModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.creationDate = jsonObject.getString(Field.CREATION_DATE);
        this.modificationDate = jsonObject.getString(Field.MODIFICATION_DATE);
        this.ownerName = jsonObject.getString(Field.OWNER_NAME);
        this.ownerId = jsonObject.getString(Field.OWNER_ID);
        this.status = jsonObject.getString(Field.STATUS);
        this.equipmentKey = jsonObject.getString(Field.EQUIPMENT_KEY);
        this.idCampaign = jsonObject.getInteger(Field.ID_CAMPAIGN);
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.causeStatus = jsonObject.getString(Field.CAUSE_STATUS);
        this.comment = jsonObject.getString(Field.COMMENT);
        this.idProject = jsonObject.getInteger(Field.ID_PROJECT);
        this.idOrderClientEquipment = jsonObject.getInteger(Field.ID_ORDER_CLIENT_EQUIPMENT);
        this.reassort = jsonObject.getBoolean(Field.REASSORT);
        this.idOfferEquipment = jsonObject.getString(Field.ID_OFFER_EQUIPMENT);

        this.price = jsonObject.getDouble(Field.PRICE);
        this.useCredit = CreditTypeEnum.getValue(jsonObject.getString(Field.USE_CREDIT));
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public OrderRegionEquipmentModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public OrderRegionEquipmentModel setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public OrderRegionEquipmentModel setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public OrderRegionEquipmentModel setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
        return this;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public OrderRegionEquipmentModel setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public OrderRegionEquipmentModel setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public OrderRegionEquipmentModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getEquipmentKey() {
        return equipmentKey;
    }

    public OrderRegionEquipmentModel setEquipmentKey(String equipmentKey) {
        this.equipmentKey = equipmentKey;
        return this;
    }

    public Integer getIdCampaign() {
        return idCampaign;
    }

    public OrderRegionEquipmentModel setIdCampaign(Integer idCampaign) {
        this.idCampaign = idCampaign;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public OrderRegionEquipmentModel setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public String getCauseStatus() {
        return causeStatus;
    }

    public OrderRegionEquipmentModel setCauseStatus(String causeStatus) {
        this.causeStatus = causeStatus;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OrderRegionEquipmentModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Integer getIdProject() {
        return idProject;
    }

    public OrderRegionEquipmentModel setIdProject(Integer idProject) {
        this.idProject = idProject;
        return this;
    }

    public Integer getIdOrderClientEquipment() {
        return idOrderClientEquipment;
    }

    public OrderRegionEquipmentModel setIdOrderClientEquipment(Integer idOrderClientEquipment) {
        this.idOrderClientEquipment = idOrderClientEquipment;
        return this;
    }

    public Boolean getReassort() {
        return reassort;
    }

    public OrderRegionEquipmentModel setReassort(Boolean reassort) {
        this.reassort = reassort;
        return this;
    }

    public String getIdOfferEquipment() {
        return idOfferEquipment;
    }

    public OrderRegionEquipmentModel setIdOfferEquipment(String idOfferEquipment) {
        this.idOfferEquipment = idOfferEquipment;
        return this;
    }

    public Double getPrice() {
        return price;
    }

    public OrderRegionEquipmentModel setPrice(Double price) {
        this.price = price;
        return this;
    }

    public CreditTypeEnum getUseCredit() {
        return useCredit;
    }

    public OrderRegionEquipmentModel setUseCredit(CreditTypeEnum useCredit) {
        this.useCredit = useCredit;
        return this;
    }
}
