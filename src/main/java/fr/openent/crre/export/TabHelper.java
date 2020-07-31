package fr.openent.crre.export;

import fr.openent.crre.Crre;
import fr.openent.crre.helpers.ExcelHelper;
import fr.openent.crre.service.impl.DefaultEquipmentService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class TabHelper {
    protected Logger logger = LoggerFactory.getLogger(DefaultEquipmentService.class);
    protected static final String CMD = "CMD";
    protected static final String CMR = "CMR";
    protected static final String LYCEE = "LYC";
    protected static final String NULL_DATA="Pas de données sur l'établissement";
    protected static final String INVESTISSEMENT = "Investissement";
    protected static final String FONCTIONNEMENT = "Fonctionnement";
    protected Workbook wb;
    protected String query;
    protected Sheet sheet;
    protected JsonObject instruction;
    protected ExcelHelper excel;
    protected int operationsRowNumber = 9;
    final protected int yTab = 9;
    final protected int xTab = 1;
    protected int cellColumn = 1;
    protected boolean isEmpty = false;
    protected Logger log = LoggerFactory.getLogger(DefaultEquipmentService.class);
    protected int arrayLength = 4;
    protected long timeout = 999999999;
    protected JsonArray datas;
    protected final int LIMIT_FORMULA_SIZE = 8000;
    protected final int LIMIT_ATTEMPTS_CREATION = 3;
    protected int attemptNumber = 0;
    /**
     * Format : H-code
     */
    protected JsonObject tabx;
    protected JsonArray taby;
    protected ArrayList<ArrayList<Double>> priceTab;

    public TabHelper(Workbook wb, String TabName) {
        this.wb = wb;
        this.tabx = new JsonObject();
        this.taby = new JsonArray();
        this.sheet = wb.getSheet(TabName);
        if (wb.getSheetIndex(this.sheet) == -1) {
            this.sheet = wb.createSheet(TabName);
        }
        this.excel = new ExcelHelper(wb, sheet);
        priceTab = new ArrayList<ArrayList<Double>>();
        log.info("Initialize tab : " + TabName);
    }

    public abstract void create(Handler<Either<String, Boolean>> handler);

    /**
     * retrieve datas to insert into the page
     *
     * @param handler
     */
    public abstract void getDatas(Handler<Either<String, JsonArray>> handler);


    protected JsonArray sortByCity(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            jsonValues.add(values.getJsonObject(i));
        }

        jsonValues.sort(new Comparator<JsonObject>() {
            private static final String KEY_NAME = "zipCode";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                String cityA = "";
                String cityB = "";
                String nameA = "";
                String nameB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                            valA = a.getString(KEY_NAME).substring(0, 2);
                    }
                    if (b.containsKey(KEY_NAME)) {
                            valB = b.getString(KEY_NAME).substring(0, 2);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting structures during export");
                }
                if (valA.compareTo(valB) == 0) {
                    if (a.containsKey("city")) {
                        cityA = a.getString("city");
                    }
                    if (b.containsKey("city")) {
                        cityB = b.getString("city");
                    }
                    if (cityA.compareTo(cityB) == 0) {
                        if (a.containsKey("uai")) {
                            nameA = a.getString("uai");
                        }
                        if (b.containsKey("uai")) {
                            nameB = b.getString("uai");
                        }
                        return nameA.compareTo(nameB);
                    }
                    return cityA.compareTo(cityB);
                }
                return valA.compareTo(valB);
            }
        });

        for (int i = 0; i < values.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }


    protected void sizeMergeRegionWithStyle(int line, int columnStart, int columnEnd, CellStyle style) {
        CellRangeAddress merge = new CellRangeAddress(line, line, columnStart, columnEnd);
        sheet.addMergedRegion(merge);
        excel.setRegionHeaderStyle(merge, sheet, style);
        short height = 1000;
        Row row = sheet.getRow(line);
        row.setHeight(height);

    }

    protected void sizeMergeRegionLines(int cellColumn,int lineStart,int lineEnd ){
        sizeMergeRegionLinesWithStyle(cellColumn,lineStart,lineEnd,excel.standardTextStyle);
    }

    protected void sizeMergeRegionLinesWithStyle(int cellColumn,int lineStart,int lineEnd ,CellStyle style){
        CellRangeAddress merge = new CellRangeAddress(lineStart, lineEnd, cellColumn, cellColumn);
        sheet.addMergedRegion(merge);
        excel.setRegionHeaderStyle(merge, sheet, style);
    }

    public boolean checkEmpty() {
        if (datas.isEmpty()) {
            excel.insertBlackOnGreenHeader(0, 0, "Cet onglet ne possède pas de données à afficher");
            excel.autoSize(1);
        }
        return datas.isEmpty();
    }

    protected void sqlHandler(Handler<Either<String, JsonArray>> handler) {
        sqlHandler(handler, new JsonArray().add(instruction.getInteger("id")));
    }

    protected void sqlHandler(Handler<Either<String,JsonArray>> handler, JsonArray params){
        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(Crre.timeout * 1000000000L),SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }

    public void handleDatasDefault(Either<String, JsonArray> event, Handler<Either<String, Boolean>> handler) {
        try {
            if (event.isLeft()) {
                log.error("Failed to retrieve datas");
                handler.handle(new Either.Left<>("Failed to retrieve datas"));
            } else {
                if (checkEmpty()) {
                    handler.handle(new Either.Right<>(true));
                } else {
                    initDatas(handler);
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            handler.handle(new Either.Left<>("error when creating excel"));

        }
    }

    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));
        }
        getStructures(new JsonArray(structuresId), repStructures -> {
            boolean errorCatch= false;
            if (repStructures.isRight()) {
                try {
                    JsonArray structures = repStructures.right().getValue();
                    setStructuresFromDatas(structures);
                    if (datas.isEmpty()) {
                        handler.handle(new Either.Left<>("No data in database"));
                    } else {
                        datas = sortByCity(datas);
                        writeArray();
                    }
                }catch (Exception e){
                    errorCatch = true;
                }
                if(errorCatch)
                    handler.handle(new Either.Left<>("Error when writting files"));
                else
                    handler.handle(new Either.Right<>(true));
            } else {
                handler.handle(new Either.Left<>("Error when casting neo"));
            }
        });
    }

    protected void writeArray() {
        excel.insertWithStyle(0,0,"UAI",excel.yellowLabel);
        excel.insertWithStyle(1,0,"Nom de l'établissement",excel.yellowLabel);
        excel.insertWithStyle(2,0,"Commune",excel.yellowLabel);
        excel.insertWithStyle(3,0,"Tel",excel.yellowLabel);
        excel.insertWithStyle(4,0,"Equipment",excel.yellowLabel);
        excel.insertWithStyle(5,0,"Qté",excel.yellowLabel);
        for(int i=0;i<datas.size();i++){
            JsonObject data = datas.getJsonObject(i);
            excel.insertCellTab(0,i+1,makeCellWithoutNull(data.getString("uai")));
            excel.insertCellTab(1,i+1,makeCellWithoutNull(data.getString("nameEtab")));
            excel.insertCellTab(2,i+1,makeCellWithoutNull(data.getString("city")));
            excel.insertCellTab(3,i+1,makeCellWithoutNull(data.getString("phone")));
            excel.insertCellTab(4,i+1,makeCellWithoutNull(data.getString("name")));
            excel.insertCellTab(5,i+1,makeCellWithoutNull(data.getString("amount")));
        }
        excel.autoSize(6);
    }

    protected JsonArray sortByUai(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<>();
        try {
            for (int i = 0; i < values.size(); i++) {
                jsonValues.add(values.getJsonObject(i));
            }
            jsonValues.sort(new Comparator<JsonObject>() {
                private static final String KEY_NAME = "uai";
                @Override
                public int compare(JsonObject a, JsonObject b) {
                    String valA = "";
                    String valB = "";
                    try {
                        if (a.containsKey(KEY_NAME)) {
                            valA = a.getString(KEY_NAME);
                        }
                        if (b.containsKey(KEY_NAME)) {
                            valB = b.getString(KEY_NAME);
                        }
                    } catch (NullPointerException e) {
                        log.error("error when sorting values by uai during export");
                    }
                    return valA.compareTo(valB);
                }
            });
            for (int i = 0; i < values.size(); i++) {
                sortedJsonArray.add(jsonValues.get(i));
            }
        }
        catch(NullPointerException e){
            log.error("error in sorting by uai " + values);
           throw e;
        }
        return sortedJsonArray;

    }
    protected void setStructuresFromDatas(JsonArray structures) {
        JsonArray actions;
        JsonObject  structure;
        LocalDateTime test = LocalDateTime.now();
        DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("hh:mm:ss");
        log.info("@CrreWorker["+ this.getClass() +"] END " +   test.format(formatter) +" STRUCTURES GET FROM NEO "+ structures.size());
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            initEmptyStructures(data);
            if(data.containsKey("actions"))
                actions = new JsonArray(data.getString("actions"));
            else
                actions =new JsonArray();
            getElemsStructure(structures,data);
            data.put("actionsJO", actions);
        }
    }

    protected  void getElemsStructure(JsonArray structures,JsonObject data){
        JsonObject  structure;
        for (int j = 0; j < structures.size(); j++) {
            structure = structures.getJsonObject(j);
            if (data.getString("id_structure").equals(structure.getString("id"))) {

                if(structure.getString("name") != null){
                    data.put("nameEtab", structure.getString("name"));
                }
                putDataIfNotNull("uai",data, structure);
                putDataIfNotNull("city",data, structure);
                    putDataIfNotNull("type",data, structure);
                putDataIfNotNull("address",data, structure);
                putDataIfNotNull("zipCode",data, structure);
                putDataIfNotNull("phone",data, structure);
            }
        }
    }

    private void putDataIfNotNull(String key,JsonObject data, JsonObject structure) {
        if(structure.getString(key) != null){
            data.put(key, structure.getString(key));
        }
    }

    private void initEmptyStructures(JsonObject data) {
        data.put("nameEtab", NULL_DATA);
        data.put("uai", NULL_DATA);
        data.put("city", NULL_DATA);
        data.put("type", NULL_DATA);
        data.put("address",NULL_DATA);
        data.put("zipCode", "??");
        data.put("phone", NULL_DATA);
    }


    protected String makeCellWithoutNull ( String valueGet){
        return valueGet != null? valueGet : "Pas d'informations";
    }
    protected void futureHandler(Handler<Either<String, JsonArray>> handler, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray results =new JsonArray();
                List<JsonArray> resultsList = event.result().list();
               for(int i = 0 ; i < resultsList.size();i++){
                  results.add(resultsList.get(i).getJsonObject(0));
               }
                handler.handle(new Either.Right(results));

            } else {
                handler.handle(new Either.Left<>("Error when resolving futures : " + event.cause().getMessage()));
            }
        });
    }
    /**
     * get structures from neo
     * @param ids
     * @param handler
     */
    //TODO next improve : make futures for each structs
    protected void getStructures(JsonArray ids, Handler<Either<String, JsonArray>> handler)  {
        LocalDateTime test = LocalDateTime.now();
        List<Future> futures = new ArrayList<>();
        DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("hh:mm:ss");
        log.info("@CrreWorker["+ this.getClass() +"] START " +   test.format(formatter) + " Array structures id to send SIZE : " + ids.size());


        for(int i = 0 ; i < ids.size();i++){
            Future<JsonArray> future = Future.future();
            futures.add(future);
        }
        futureHandler(handler,futures);
        for(int i = 0 ; i < ids.size();i++){
            String id = ids.getString(i);
            getStructure(id,getHandler(futures.get(i)));
        }
    }

    private void getStructure(String id, Handler<Either<String, JsonArray>> handler) {

        String query = "" +
                "MATCH (s:Structure) " +
                "WHERE s.id = {id} " +
                "RETURN " +
                "s.id as id," +
                " s.UAI as uai," +
                " s.name as name," +
                " s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                "s.zipCode as zipCode," +
                " s.city as city," +
                " s.type as type," +
                " s.phone as phone";
        try {
            Neo4j.getInstance().execute(query, new JsonObject().put("id", id), Neo4jResult.validResultHandler(handler));
        }catch (Exception e){
            logger.error( "@CrreWorker["+ e.getClass() +"] " + e.getMessage() +" tabHelper");
            getStructure(id,handler);
        }
    }
    protected Handler<Either<String, JsonArray>> getHandler(Future<JsonArray> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }
    /**
     *
     * @param structures Result of getStructures ( neoStructures)
     *
     * Method called when all the data are init to write an excel Page
     */
    protected  void fillPage(JsonArray structures){

    }


    protected Handler<Either<String, JsonArray>> getStructureHandler(JsonArray structuresId, Handler<Either<String, Boolean>> handler) {
        return repStructures -> {
            boolean errorCatch = false;
            String errorSTR = "" ;
            if (repStructures.isRight()) {
                try {
                    JsonArray structures = repStructures.right().getValue();
                    structures = sortByUai(structures);
                    fillPage(structures);
                }catch (Exception e){
                    errorCatch = true;
                    errorSTR = e.getMessage();
                    log.error("------------------------------ERROR---------------------------");
                    for (StackTraceElement elem : e.getStackTrace()) {
                        log.error("\t\t"+ elem);
                    }
                    log.error("-------------------------END ERROR---------------------------");

                }
                HandleCatchResult(errorCatch, errorSTR, structuresId, handler);
            } else {
                handler.handle(new Either.Left<>("Error when casting neo"));
            }
        };
    }


    protected void HandleCatchResult(boolean errorCatch, String errorSTR, JsonArray structuresId, Handler<Either<String, Boolean>> handler) {
        if(errorCatch && attemptNumber < LIMIT_ATTEMPTS_CREATION){
            getStructures(structuresId,getStructureHandler(structuresId, handler));
            attemptNumber ++;
        }
        else if(errorCatch && attemptNumber == LIMIT_ATTEMPTS_CREATION){
            handler.handle(new Either.Left<>("[" + this.getClass()  + "] "+ errorSTR));

        }else{
            handler.handle(new Either.Right<>(true));
        }
    }
}
