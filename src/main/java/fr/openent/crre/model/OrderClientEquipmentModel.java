package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class OrderClientEquipmentModel implements IModel<OrderClientEquipmentModel>, Cloneable {
    private Integer id;
    private Integer amount;
    private String creationDate;
    private Integer idCampaign;
    private String idStructure;
    private OrderStatus status;
    private String equipmentKey;
    private String causeStatus;
    private String comment;
    private String userId;
    private Integer idBasket;
    private boolean reassort;

    public OrderClientEquipmentModel() {
    }

    public OrderClientEquipmentModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.creationDate = jsonObject.getString(Field.CREATION_DATE);
        this.idCampaign = jsonObject.getInteger(Field.ID_CAMPAIGN);
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.status = OrderStatus.getValue(jsonObject.getString(Field.STATUS));
        this.equipmentKey = jsonObject.getString(Field.EQUIPMENT_KEY);
        this.causeStatus = jsonObject.getString(Field.CAUSE_STATUS);
        this.comment = jsonObject.getString(Field.COMMENT);
        this.userId = jsonObject.getString(Field.USER_ID);
        this.idBasket = jsonObject.getInteger(Field.ID_BASKET);
        this.reassort = jsonObject.getBoolean(Field.REASSORT);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public OrderClientEquipmentModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public OrderClientEquipmentModel setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public OrderClientEquipmentModel setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Integer getIdCampaign() {
        return idCampaign;
    }

    public OrderClientEquipmentModel setIdCampaign(Integer idCampaign) {
        this.idCampaign = idCampaign;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public OrderClientEquipmentModel setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderClientEquipmentModel setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public String getEquipmentKey() {
        return equipmentKey;
    }

    public OrderClientEquipmentModel setEquipmentKey(String equipmentKey) {
        this.equipmentKey = equipmentKey;
        return this;
    }

    public String getCauseStatus() {
        return causeStatus;
    }

    public OrderClientEquipmentModel setCauseStatus(String causeStatus) {
        this.causeStatus = causeStatus;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public OrderClientEquipmentModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public OrderClientEquipmentModel setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Integer getIdBasket() {
        return idBasket;
    }

    public OrderClientEquipmentModel setIdBasket(Integer idBasket) {
        this.idBasket = idBasket;
        return this;
    }

    public boolean isReassort() {
        return reassort;
    }

    public OrderClientEquipmentModel setReassort(boolean reassort) {
        this.reassort = reassort;
        return this;
    }

    @Override
    public OrderClientEquipmentModel clone() {
        try {
            return (OrderClientEquipmentModel) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
