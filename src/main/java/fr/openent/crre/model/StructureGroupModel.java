package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class StructureGroupModel implements IModel<StructureGroupModel> {
    private Integer id;
    private String name;
    private String description;
    private List<String> structures;

    public StructureGroupModel(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.name = jsonObject.getString(Field.NAME);
        this.description = jsonObject.getString(Field.DESCRIPTION);
        JsonArray structureJsonArray;
        if (jsonObject.getValue(Field.STRUCTURES, null) instanceof String) {
            structureJsonArray = new JsonArray(jsonObject.getString(Field.STRUCTURES));
        } else {
            structureJsonArray = jsonObject.getJsonArray(Field.STRUCTURES, new JsonArray());
        }
        this.structures = structureJsonArray.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public StructureGroupModel() {
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public StructureGroupModel setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public StructureGroupModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public StructureGroupModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<String> getStructures() {
        return structures;
    }

    public StructureGroupModel setStructures(List<String> structures) {
        this.structures = structures;
        return this;
    }
}
