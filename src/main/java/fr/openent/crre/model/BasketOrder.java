package fr.openent.crre.model;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import io.vertx.core.json.JsonObject;

public class BasketOrder implements IModel<BasketOrder> {
    private Integer id;
    private String name;
    private String idStructure;
    private Integer idCampaign;
    private String nameUser;
    private String idUser;
    private Double total;
    private Integer amount;
    private String created;

    public BasketOrder(JsonObject jsonObject) {
        this.id = jsonObject.getInteger(Field.ID);
        this.name = jsonObject.getString(Field.NAME);
        this.idStructure = jsonObject.getString(Field.ID_STRUCTURE);
        this.idCampaign = jsonObject.getInteger(Field.ID_CAMPAIGN);
        this.nameUser = jsonObject.getString(Field.NAME_USER);
        this.idUser = jsonObject.getString(Field.ID_USER);
        this.total = jsonObject.getDouble(Field.TOTAL);
        this.amount = jsonObject.getInteger(Field.AMOUNT);
        this.created = jsonObject.getString(Field.CREATED);
    }

    public BasketOrder() {
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public Integer getId() {
        return id;
    }

    public BasketOrder setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public BasketOrder setName(String name) {
        this.name = name;
        return this;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public BasketOrder setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public Integer getIdCampaign() {
        return idCampaign;
    }

    public BasketOrder setIdCampaign(int idCampaign) {
        this.idCampaign = idCampaign;
        return this;
    }

    public String getNameUser() {
        return nameUser;
    }

    public BasketOrder setNameUser(String nameUser) {
        this.nameUser = nameUser;
        return this;
    }

    public String getIdUser() {
        return idUser;
    }

    public BasketOrder setIdUser(String idUser) {
        this.idUser = idUser;
        return this;
    }

    public Double getTotal() {
        return total;
    }

    public BasketOrder setTotal(double total) {
        this.total = total;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public BasketOrder setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public BasketOrder setCreated(String created) {
        this.created = created;
        return this;
    }
}
