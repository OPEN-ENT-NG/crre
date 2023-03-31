package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class OrderUniversalModel implements IModel<OrderUniversalModel> {
    private Integer id;
    private Integer amount;
    private String prescriberValidationDate;
    private Campaign campaign;
    private String idStructure;
    private OrderStatus status;
    private String equipmentKey;
    private String causeStatus;
    private String comment;
    private String prescriberId;
    private BasketOrder basket;
    private Boolean reassort;
    private String validatorId;
    private String validatorName;
    private String validatorValidationDate;
    private String modificationDate;
    private ProjectModel project;
    private String equipmentName;
    private String equipmentImage;
    private Double equipmentPrice;
    private String equipmentGrade;
    private String equipmentEditor;
    private String equipmentDiffusor;
    private String equipmentFormat;
    private Double equipmentTva5;
    private Double equipmentTva20;
    private Double equipmentPriceht;
    private String offers;
    private Integer totalFree;

    private Integer orderClientId;
    private Integer orderRegionId;

    public OrderUniversalModel() {
    }

    public OrderUniversalModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.prescriberValidationDate = jsonObject.getString(Field.PRESCRIBER_VALIDATION_DATE);
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.status = OrderStatus.getValue(jsonObject.getString(Field.STATUS));
        this.equipmentKey = jsonObject.getString(Field.EQUIPMENT_KEY);
        this.causeStatus = jsonObject.getString(Field.CAUSE_STATUS);
        this.comment = jsonObject.getString(Field.COMMENT);
        this.prescriberId = jsonObject.getString(Field.PRESCRIBER_ID);
        this.reassort = jsonObject.getBoolean(Field.REASSORT);
        this.validatorId = jsonObject.getString(Field.VALIDATOR_ID);
        this.validatorName = jsonObject.getString(Field.VALIDATOR_NAME);
        this.validatorValidationDate = jsonObject.getString(Field.VALIDATOR_VALIDATION_DATE);
        this.modificationDate = jsonObject.getString(Field.MODIFICATION_DATE);
        this.equipmentName = jsonObject.getString(Field.EQUIPMENT_NAME);
        this.equipmentImage = jsonObject.getString(Field.EQUIPMENT_IMAGE);
        this.equipmentPrice = jsonObject.getDouble(Field.EQUIPMENT_PRICE);
        this.equipmentGrade = jsonObject.getString(Field.EQUIPMENT_GRADE);
        this.equipmentEditor = jsonObject.getString(Field.EQUIPMENT_EDITOR);
        this.equipmentDiffusor = jsonObject.getString(Field.EQUIPMENT_DIFFUSOR);
        this.equipmentFormat = jsonObject.getString(Field.EQUIPMENT_FORMAT);
        this.equipmentTva5 = jsonObject.getDouble(Field.EQUIPMENT_TVA5);
        this.equipmentTva20 = jsonObject.getDouble(Field.EQUIPMENT_TVA20);
        this.equipmentPriceht = jsonObject.getDouble(Field.EQUIPMENT_PRICEHT);
        this.offers = jsonObject.getString(Field.OFFERS);
        this.totalFree = jsonObject.getInteger(Field.TOTAL_FREE);
        if (jsonObject.getValue(Field.PROJECT) != null && jsonObject.getValue(Field.PROJECT) instanceof JsonObject) {
            this.project = IModelHelper.toModel(jsonObject.getJsonObject(Field.PROJECT), ProjectModel.class);
        }
        if (jsonObject.getValue(Field.PROJECT) != null && jsonObject.getValue(Field.PROJECT) instanceof String) {
            this.project = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.PROJECT)), ProjectModel.class);
        }
        if (jsonObject.getValue(Field.BASKET) != null && jsonObject.getValue(Field.BASKET) instanceof JsonObject) {
            this.basket = IModelHelper.toModel(jsonObject.getJsonObject(Field.BASKET), BasketOrder.class);
        }
        if (jsonObject.getValue(Field.BASKET) != null && jsonObject.getValue(Field.BASKET) instanceof String) {
            this.basket = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.BASKET)), BasketOrder.class);
        }
        if (jsonObject.getValue(Field.CAMPAIGN) != null && jsonObject.getValue(Field.CAMPAIGN) instanceof JsonObject) {
            this.campaign = IModelHelper.toModel(jsonObject.getJsonObject(Field.CAMPAIGN), Campaign.class);
        }
        if (jsonObject.getValue(Field.CAMPAIGN) != null && jsonObject.getValue(Field.CAMPAIGN) instanceof String) {
            this.campaign = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.CAMPAIGN)), Campaign.class);
        }

        this.orderClientId = jsonObject.getInteger(Field.ORDER_CLIENT_ID);
        this.orderRegionId = jsonObject.getInteger(Field.ORDER_REGION_ID);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public OrderUniversalModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public OrderUniversalModel setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public String getPrescriberValidationDate() {
        return prescriberValidationDate;
    }

    public OrderUniversalModel setPrescriberValidationDate(String prescriberValidationDate) {
        this.prescriberValidationDate = prescriberValidationDate;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public OrderUniversalModel setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderUniversalModel setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public String getEquipmentKey() {
        return equipmentKey;
    }

    public OrderUniversalModel setEquipmentKey(String equipmentKey) {
        this.equipmentKey = equipmentKey;
        return this;
    }

    public String getCauseStatus() {
        return causeStatus;
    }

    public OrderUniversalModel setCauseStatus(String causeStatus) {
        this.causeStatus = causeStatus;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OrderUniversalModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getPrescriberId() {
        return prescriberId;
    }

    public OrderUniversalModel setPrescriberId(String prescriberId) {
        this.prescriberId = prescriberId;
        return this;
    }

    public Boolean getReassort() {
        return reassort;
    }

    public OrderUniversalModel setReassort(Boolean reassort) {
        this.reassort = reassort;
        return this;
    }

    public String getValidatorId() {
        return validatorId;
    }

    public OrderUniversalModel setValidatorId(String validatorId) {
        this.validatorId = validatorId;
        return this;
    }

    public String getValidatorName() {
        return validatorName;
    }

    public OrderUniversalModel setValidatorName(String validatorName) {
        this.validatorName = validatorName;
        return this;
    }

    public String getValidatorValidationDate() {
        return validatorValidationDate;
    }

    public OrderUniversalModel setValidatorValidationDate(String validatorValidationDate) {
        this.validatorValidationDate = validatorValidationDate;
        return this;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public OrderUniversalModel setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
        return this;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public OrderUniversalModel setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
        return this;
    }

    public String getEquipmentImage() {
        return equipmentImage;
    }

    public OrderUniversalModel setEquipmentImage(String equipmentImage) {
        this.equipmentImage = equipmentImage;
        return this;
    }

    public Double getEquipmentPrice() {
        return equipmentPrice;
    }

    public OrderUniversalModel setEquipmentPrice(Double equipmentPrice) {
        this.equipmentPrice = equipmentPrice;
        return this;
    }

    public String getEquipmentGrade() {
        return equipmentGrade;
    }

    public OrderUniversalModel setEquipmentGrade(String equipmentGrade) {
        this.equipmentGrade = equipmentGrade;
        return this;
    }

    public String getEquipmentEditor() {
        return equipmentEditor;
    }

    public OrderUniversalModel setEquipmentEditor(String equipmentEditor) {
        this.equipmentEditor = equipmentEditor;
        return this;
    }

    public String getEquipmentDiffusor() {
        return equipmentDiffusor;
    }

    public OrderUniversalModel setEquipmentDiffusor(String equipmentDiffusor) {
        this.equipmentDiffusor = equipmentDiffusor;
        return this;
    }

    public String getEquipmentFormat() {
        return equipmentFormat;
    }

    public OrderUniversalModel setEquipmentFormat(String equipmentFormat) {
        this.equipmentFormat = equipmentFormat;
        return this;
    }

    public Double getEquipmentTva5() {
        return equipmentTva5;
    }

    public OrderUniversalModel setEquipmentTva5(Double equipmentTva5) {
        this.equipmentTva5 = equipmentTva5;
        return this;
    }

    public Double getEquipmentTva20() {
        return equipmentTva20;
    }

    public OrderUniversalModel setEquipmentTva20(Double equipmentTva20) {
        this.equipmentTva20 = equipmentTva20;
        return this;
    }

    public Double getEquipmentPriceht() {
        return equipmentPriceht;
    }

    public OrderUniversalModel setEquipmentPriceht(Double equipmentPriceht) {
        this.equipmentPriceht = equipmentPriceht;
        return this;
    }

    public String getOffers() {
        return offers;
    }

    public OrderUniversalModel setOffers(String offers) {
        this.offers = offers;
        return this;
    }

    public Integer getTotalFree() {
        return totalFree;
    }

    public OrderUniversalModel setTotalFree(Integer totalFree) {
        this.totalFree = totalFree;
        return this;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public OrderUniversalModel setCampaign(Campaign campaign) {
        this.campaign = campaign;
        return this;
    }

    public BasketOrder getBasket() {
        return basket;
    }

    public OrderUniversalModel setBasket(BasketOrder basket) {
        this.basket = basket;
        return this;
    }

    public ProjectModel getProject() {
        return project;
    }

    public OrderUniversalModel setProject(ProjectModel project) {
        this.project = project;
        return this;
    }

    public Integer getOrderClientId() {
        return orderClientId;
    }

    public OrderUniversalModel setOrderClientId(Integer orderClientId) {
        this.orderClientId = orderClientId;
        return this;
    }

    public Integer getOrderRegionId() {
        return orderRegionId;
    }

    public OrderUniversalModel setOrderRegionId(Integer orderRegionId) {
        this.orderRegionId = orderRegionId;
        return this;
    }
}
