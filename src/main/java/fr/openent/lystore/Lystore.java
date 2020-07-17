package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import fr.openent.lystore.export.ExportLystoreWorker;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Lystore extends BaseServer {

    public static final String ORDERSSENT ="ORDERSSENT" ;
    public static String lystoreSchema;
    public static Integer iterationWorker;
    public static JsonObject CONFIG;
    public static Storage STORAGE;
    public static Integer PAGE_SIZE = 50;
    public static final String LYSTORE_COLLECTION = "lystore_export";
    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";
    public static long timeout = 99999999999L;
    public static final String ORDERS = "ORDERS";
    public static final String INSTRUCTIONS = "INSTRUCTION";
    public static final String XLSX = "xlsx";
    public static final String PDF = "pdf";

    @Override
    public void start() throws Exception {
        super.start();
        lystoreSchema = config.getString("db-schema");
       if(config.containsKey("iteration-worker")){
           iterationWorker = config.getInteger("iteration-worker");
       }else{
           log.info("no iteration worker in config");
           iterationWorker = 10 ;
        }
        EventBus eb = getEventBus(vertx);
        Storage storage = new StorageFactory(vertx, config).getStorage();
        STORAGE = storage;
        JsonObject mail = config.getJsonObject("mail", new JsonObject());



        addController(new LystoreController());
        addController(new AgentController());
        addController(new SupplierController());
        addController(new ProgramController());
        addController(new ContractTypeController());
        addController(new ContractController());
        addController(new TagController());
        addController(new EquipmentController(vertx));
        addController(new TaxController());
        addController(new LogController());
        addController(new CampaignController());
        addController(new PurseController(vertx));
        addController(new StructureGroupController(vertx));
        addController(new StructureController());
        addController(new BasketController(vertx, storage, config.getJsonObject("slack", new JsonObject()), mail));
        addController(new OrderController(storage, vertx, config, eb));
        addController(new UserController());
        addController(new EquipmentTypeController());
        addController(new TitleController(vertx, eb));
        addController(new GradeController());
        addController(new ProjectController());
        addController(new OperationController());
        addController(new InstructionController(storage));
        addController(new OrderRegionController());
        addController(new ExportController(storage));
        CONFIG = config;
        vertx.deployVerticle(ExportLystoreWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        launchWorker(eb);

    }

    public static void launchWorker(EventBus eb) {
        eb.send(ExportLystoreWorker.class.getSimpleName(), new JsonObject(), new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport ->{
                    if(!eventExport.body().getString("status").equals("ok"))
                        launchWorker(eb);
                    log.info("Ok calling worker " + eventExport.body().toString());
                }
        ));
    }
}
