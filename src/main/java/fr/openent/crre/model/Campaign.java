package fr.openent.crre.model;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

public class Campaign extends Model {

    private Integer id;
    private String name;
    private String description;
    private String image;
    private boolean accessible;
    private boolean purse_enabled;
    private boolean priority_enabled;
    private String priority_field;
    private String catalog;
    private Boolean reassort;
    private String start_date;
    private String end_date;
    private Boolean automatic_close;
    private String use_credit;
    private Integer id_type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    public boolean isPurse_enabled() {
        return purse_enabled;
    }

    public void setPurse_enabled(boolean purse_enabled) {
        this.purse_enabled = purse_enabled;
    }

    public boolean isPriority_enabled() {
        return priority_enabled;
    }

    public void setPriority_enabled(boolean priority_enabled) {
        this.priority_enabled = priority_enabled;
    }

    public String getPriority_field() {
        return priority_field;
    }

    public void setPriority_field(String priority_field) {
        this.priority_field = priority_field;
    }

    public String getCatalog() { return catalog; }

    public void setCatalog(String catalog) { this.catalog = catalog; }

    public Boolean getReassort() { return reassort; }

    public void setReassort(Boolean reassort) { this.reassort = reassort; }

    public String getStart_date() { return start_date; }

    public void setStart_date(String start_date) { this.start_date = start_date; }

    public String getEnd_date() { return end_date; }

    public void setEnd_date(String end_date) { this.end_date = end_date; }

    public Boolean getAutomatic_close() { return automatic_close; }

    public void setAutomatic_close(Boolean automatic_close) { this.automatic_close = automatic_close; }

    public String getUse_credit() { return use_credit; }

    public void setUse_credit(String use_credit) { this.use_credit = use_credit; }

    public Integer getId_type() { return id_type; }

    public void setId_type(Integer id_type) { this.id_type = id_type; }

    public Campaign(Integer id, String name, String description, String image, Boolean accessible, Boolean purse_enabled,
                    Boolean priority_enabled, String priority_field, String catalog, Boolean reassort, String start_date,
                    String end_date, Boolean automatic_close, String use_credit, Integer id_type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = image;
        this.accessible = accessible;
        this.purse_enabled = purse_enabled;
        this.priority_enabled = priority_enabled;
        this.priority_field = priority_field;
        this.catalog = catalog;
        this.reassort = reassort;
        this.start_date = start_date;
        this.end_date = end_date;
        this.automatic_close = automatic_close;
        this.use_credit = use_credit;
        this.id_type = id_type;

    }

    public Campaign(JsonObject campaign) {
        this.id = campaign.getInteger(Field.ID, null);
        this.name = campaign.getString(Field.NAME, null);
        this.description = campaign.getString("description", null);
        this.image = campaign.getString("image", null);
        this.accessible = campaign.getBoolean("accessible", null);
        this.purse_enabled = campaign.getBoolean("purse_enabled", null);
        this.priority_enabled = campaign.getBoolean("priority_enabled", null);
        this.priority_field = campaign.getString("priority_field", null);
        this.catalog = campaign.getString("catalog", null);
        this.reassort = campaign.getBoolean("reassort", null);
        this.start_date = campaign.getString("start_date", null);
        this.end_date = campaign.getString("end_date", null);
        this.automatic_close = campaign.getBoolean("automatic_close", null);
        this.use_credit = campaign.getString("use_credit", null);
        this.id_type = campaign.getInteger("id_type", null);
    }

    public Campaign() {
        table = Crre.crreSchema + ".campaign";

        fillables.put(Field.ID, Arrays.asList("CREATE", "UPDATE", "mandatory"));
        fillables.put(Field.NAME, Arrays.asList("CREATE", "UPDATE", "mandatory"));
        fillables.put("description", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("image", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("accessible", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("purse_enabled", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("priority_enabled",Arrays.asList("CREATE", "UPDATE"));
        fillables.put("priority_field", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("catalog", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("reassort", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("start_date", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("end_date", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("automatic_close", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("use_credit", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("id_type", Arrays.asList("CREATE", "UPDATE"));
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put(Field.ID, this.id)
                .put(Field.NAME, this.name)
                .put("description", this.description)
                .put("image", this.image)
                .put("accessible", this.accessible)
                .put("purse_enabled", this.purse_enabled)
                .put("priority_field", this.priority_field)
                .put("priority_enabled", this.priority_enabled)
                .put("catalog", this.catalog)
                .put("reassort", this.reassort)
                .put("start_date", this.start_date)
                .put("end_date", this.end_date)
                .put("automatic_close", this.automatic_close)
                .put("use_credit", this.use_credit)
                .put("id_type", this.use_credit);
    }
}

