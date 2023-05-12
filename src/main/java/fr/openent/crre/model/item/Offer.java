package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Offer implements IModel<Offer> {
    private Boolean adopting;
    private String duration;
    private String eanBookseller;
    private Boolean is3years;
    private List<Lep> leps;
    private String libelle;
    private List<String> licences;
    private Boolean prescriber;
    private Float priceHT;
    private Integer minPurchaseAmount;
    private String editorReference;
    private List<Tva> tvas;
    private String type;

    public Offer() {

    }

    public Offer(JsonObject json) {
        this.adopting = json.getBoolean(ItemField.ADOPTING);
        this.duration = json.getString(ItemField.DURATION);
        this.eanBookseller = json.getString(ItemField.EAN_BOOKSELLER);
        this.is3years = json.getBoolean(ItemField.IS3YEARS);
        this.leps = IModelHelper.toList(json.getJsonArray(ItemField.LEPS), Lep.class);
        this.libelle = json.getString(ItemField.LIBELLE);
        this.licences = JsonHelper.jsonArrayToList(json.getJsonArray(ItemField.LICENCE), JsonObject.class).stream()
                .map(licence -> licence.getString(ItemField.VALUE))
                .collect(Collectors.toList());
        this.prescriber = json.getBoolean(ItemField.PRESCRIBER);
        this.priceHT = json.getFloat(ItemField.PRICE_HT);
        this.minPurchaseAmount = json.getInteger(ItemField.MIN_PURCHASE_AMOUNT);
        this.editorReference = json.getString(ItemField.EDITOR_REFERENCE);
        this.tvas = IModelHelper.toList(json.getJsonArray(ItemField.TVAS), Tva.class);
        this.type = json.getString(ItemField.TYPE);
    }

    public boolean isEmpty() {
        return this.type == null || this.eanBookseller == null;
    }

    // Getters and setters

    public Boolean getAdopting() {
        return adopting;
    }

    public Offer setAdopting(Boolean adopting) {
        this.adopting = adopting;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public Offer setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public String getEanBookseller() {
        return eanBookseller;
    }

    public Offer setEanBookseller(String eanBookseller) {
        this.eanBookseller = eanBookseller;
        return this;
    }

    public Boolean getIs3years() {
        return is3years;
    }

    public Offer setIs3years(Boolean is3years) {
        this.is3years = is3years;
        return this;
    }

    public List<Lep> getLeps() {
        return leps;
    }

    public Offer setLeps(List<Lep> leps) {
        this.leps = leps;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public Offer setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public List<String> getLicences() {
        return licences;
    }

    public Offer setLicences(List<String> licences) {
        this.licences = licences;
        return this;
    }

    public Boolean getPrescriber() {
        return prescriber;
    }

    public Offer setPrescriber(Boolean prescriber) {
        this.prescriber = prescriber;
        return this;
    }

    public Float getPriceHT() {
        return priceHT;
    }

    public Offer setPriceHT(Float priceHT) {
        this.priceHT = priceHT;
        return this;
    }

    public Integer getMinPurchaseAmount() {
        return minPurchaseAmount;
    }

    public Offer setMinPurchaseAmount(Integer minPurchaseAmount) {
        this.minPurchaseAmount = minPurchaseAmount;
        return this;
    }

    public String getEditorReference() {
        return editorReference;
    }

    public Offer setEditorReference(String editorReference) {
        this.editorReference = editorReference;
        return this;
    }

    public List<Tva> getTvas() {
        return tvas;
    }

    public Offer setTvas(List<Tva> tvas) {
        this.tvas = tvas;
        return this;
    }

    public String getType() {
        return type;
    }

    public Offer setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put(ItemField.ADOPTING, this.adopting);
        json.put(ItemField.DURATION, this.duration);
        json.put(ItemField.EAN_BOOKSELLER, this.eanBookseller);
        json.put(ItemField.IS3YEARS, this.is3years);
        JsonArray leps = new JsonArray();
        for (Lep lep : this.leps) {
            leps.add(lep.toJson());
        }
        json.put(ItemField.LEPS, leps);
        json.put(ItemField.LIBELLE, this.libelle);
        JsonArray licences = new JsonArray();
        for (String licence : this.licences) {
            licences.add(new JsonObject().put(ItemField.VALUE, licence));
        }
        json.put(ItemField.LICENCE, licences);
        json.put(ItemField.PRESCRIBER, this.prescriber);
        json.put(ItemField.PRICE_HT, this.priceHT);
        json.put(ItemField.MIN_PURCHASE_AMOUNT, this.minPurchaseAmount);
        json.put(ItemField.EDITOR_REFERENCE, this.editorReference);
        json.put(ItemField.TVAS, IModelHelper.toJsonArray(this.tvas));
        json.put(ItemField.TYPE, this.type);
        return json;
    }
}