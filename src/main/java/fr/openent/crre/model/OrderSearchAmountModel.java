package fr.openent.crre.model;

import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class OrderSearchAmountModel implements IModel<OrderSearchAmountModel> {
    private Integer licence;
    private Integer consumableLicence;
    private Double credit;
    private Double consumableCredit;
    private Integer total;
    private Double totalFiltered;
    private Double totalFilteredConsumable;

    public OrderSearchAmountModel() {
    }

    public OrderSearchAmountModel(JsonObject jsonObject) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getLicence() {
        return licence;
    }

    public OrderSearchAmountModel setLicence(Integer licence) {
        this.licence = licence;
        return this;
    }

    public Integer getConsumableLicence() {
        return consumableLicence;
    }

    public OrderSearchAmountModel setConsumableLicence(Integer consumableLicence) {
        this.consumableLicence = consumableLicence;
        return this;
    }

    public Double getCredit() {
        return credit;
    }

    public OrderSearchAmountModel setCredit(Double credit) {
        this.credit = credit;
        return this;
    }

    public Double getConsumableCredit() {
        return consumableCredit;
    }

    public OrderSearchAmountModel setConsumableCredit(Double consumableCredit) {
        this.consumableCredit = consumableCredit;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public OrderSearchAmountModel setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public Double getTotalFiltered() {
        return totalFiltered;
    }

    public OrderSearchAmountModel setTotalFiltered(Double totalFiltered) {
        this.totalFiltered = totalFiltered;
        return this;
    }

    public Double getTotalFilteredConsumable() {
        return totalFilteredConsumable;
    }

    public OrderSearchAmountModel setTotalFilteredConsumable(Double totalFilteredConsumable) {
        this.totalFilteredConsumable = totalFilteredConsumable;
        return this;
    }
}
