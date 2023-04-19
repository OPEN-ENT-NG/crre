package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class StructureNeo4jModel implements  IModel<StructureNeo4jModel> {
    private String id;
    private String uai;
    private String name;
    private String phone;
    private String address;
    private String zipCode;
    private String type;

    public StructureNeo4jModel() {
    }

    public StructureNeo4jModel(JsonObject jsonObject) {
        this.id = jsonObject.getString(Field.ID);
        this.uai = jsonObject.getString(Field.UAI);
        this.name = jsonObject.getString(Field.NAME);
        this.phone = jsonObject.getString(Field.PHONE);
        this.address = jsonObject.getString(Field.ADDRESS);
        this.zipCode = jsonObject.getString(Field.ZIPCODE);
        this.type = jsonObject.getString(Field.TYPE);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public String getId() {
        return id;
    }

    public StructureNeo4jModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getUai() {
        return uai;
    }

    public StructureNeo4jModel setUai(String uai) {
        this.uai = uai;
        return this;
    }

    public String getName() {
        return name;
    }

    public StructureNeo4jModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public StructureNeo4jModel setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public StructureNeo4jModel setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getZipCode() {
        return zipCode;
    }

    public StructureNeo4jModel setZipCode(String zipCode) {
        this.zipCode = zipCode;
        return this;
    }

    public String getType() {
        return type;
    }

    public StructureNeo4jModel setType(String type) {
        this.type = type;
        return this;
    }
}
