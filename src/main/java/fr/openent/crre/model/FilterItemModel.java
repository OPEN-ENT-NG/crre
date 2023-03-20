package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.helpers.JsonHelper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class FilterItemModel implements IModel<FilterItemModel>, Cloneable {
    private List<String> disciplines;
    private List<String> classes;
    private List<String> grades;
    private List<String> editors;
    private List<String> distributors;
    private List<String> catalogs;
    private List<String> itemTypes;
    private List<String> structureSectors;
    private List<String> targets;
    private String searchingText;


    public FilterItemModel() {
    }

    public FilterItemModel(JsonObject jsonObject) {
        this.searchingText = jsonObject.getString(Field.SEARCHING_TEXT, null);
        this.disciplines = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.DISCIPLINES, new JsonArray()), String.class);
        this.classes = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.CLASSES, new JsonArray()), String.class);
        this.grades = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.GRADES, new JsonArray()), String.class);
        this.editors = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.EDITORS, new JsonArray()), String.class);
        this.distributors = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.DISTRIBUTORS, new JsonArray()), String.class);
        this.catalogs = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.CATALOGS, new JsonArray()), String.class);
        this.itemTypes = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.ITEM_TYPES, new JsonArray()), String.class);
        this.structureSectors = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.STRUCTURE_SECTORS, new JsonArray()), String.class);
        this.targets = JsonHelper.jsonArrayToList(jsonObject.getJsonArray(Field.TARGETS, new JsonArray()), String.class);


    }

    @Override
    public JsonObject toJson() {
        JsonObject FilterItemModelJson = new JsonObject();

        if (this.searchingText != null) {
            FilterItemModelJson.put(Field.SEARCHING_TEXT, this.searchingText);
        }
        if (this.distributors != null && !this.distributors.isEmpty()) {
            FilterItemModelJson.put(Field.DISTRIBUTORS, new JsonArray(this.distributors));
        }
        if (this.classes != null && !this.classes.isEmpty()) {
            FilterItemModelJson.put(Field.CLASSES, new JsonArray(this.classes));
        }
        if (this.grades != null && !this.grades.isEmpty()) {
            FilterItemModelJson.put(Field.GRADES, new JsonArray(this.grades));
        }
        if (this.disciplines != null && !this.disciplines.isEmpty()) {
            FilterItemModelJson.put(Field.DISCIPLINES, new JsonArray(this.disciplines));
        }
        if (this.itemTypes != null && !this.itemTypes.isEmpty()) {
            FilterItemModelJson.put(Field.ITEM_TYPES, new JsonArray(this.itemTypes));
        }
        if (this.targets != null && !this.targets.isEmpty()) {
            FilterItemModelJson.put(Field.TARGETS, new JsonArray(this.targets));
        }
        if (this.editors != null && !this.editors.isEmpty()) {
            FilterItemModelJson.put(Field.EDITORS, new JsonArray(this.editors));
        }
        if (this.catalogs != null && !this.catalogs.isEmpty()) {
            FilterItemModelJson.put(Field.CATALOGS, new JsonArray(this.catalogs));
        }
        if (this.structureSectors != null && !this.structureSectors.isEmpty()) {
            FilterItemModelJson.put(Field.STRUCTURE_SECTORS, new JsonArray(this.structureSectors));
        }

        return FilterItemModelJson;
    }

    public boolean isEmpty() {
        return this.disciplines.isEmpty() && this.classes.isEmpty() && this.grades.isEmpty() && this.editors.isEmpty()
                && this.distributors.isEmpty() && this.catalogs.isEmpty() && this.itemTypes.isEmpty()
                && this.structureSectors.isEmpty() && this.targets.isEmpty() && this.searchingText == null;
    }

    public List<String> getDisciplines() {
        return disciplines;
    }

    public FilterItemModel setDisciplines(List<String> disciplines) {
        this.disciplines = disciplines;
        return this;
    }

    public List<String> getClasses() {
        return classes;
    }

    public FilterItemModel setClasses(List<String> classes) {
        this.classes = classes;
        return this;
    }

    public List<String> getGrades() {
        return grades;
    }

    public FilterItemModel setGrades(List<String> grades) {
        this.grades = grades;
        return this;
    }

    public List<String> getEditors() {
        return editors;
    }

    public FilterItemModel setEditors(List<String> editors) {
        this.editors = editors;
        return this;
    }

    public List<String> getDistributors() {
        return distributors;
    }

    public FilterItemModel setDistributors(List<String> distributors) {
        this.distributors = distributors;
        return this;
    }

    public List<String> getCatalogs() {
        return catalogs;
    }

    public FilterItemModel setCatalogs(List<String> catalogs) {
        this.catalogs = catalogs;
        return this;
    }

    public List<String> getItemTypes() {
        return itemTypes;
    }

    public FilterItemModel setItemTypes(List<String> itemTypes) {
        this.itemTypes = itemTypes;
        return this;
    }

    public List<String> getStructureSectors() {
        return structureSectors;
    }

    public FilterItemModel setStructureSectors(List<String> structureSectors) {
        this.structureSectors = structureSectors;
        return this;
    }

    public List<String> getTargets() {
        return targets;
    }

    public FilterItemModel setTargets(List<String> targets) {
        this.targets = targets;
        return this;
    }

    public String getSearchingText() {
        return searchingText;
    }

    public FilterItemModel setSearchingText(String searchingText) {
        this.searchingText = searchingText;
        return this;
    }
    @Override
    public FilterItemModel clone() {
        return new FilterItemModel(this.toJson());
    }
}
