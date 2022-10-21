package fr.openent.crre;

import fr.openent.crre.controllers.*;
import fr.openent.crre.cron.statistics;
import fr.openent.crre.cron.synchTotalStudents;
import fr.openent.crre.cron.updateStatus;
import fr.openent.crre.helpers.elasticsearch.ElasticSearch;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.ExportWorker;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

public class Crre extends BaseServer {

    public static String crreSchema;
    public static Integer iterationWorker;
    public static JsonObject CONFIG;
    public static Storage storage;
    public static final String ADMINISTRATOR_RIGHT = "crre.administrator";
    public static final String VALIDATOR_RIGHT = "crre.validator";
    public static final String PRESCRIPTOR_RIGHT = "crre.prescriptor";
    public static final String REASSORT_RIGHT = "crre.reassort";
    public static final String UPDATE_STUDENT_RIGHT = "crre.updateStudent";
    public static final String ACCESS_RIGHT = "crre.access";
    public static long timeout = 99999999999L;

    private ServiceFactory serviceFactory;


    @Override
    public void start() throws Exception {
        super.start();
        crreSchema = config.getString("db-schema");
        if(config.containsKey("iteration-worker")){
            iterationWorker = config.getInteger("iteration-worker");
        }else{
            log.info("no iteration worker in config");
            iterationWorker = 10 ;
        }
        storage = new StorageFactory(vertx, config).getStorage();
        serviceFactory = new ServiceFactory(vertx, config);
        JsonObject mail = config.getJsonObject("mail", new JsonObject());

        try {
            new CronTrigger(vertx, config.getString("timeSecondSynchCron")).schedule(
                    new synchTotalStudents(vertx)
            );
            new CronTrigger(vertx, config.getString("timeSecondStatCron")).schedule(
                    new statistics(vertx)
            );
            new CronTrigger(vertx, config.getString("timeSecondStatutCron")).schedule(
                    new updateStatus(serviceFactory)
            );
        } catch (Exception e) {
            log.error("Invalid CRRE cron expression.", e);
        }

        if (this.config.getBoolean("elasticsearch", false)) {
            if (this.config.getJsonObject("elasticsearchConfig") != null) {
                ElasticSearch.getInstance().init(this.vertx, this.config.getJsonObject("elasticsearchConfig"));
            }
        }

        addController(new CrreController());
        addController(new EquipmentController());
        addController(new LogController());
        addController(new CampaignController());
        addController(new PurseController(storage));
        addController(new StructureGroupController(storage));
        addController(new StructureController(getEventBus(vertx)));
        addController(new BasketController());
        addController(new OrderController());
        addController(new UserController());
        addController(new OrderRegionController(vertx, config, mail));
        addController(new StatisticsController());
        addController(new QuoteController());
        vertx.deployVerticle(ExportWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        CONFIG = config;
    }
}
