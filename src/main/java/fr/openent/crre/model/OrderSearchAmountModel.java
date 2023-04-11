package fr.openent.crre.model;

import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class OrderSearchAmountModel implements IModel<OrderSearchAmountModel> {
    private Integer nbItem = 0;
    private Integer nbLicence = 0;
    private Integer nbConsumableLicence = 0;
    private Double priceCredit = 0.0;
    private Double priceConsumableCredit = 0.0;

    public OrderSearchAmountModel() {
    }

    public OrderSearchAmountModel(JsonObject jsonObject) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, false);
    }

    public Integer getNbItem() {
        return nbItem;
    }

    public OrderSearchAmountModel setNbItem(Integer nbItem) {
        this.nbItem = nbItem;
        return this;
    }

    public Integer getNbLicence() {
        return nbLicence;
    }

    public OrderSearchAmountModel setNbLicence(Integer nbLicence) {
        this.nbLicence = nbLicence;
        return this;
    }

    public Integer getNbConsumableLicence() {
        return nbConsumableLicence;
    }

    public OrderSearchAmountModel setNbConsumableLicence(Integer nbConsumableLicence) {
        this.nbConsumableLicence = nbConsumableLicence;
        return this;
    }

    public Double getPriceCredit() {
        return priceCredit;
    }

    public OrderSearchAmountModel setPriceCredit(Double priceCredit) {
        this.priceCredit = priceCredit;
        return this;
    }

    public Double getPriceConsumableCredit() {
        return priceConsumableCredit;
    }

    public OrderSearchAmountModel setPriceConsumableCredit(Double priceConsumableCredit) {
        this.priceConsumableCredit = priceConsumableCredit;
        return this;
    }
}
