package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.ConfigField;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigElasticSearch implements IModel<ConfigElasticSearch> {
    private final List<String> serverUris;
    private final String serverUri;
    private final Integer poolSize;
    private final boolean keepAlive;
    private final String index;
    private final String username;
    private final String password;
    private final boolean elasticSearchSsl;

    public ConfigElasticSearch(JsonObject jsonObject) {
        this.serverUris = jsonObject.getJsonArray(ConfigField.SERVER_DASH_URIS).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
        this.serverUri = jsonObject.getString(ConfigField.SERVER_DASH_URI);
        this.poolSize = jsonObject.getInteger(ConfigField.POOL_SIZE, 16);
        this.keepAlive = jsonObject.getBoolean(ConfigField.KEEP_ALIVE, true);
        this.index = jsonObject.getString(ConfigField.INDEX);
        this.username = jsonObject.getString(ConfigField.USERNAME, null);
        this.password = jsonObject.getString(ConfigField.PASSWORD, null);
        this.elasticSearchSsl = jsonObject.getBoolean(ConfigField.ELASTIC_SEARCH_DASH_SSL, false);
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(ConfigField.SERVER_DASH_URIS, new JsonArray(new ArrayList<>(this.serverUris)))
                .put(ConfigField.SERVER_DASH_URI, this.serverUri)
                .put(ConfigField.POOL_SIZE, this.poolSize)
                .put(ConfigField.KEEP_ALIVE, this.keepAlive)
                .put(ConfigField.INDEX, this.index)
                .put(ConfigField.USERNAME, this.username)
                .put(ConfigField.PASSWORD, this.password)
                .put(ConfigField.ELASTIC_SEARCH_DASH_SSL, this.elasticSearchSsl);
    }

    public String getServerUri() {
        return serverUri;
    }

    public Integer getPoolSize() {
        return poolSize;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public List<String> getServerUris() {
        return serverUris;
    }

    public String getIndex() {
        return index;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isElasticSearchSsl() {
        return elasticSearchSsl;
    }
}
