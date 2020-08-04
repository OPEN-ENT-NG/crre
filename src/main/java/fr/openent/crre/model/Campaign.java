package fr.openent.crre.model;

import fr.openent.crre.Crre;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Collections;


public class Campaign extends Model {

    private Integer id;
    private String name;
    private String description;
    private String image;
    private boolean accessible;
    private boolean purse_enabled;
    private boolean priority_enabled;
    private String priority_field;

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

    public Campaign(Integer id, String name, String description, String image, Boolean accessible, Boolean purse_enabled, Boolean priority_enabled, String priority_field) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.image = image;
        this.accessible = accessible;
        this.purse_enabled = purse_enabled;
        this.priority_enabled = priority_enabled;
        this.priority_field = priority_field;

    }

    public Campaign(JsonObject campaign) {
        this.id = campaign.getInteger("id", null);
        this.name = campaign.getString("name", null);
        this.description = campaign.getString("description", null);
        this.image = campaign.getString("image", null);
        this.accessible = campaign.getBoolean("accessible", null);
        this.purse_enabled = campaign.getBoolean("purse_enabled", null);
        this.priority_enabled = campaign.getBoolean("priority_enabled", null);
        this.priority_field = campaign.getString("priority_field", null);
    }

    public Campaign() {
        table = Crre.crreSchema + ".campaign";

        fillables.put("id", Arrays.asList("CREATE", "UPDATE", "mandatory"));
        fillables.put("name", Arrays.asList("CREATE", "UPDATE", "mandatory"));
        fillables.put("description", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("image", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("accessible", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("description", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("accessible", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("purse_enabled", Arrays.asList("CREATE", "UPDATE"));
        fillables.put("priority_enabled",Arrays.asList("CREATE", "UPDATE"));
        fillables.put("priority_field", Arrays.asList("CREATE", "UPDATE"));
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put("id", this.id)
                .put("name", this.name)
                .put("description", this.description)
                .put("image", this.image)
                .put("accessible", this.accessible)
                .put("purse_enabled", this.purse_enabled)
                .put("priority_field", this.priority_field)
                .put("priority_enabled", this.priority_enabled);
    }
}

