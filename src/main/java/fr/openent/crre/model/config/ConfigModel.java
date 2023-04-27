package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.ConfigField;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.IModelHelper;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConfigModel implements IModel<ConfigModel> {
    private final String dbSchema;
    private final Integer iterationWorker;
    private final String timeSecondSynchCron;
    private final String timeSecondStatCron;
    private final String timeSecondStatutCron;
    private final String timeSecondNotifyAdminsCron;
    private final boolean elasticSearch;
    private final ConfigElasticSearch elasticSearchConfig;
    private final boolean encodeEmailContent;
    private final Map<String ,ConfigLibraryModel> libraryConfig;
    private final boolean devMode;

    public ConfigModel(JsonObject jsonObject) {
        this.dbSchema = jsonObject.getString(ConfigField.DB_DASH_SCHEMA);
        this.iterationWorker = jsonObject.getInteger(ConfigField.ITERATION_DASH_WORKER, 10);
        this.timeSecondSynchCron = jsonObject.getString(ConfigField.TIME_SECOND_SYNCH_CRON);
        this.timeSecondStatCron = jsonObject.getString(ConfigField.TIME_SECOND_STAT_CRON);
        this.timeSecondStatutCron = jsonObject.getString(ConfigField.TIME_SECOND_STATUT_CRON);
        this.timeSecondNotifyAdminsCron = jsonObject.getString(ConfigField.TIME_SECOND_NOTIFY_ADMINS_CRON);
        this.elasticSearch = jsonObject.getBoolean(ConfigField.ELASTIC_SEARCH, false);
        JsonObject elasticSearchConfigJson = jsonObject.getJsonObject(ConfigField.ELASTIC_SEARCH_CONFIG, null);
        if (elasticSearchConfigJson != null) {
            this.elasticSearchConfig = new ConfigElasticSearch(elasticSearchConfigJson);
        } else {
            this.elasticSearchConfig = null;
        }
        this.encodeEmailContent = jsonObject.getBoolean(ConfigField.ENCODEEMAILCONTENT, true);

        if (jsonObject.containsKey(ConfigField.LIBRARYCONFIG) && jsonObject.getValue(ConfigField.LIBRARYCONFIG) instanceof JsonArray) {
            this.libraryConfig = IModelHelper.toList(jsonObject.getJsonArray(ConfigField.LIBRARYCONFIG), ConfigLibraryModel.class).stream()
                    .collect(Collectors.toMap(ConfigLibraryModel::getName, Function.identity()));
        } else {
            this.libraryConfig = new HashMap<>();
        }

        this.devMode = jsonObject.getBoolean(ConfigField.DEV_DASH_MODE, false);
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(ConfigField.DB_DASH_SCHEMA, this.dbSchema)
                .put(ConfigField.ITERATION_DASH_WORKER, this.iterationWorker)
                .put(ConfigField.TIME_SECOND_SYNCH_CRON, this.timeSecondSynchCron)
                .put(ConfigField.TIME_SECOND_STAT_CRON, this.timeSecondStatCron)
                .put(ConfigField.TIME_SECOND_STATUT_CRON, this.timeSecondStatutCron)
                .put(ConfigField.ELASTIC_SEARCH, this.elasticSearch)
                .put(ConfigField.LIBRARYCONFIG, IModelHelper.toJsonArray(new ArrayList<>(this.libraryConfig.values())))
                .put(ConfigField.ELASTIC_SEARCH_CONFIG, this.elasticSearchConfig.toJson())
                .put(ConfigField.ENCODEEMAILCONTENT, this.encodeEmailContent);
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public Integer getIterationWorker() {
        return iterationWorker;
    }

    public String getTimeSecondSynchCron() {
        return timeSecondSynchCron;
    }

    public String getTimeSecondStatCron() {
        return timeSecondStatCron;
    }

    public String getTimeSecondStatutCron() {
        return timeSecondStatutCron;
    }

    public String getTimeSecondNotifyAdminsCron() {
        return timeSecondNotifyAdminsCron;
    }

    public boolean isElasticSearch() {
        return elasticSearch;
    }

    public ConfigElasticSearch getElasticSearchConfig() {
        return elasticSearchConfig;
    }

    public boolean isEncodeEmailContent() {
        return encodeEmailContent;
    }

    public Map<String, ConfigLibraryModel> getLibraryConfig() {
        return libraryConfig;
    }

    public boolean isDevMode() {
        return devMode;
    }
}
