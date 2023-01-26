package fr.openent.crre.model.config;

import fr.openent.crre.core.constants.ConfigField;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.IModel;
import io.vertx.core.json.JsonObject;

public class ConfigModel implements IModel<ConfigModel> {
    private final String dbSchema;
    private final Integer iterationWorker;
    private final String timeSecondSynchCron;
    private final String timeSecondStatCron;
    private final String timeSecondStatutCron;
    private final boolean elasticSearch;
    private final ConfigMailModel mail;
    private final ConfigElasticSearch elasticSearchConfig;
    private final boolean encodeEmailContent;

    public ConfigModel(JsonObject jsonObject) {
        this.dbSchema = jsonObject.getString(ConfigField.DB_DASH_SCHEMA);
        this.iterationWorker = jsonObject.getInteger(ConfigField.ITERATION_DASH_WORKER, 10);
        this.timeSecondSynchCron = jsonObject.getString(ConfigField.TIME_SECOND_SYNCH_CRON);
        this.timeSecondStatCron = jsonObject.getString(ConfigField.TIME_SECOND_STAT_CRON);
        this.timeSecondStatutCron = jsonObject.getString(ConfigField.TIME_SECOND_STATUT_CRON);
        this.elasticSearch = jsonObject.getBoolean(ConfigField.ELASTIC_SEARCH, false);
        this.mail = new ConfigMailModel(jsonObject.getJsonObject(ConfigField.MAIL, new JsonObject()));
        JsonObject elasticSearchConfigJson = jsonObject.getJsonObject(ConfigField.ELASTIC_SEARCH_CONFIG, null);
        if (elasticSearchConfigJson != null) {
            this.elasticSearchConfig = new ConfigElasticSearch(elasticSearchConfigJson);
        } else {
            this.elasticSearchConfig = null;
        }
        this.encodeEmailContent = jsonObject.getBoolean(ConfigField.ENCODEEMAILCONTENT, true);
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
                .put(ConfigField.MAIL, this.mail.toJson())
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

    public boolean isElasticSearch() {
        return elasticSearch;
    }

    public ConfigMailModel getMail() {
        return mail;
    }

    public ConfigElasticSearch getElasticSearchConfig() {
        return elasticSearchConfig;
    }

    public boolean isEncodeEmailContent() {
        return encodeEmailContent;
    }
}
