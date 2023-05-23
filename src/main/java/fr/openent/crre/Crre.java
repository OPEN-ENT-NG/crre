package fr.openent.crre;

import fr.openent.crre.controllers.*;
import fr.openent.crre.cron.NotifyNewOrdersAdmin;
import fr.openent.crre.cron.statistics;
import fr.openent.crre.cron.synchTotalStudents;
import fr.openent.crre.cron.UpdateStatusCron;
import fr.openent.crre.helpers.elasticsearch.ElasticSearch;
import fr.openent.crre.model.config.ConfigModel;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.impl.ExportWorker;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.EmailFactory;
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


    @Override
    public void start() throws Exception {
        super.start();
        ConfigModel configModel = new ConfigModel(config);
        crreSchema = configModel.getDbSchema();
        iterationWorker = configModel.getIterationWorker();
        storage = new StorageFactory(vertx, config).getStorage();
        EmailFactory emailFactory = new EmailFactory(this.vertx, this.config);
        ServiceFactory serviceFactory = new ServiceFactory(vertx, configModel, emailFactory, storage);

        try {
            new CronTrigger(vertx, configModel.getTimeSecondSynchCron()).schedule(
                    new synchTotalStudents(serviceFactory)
            );
        } catch (Exception e) {
            log.error(String.format("[CRRE@%s::start] Invalid synch cron expression. %s", this.getClass().getSimpleName(), e.getMessage()));
        }

        try {
            new CronTrigger(vertx, configModel.getTimeSecondStatCron()).schedule(
                    new statistics(serviceFactory)
            );
        } catch (Exception e) {
            log.error(String.format("[CRRE@%s::start] Invalid statistics cron expression. %s", this.getClass().getSimpleName(), e.getMessage()));
        }

        try {
            new CronTrigger(vertx, configModel.getTimeSecondStatutCron()).schedule(
                    new UpdateStatusCron(serviceFactory)
            );
        } catch (Exception e) {
            log.error(String.format("[CRRE@%s::start] Invalid update cron expression. %s", this.getClass().getSimpleName(), e.getMessage()));
        }

        try {
            new CronTrigger(vertx, configModel.getTimeSecondNotifyAdminsCron()).schedule(
                    new NotifyNewOrdersAdmin(serviceFactory)
            );
        } catch (Exception e) {
            log.error(String.format("[CRRE@%s::start] Invalid notify cron expression. %s", this.getClass().getSimpleName(), e.getMessage()));
        }

        if (configModel.isElasticSearch() && configModel.getElasticSearchConfig() != null) {
            ElasticSearch.getInstance().init(this.vertx, configModel.getElasticSearchConfig());
        }

        addController(new CrreController());
        addController(new EquipmentController(serviceFactory));
        addController(new LogController(serviceFactory));
        addController(new CampaignController(serviceFactory));
        addController(new PurseController(storage, serviceFactory));
        addController(new StructureGroupController(storage, serviceFactory));
        addController(new StructureController(serviceFactory));
        addController(new BasketController(serviceFactory));
        addController(new OrderController(serviceFactory));
        addController(new UserController(serviceFactory));
        addController(new OrderRegionController(serviceFactory));
        addController(new StatisticsController(serviceFactory));
        addController(new QuoteController(serviceFactory));
        addController(new WorkflowController(serviceFactory));
        addController(new ArchiveController(serviceFactory));
        if (serviceFactory.getConfig().isDevMode()) {
            addController(new DevController(serviceFactory));
        }
        vertx.deployVerticle(ExportWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        CONFIG = config;
    }
}
