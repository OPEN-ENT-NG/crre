package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

public class OrderUniversalOfferModel implements IModel<OrderUniversalOfferModel> {
    private String name;
    private String titre;
    private Integer amount;
    private String ean;
    private Double unitedPriceTTC;
    private Double totalPriceHT;
    private Double totalPriceTTC;
    private String type;
    private String idOffer;

    OrderUniversalModel orderUniversalModel;

    public OrderUniversalOfferModel() {
    }

    public OrderUniversalOfferModel(JsonObject jsonObject) {
        this.name = jsonObject.getString(Field.NAME);
        this.titre = jsonObject.getString(Field.TITRE);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.ean = jsonObject.getString(Field.EAN);
        this.unitedPriceTTC = jsonObject.getDouble(Field.UNITEDPRICETTC);
        this.totalPriceHT = jsonObject.getDouble(Field.TOTALPRICEHT);
        this.totalPriceTTC = jsonObject.getDouble(Field.TOTALPRICETTC);
        // If you want to remove this condition, you must resume giving to change the "typeCatalogue" field to "type" in the offer column
        if (!StringUtils.isEmpty(jsonObject.getString(Field.TYPECATALOGUE))) {
            this.type = jsonObject.getString(Field.TYPECATALOGUE);
        } else {
            this.type = jsonObject.getString(Field.TYPE);
        }
    }

    public String getName() {
        return name;
    }

    public OrderUniversalOfferModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getTitre() {
        return titre;
    }

    public OrderUniversalOfferModel setTitre(String titre) {
        this.titre = titre;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public OrderUniversalOfferModel setAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public String getEan() {
        return ean;
    }

    public OrderUniversalOfferModel setEan(String ean) {
        this.ean = ean;
        return this;
    }

    public Double getUnitedPriceTTC() {
        return unitedPriceTTC;
    }

    public OrderUniversalOfferModel setUnitedPriceTTC(Double unitedPriceTTC) {
        this.unitedPriceTTC = unitedPriceTTC;
        return this;
    }

    public Double getTotalPriceHT() {
        return totalPriceHT;
    }

    public OrderUniversalOfferModel setTotalPriceHT(Double totalPriceHT) {
        this.totalPriceHT = totalPriceHT;
        return this;
    }

    public Double getTotalPriceTTC() {
        return totalPriceTTC;
    }

    public OrderUniversalOfferModel setTotalPriceTTC(Double totalPriceTTC) {
        this.totalPriceTTC = totalPriceTTC;
        return this;
    }

    public String getType() {
        return type;
    }

    public OrderUniversalOfferModel setType(String type) {
        this.type = type;
        return this;
    }

    public OrderUniversalModel getOrderUniversalModel() {
        return orderUniversalModel;
    }

    public OrderUniversalOfferModel setOrderUniversalModel(OrderUniversalModel orderUniversalModel) {
        this.orderUniversalModel = orderUniversalModel;
        return this;
    }

    public String getIdOffer() {
        return idOffer;
    }

    public OrderUniversalOfferModel setIdOffer(String idOffer) {
        this.idOffer = idOffer;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject().put(Field.AMOUNT, this.amount)
                .put(Field.TITRE, this.titre)
                .put(Field.UNITEDPRICETTC, this.unitedPriceTTC)
                .put(Field.TOTALPRICEHT, this.totalPriceHT)
                .put(Field.TOTALPRICETTC, this.totalPriceTTC)
                .put(Field.NAME, this.name)
                .put(Field.EAN, this.ean)
                .put(Field.TYPE, this.type);
    }
}
