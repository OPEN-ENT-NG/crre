package fr.openent.crre.model.item;

import fr.openent.crre.core.constants.ItemField;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class Lep implements IModel<Lep> {
    private String ean;
    private String title;
    private String description;
    private String duration;
    private List<String> licences;
    private List<Condition> conditions;

    public Lep(JsonObject json) {
        this.ean = json.getString(ItemField.EAN);
        this.title = json.getString(ItemField.TITLE);
        this.description = json.getString(ItemField.DESCRIPTION);
        this.duration = json.getString(ItemField.DURATION);
        this.licences = JsonHelper.jsonArrayToList(json.getJsonArray(ItemField.LICENCE), JsonObject.class)
                .stream()
                .filter(licences -> licences.containsKey(ItemField.VALUE))
                .map(licences -> licences.getString(ItemField.VALUE))
                .collect(Collectors.toList());

        JsonArray conditionsArray = json.getJsonArray(ItemField.CONDITIONS);
        if (conditionsArray != null && conditionsArray.size() > 0) {
            this.conditions = IModelHelper.toList(conditionsArray, Condition.class);
        }
    }

    // Getters and setters

    public String getEan() {
        return ean;
    }

    public Lep setEan(String ean) {
        this.ean = ean;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Lep setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Lep setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public Lep setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public List<String> getLicences() {
        return licences;
    }

    public Lep setLicences(List<String> licences) {
        this.licences = licences;
        return this;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public Lep setConditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }
    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put(ItemField.EAN, this.ean)
                .put(ItemField.TITLE, this.title)
                .put(ItemField.DESCRIPTION, this.description)
                .put(ItemField.DURATION, this.duration);

        if (this.licences != null && !this.licences.isEmpty()) {
            JsonArray licences = new JsonArray();
            for (String licence : this.licences) {
                licences.add(new JsonObject()
                        .put(ItemField.VALUE, licence));
            }
            json.put(ItemField.LICENCE, licences);
        }

        if (this.conditions != null && !this.conditions.isEmpty()) {
            json.put(ItemField.CONDITIONS, IModelHelper.toJsonArray(this.conditions));
        }

        return json;
    }
}
