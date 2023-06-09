package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class PurseImportElement implements IModel<PurseImportElement> {
    private String uai;
    private String idStructure;
    private Credits credits;
    private Credits creditsConsumable;

    public PurseImportElement(JsonObject json) {
        this.uai = json.getString(Field.UAI);
        this.idStructure = json.getString(Field.IDSTRUCTURE);
        this.credits = IModelHelper.toModel(json.getJsonObject(Field.CREDITS), Credits.class).orElse(null);
        this.creditsConsumable = IModelHelper.toModel(json.getJsonObject(Field.CONSUMABLE_CREDITS), Credits.class).orElse(null);
    }

    public PurseImportElement() {
        this.uai = null;
        this.credits = null;
        this.creditsConsumable = null;
    }

    public PurseImportElement(String uai, Credits credits, Credits creditsConsumable) {
        this.uai = uai;
        this.credits = credits;
        this.creditsConsumable = creditsConsumable;
    }

    public String getUai() {
        return uai;
    }

    public PurseImportElement setUai(String uai) {
        this.uai = uai;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public PurseImportElement setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public Credits getCredits() {
        return credits;
    }

    public PurseImportElement setCredits(Credits credits) {
        this.credits = credits;
        return this;
    }

    public Credits getCreditsConsumable() {
        return creditsConsumable;
    }

    public PurseImportElement setCreditsConsumable(Credits creditsConsumable) {
        this.creditsConsumable = creditsConsumable;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.UAI, this.uai)
                .put(Field.IDSTRUCTURE, this.idStructure)
                .put(Field.CREDITS, this.credits.toJson())
                .put(Field.CONSUMABLE_CREDITS, this.creditsConsumable.toJson());
    }
}
