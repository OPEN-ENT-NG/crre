package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fr.openent.crre.helpers.DateHelper.formatDate;
import static fr.openent.crre.helpers.EquipmentHelper.getRoundedPrice;

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
    private String equipmentBookseller;
    private StudentsTableModel students;

    private List<OrderUniversalOfferModel> offers;
    private Integer totalFree;
    private Integer orderClientId;
    private Integer orderRegionId;
    private boolean valid = true;

    //Not defined for send to bookseller commands.
    private String equipmentCatalogueType;
    private String equipmentType;
    private String equipmentEanLibrary;

    private StructureNeo4jModel structure;

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
        if (jsonObject.getValue(Field.EQUIPMENT_PRICE) != null) {
            this.equipmentPrice = Double.parseDouble(jsonObject.getValue(Field.EQUIPMENT_PRICE).toString());
        }
        this.equipmentGrade = jsonObject.getString(Field.EQUIPMENT_GRADE);
        this.equipmentEditor = jsonObject.getString(Field.EQUIPMENT_EDITOR);
        this.equipmentDiffusor = jsonObject.getString(Field.EQUIPMENT_DIFFUSOR);
        this.equipmentFormat = jsonObject.getString(Field.EQUIPMENT_FORMAT);
        this.equipmentTva5 = jsonObject.getDouble(Field.EQUIPMENT_TVA5);
        this.equipmentTva20 = jsonObject.getDouble(Field.EQUIPMENT_TVA20);
        this.equipmentPriceht = jsonObject.getDouble(Field.EQUIPMENT_PRICEHT);
        this.equipmentBookseller = jsonObject.getString(ItemField.BOOKSELLER);

        if (Objects.nonNull(jsonObject.getValue(Field.OFFERS)) && jsonObject.getValue(Field.OFFERS) instanceof JsonArray) {
            this.offers = IModelHelper.toList(jsonObject.getJsonArray(Field.OFFERS), OrderUniversalOfferModel.class);
        } else if (Objects.nonNull(jsonObject.getValue(Field.OFFERS)) && jsonObject.getValue(Field.OFFERS) instanceof String) {
            this.offers = IModelHelper.toList(new JsonArray(jsonObject.getString(Field.OFFERS)), OrderUniversalOfferModel.class);
        } else if (Objects.nonNull(this.offers)) {
            this.offers.forEach(orderUniversalOfferModel -> orderUniversalOfferModel.setOrderUniversalModel(this));
        } else {
            this.offers = new ArrayList<>();
        }

        this.totalFree = jsonObject.getInteger(Field.TOTAL_FREE);
        if (jsonObject.getValue(Field.PROJECT) != null && jsonObject.getValue(Field.PROJECT) instanceof JsonObject) {
            this.project = IModelHelper.toModel(jsonObject.getJsonObject(Field.PROJECT), ProjectModel.class).orElse(null);
        }
        if (jsonObject.getValue(Field.PROJECT) != null && jsonObject.getValue(Field.PROJECT) instanceof String) {
            this.project = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.PROJECT)), ProjectModel.class).orElse(null);
        }
        if (jsonObject.getValue(Field.BASKET) != null && jsonObject.getValue(Field.BASKET) instanceof JsonObject) {
            this.basket = IModelHelper.toModel(jsonObject.getJsonObject(Field.BASKET), BasketOrder.class).orElse(null);
        }
        if (jsonObject.getValue(Field.BASKET) != null && jsonObject.getValue(Field.BASKET) instanceof String) {
            this.basket = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.BASKET)), BasketOrder.class).orElse(null);
        }
        if (jsonObject.getValue(Field.CAMPAIGN) != null && jsonObject.getValue(Field.CAMPAIGN) instanceof JsonObject) {
            this.campaign = IModelHelper.toModel(jsonObject.getJsonObject(Field.CAMPAIGN), Campaign.class).orElse(null);
        }
        if (jsonObject.getValue(Field.CAMPAIGN) != null && jsonObject.getValue(Field.CAMPAIGN) instanceof String) {
            this.campaign = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.CAMPAIGN)), Campaign.class).orElse(null);
        }

        this.orderClientId = jsonObject.getInteger(Field.ORDER_CLIENT_ID);
        this.orderRegionId = jsonObject.getInteger(Field.ORDER_REGION_ID);

        if (jsonObject.getValue(Field.STUDENTS) != null && jsonObject.getValue(Field.STUDENTS) instanceof JsonObject) {
            this.students = IModelHelper.toModel(jsonObject.getJsonObject(Field.STUDENTS), StudentsTableModel.class).orElse(null);
        }
        if (jsonObject.getValue(Field.STUDENTS) != null && jsonObject.getValue(Field.STUDENTS) instanceof String) {
            this.students = IModelHelper.toModel(new JsonObject(jsonObject.getString(Field.STUDENTS)), StudentsTableModel.class).orElse(null);
        }
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

    public String getPrescriberValidationDateFormat() {
        return prescriberValidationDate != null ? formatDate(prescriberValidationDate) : null;
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

    public List<OrderUniversalOfferModel> getOffers() {
        return offers;
    }

    public OrderUniversalModel setOffers(List<OrderUniversalOfferModel> offers) {
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

    public Double getUnitedPriceTTC() {
        return this.equipmentPrice != null ? getRoundedPrice(this.equipmentPrice) : 0.0;
    }

    public Double getTotalPriceHT() {
        return this.equipmentPriceht != null ? getRoundedPrice(this.amount * this.equipmentPriceht) : 0.0;
    }

    public Double getTotalPriceTTC() {
        return this.equipmentPrice != null ? getRoundedPrice(this.amount * this.equipmentPrice) : 0.0;
    }

    public Double getEquipmentPriceTva5() {
        return this.getEquipmentTva5() != null ? getRoundedPrice(this.amount + this.equipmentTva5) : null;
    }

    public Double getEquipmentPriceTva20() {
        return this.getEquipmentTva20() != null ? getRoundedPrice(this.amount + this.equipmentTva20) : null;
    }

    public String getEquipmentCatalogueType() {
        return equipmentCatalogueType;
    }

    public OrderUniversalModel setEquipmentCatalogueType(String equipmentCatalogueType) {
        this.equipmentCatalogueType = equipmentCatalogueType;
        return this;
    }

    public String getEquipmentEanLibrary() {
        return equipmentEanLibrary;
    }

    public OrderUniversalModel setEquipmentEanLibrary(String equipmentEanLibrary) {
        this.equipmentEanLibrary = equipmentEanLibrary;
        return this;
    }

    public StudentsTableModel getStudents() {
        return students;
    }

    public OrderUniversalModel setStudents(StudentsTableModel students) {
        this.students = students;
        return this;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public OrderUniversalModel setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
        return this;
    }

    public String getEquipmentBookseller() {
        return equipmentBookseller;
    }

    public OrderUniversalModel setEquipmentBookseller(String equipmentBookseller) {
        this.equipmentBookseller = equipmentBookseller;
        return this;
    }

    public StructureNeo4jModel getStructure() {
        return structure;
    }

    public OrderUniversalModel setStructure(StructureNeo4jModel structure) {
        this.structure = structure;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public OrderUniversalModel setValid(boolean valid) {
        this.valid = valid;
        return this;
    }
}
