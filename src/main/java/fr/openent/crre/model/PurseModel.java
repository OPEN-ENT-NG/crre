package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class PurseModel implements IModel<PurseModel> {
    private String idStructure;
    private Double amount;
    private Double initialAmount;
    private Double consumableAmount;
    private Double consumableInitialAmount;
    private Double addedInitialAmount;
    private Double addedConsumableInitialAmount;

    private StructureNeo4jModel structure;

    public PurseModel() {
    }

    public PurseModel(JsonObject jsonObject) {
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.amount = jsonObject.getDouble(Field.AMOUNT);
        this.initialAmount = jsonObject.getDouble(Field.INITIAL_AMOUNT);
        this.consumableAmount = jsonObject.getDouble(Field.CONSUMABLE_AMOUNT);
        this.consumableInitialAmount = jsonObject.getDouble(Field.CONSUMABLE_INITIAL_AMOUNT);
        this.addedInitialAmount = jsonObject.getDouble(Field.ADDED_INITIAL_AMOUNT);
        this.addedConsumableInitialAmount = jsonObject.getDouble(Field.ADDED_CONSUMABLE_INITIAL_AMOUNT);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public String getIdStructure() {
        return idStructure;
    }

    public PurseModel setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public Double getAmount() {
        return amount;
    }

    public PurseModel setAmount(Double amount) {
        this.amount = amount;
        return this;
    }

    public Double getInitialAmount() {
        return initialAmount;
    }

    public PurseModel setInitialAmount(Double initialAmount) {
        this.initialAmount = initialAmount;
        return this;
    }

    public Double getConsumableAmount() {
        return consumableAmount;
    }

    public PurseModel setConsumableAmount(Double consumableAmount) {
        this.consumableAmount = consumableAmount;
        return this;
    }

    public Double getConsumableInitialAmount() {
        return consumableInitialAmount;
    }

    public PurseModel setConsumableInitialAmount(Double consumableInitialAmount) {
        this.consumableInitialAmount = consumableInitialAmount;
        return this;
    }

    public Double getAddedInitialAmount() {
        return addedInitialAmount;
    }

    public PurseModel setAddedInitialAmount(Double addedInitialAmount) {
        this.addedInitialAmount = addedInitialAmount;
        return this;
    }

    public StructureNeo4jModel getStructureNeo4jModel() {
        return structure;
    }

    public PurseModel setStructureNeo4jModel(StructureNeo4jModel structureNeo4jModel) {
        this.structure = structureNeo4jModel;
        return this;
    }

    public Double getAddedConsumableInitialAmount() {
        return addedConsumableInitialAmount;
    }

    public PurseModel setAddedConsumableInitialAmount(Double addedConsumableInitialAmount) {
        this.addedConsumableInitialAmount = addedConsumableInitialAmount;
        return this;
    }
}
