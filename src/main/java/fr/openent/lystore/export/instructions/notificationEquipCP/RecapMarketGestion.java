package fr.openent.lystore.export.instructions.notificationEquipCP;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecapMarketGestion extends TabHelper {
    private static final String DPT = "DPT";
    private static final String RNE_lYC = "RNE + NOM DU LYCEE ET COMMUNE";
    private static final String ADDR = "ADRESSE DU LYCEE";
    private static final String CPDATE = "DATE DE CP";
    private static final String MARKET_CP = "NOM DU MARCHE ET N° DE CP";
    private static final String OP_DDE_NUMBER = "N°DE L'OPERATION + N° DE LA DDE ";
    private static final String CAMPAIGN = "CAMPAGNE";
    private static final String AMOUNT = "QUANTITE";
    private static final String TOTAL_PRICE = "PRIX TOTAL TTC";
    private static final String EQUIPMENT_NAME = "NOM DE L'EQUIPEMENT";
    private static final String TYPE = "TYPE";
    private static final String MARKET_NUMBER = "N° DE MARCHE";
    private static final String REGION_COMMENT = "COMMENTAIRE DE LA DEMANDE REGION";
    private static final String CIVILITY = "M. Mme Le Proviseur-e";
    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */

    private int lineNumber = 0;

    public RecapMarketGestion(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "RECAP MARCHES GESTIONNAIRE");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            JsonArray actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if(!structuresId.contains(action.getString("id_structure")))
                    structuresId.add(structuresId.size(), action.getString("id_structure"));

            }
        }
        getStructures(new JsonArray(structuresId), getStructureHandler(structuresId,handler));

//        getStructures(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
//            @Override
//            public void handle(Either<String, JsonArray> repStructures) {
//                boolean errorCatch= false;
//                if (repStructures.isRight()) {
//                    try {
//                        JsonArray structures = repStructures.right().getValue();
//                        if (datas.isEmpty()) {
//                            handler.handle(new Either.Left<>("No data in database"));
//                        } else {
//                        }
//                    }catch (Exception e){
//                        errorCatch = true;
//                        logger.error( "["+ e.getClass() +"] "+ e.getMessage()+" Recap");
//                    }
//                    if(errorCatch)
//                        handler.handle(new Either.Left<>("Error when writting files"));
//                    else
//                        handler.handle(new Either.Right<>(true));
//                } else {
//                    log.error("[RecapMarketGestion] getStructuresLeft");
//                    handler.handle(new Either.Left<>("Error when casting neo"));
//
//                }
//            }
//        });
    }

    @Override
    protected  void fillPage(JsonArray structures){
        setStructures(structures);
        writeArray();
    }
    private void writeArray() {
        log.info("RECAP MARKT  writeArray");

        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateExcel = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;
        try {
            orderDate = formatterDate.parse(instruction.getString("date_cp"));
        } catch (ParseException e) {
            log.error("Incorrect date format");
        }

        for (int i = 0; i < datas.size(); i++) {
            lineNumber++;
            try{

            JsonObject market = datas.getJsonObject(i);
            setLabel(market.getString("market"));
            JsonArray orders = market.getJsonArray("actionsJO");


            orders = sortByCity(orders, false);
            String previousCampaign = "";
            String previousZip = orders.getJsonObject(0).getString("zipCode").substring(0, 2);//check if orders > 0
            String previousCode = "";
            String zip = "";
            int startLine = lineNumber;
            for (int j = 0; j < orders.size(); j++) {
                try{
                    String address;
                    JsonObject order = orders.getJsonObject(j);
                    try {
                        address = order.getString("address").replace(",", ",\n");
                    } catch (NullPointerException e) {
                        address = order.getString("address");
                    }

                    zip = order.getString("zipCode").substring(0, 2);
                    String code = order.getString("code");
                    String campaign = order.getString("campaign");


                    if (!previousCampaign.equals(campaign)) {
                        if (j != 0) {
                            excel.insertHeader(0, lineNumber, previousZip);
                            previousZip = zip;
                            excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
                            lineNumber++;
                            startLine = lineNumber;

                        }
                        lineNumber++;
                        excel.insertUnderscoreHeader(0, lineNumber, campaign);
                        mergeCurrentLine(true);
                        previousCampaign = campaign;
                        previousCode = "";

                        lineNumber += 2;
                    }

                    if (!previousCode.equals(code)) {
                        lineNumber++;
                        previousCode = code;
                        excel.insertHeader(0, lineNumber, code);
                        mergeCurrentLine(false);
                        lineNumber++;
                        insertHeaders();
                        startLine = lineNumber;

                    }

                    if (!previousZip.equals(zip)) {
                        excel.insertHeader(0, lineNumber, previousZip);
                        previousZip = zip;
                        excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
                        lineNumber++;
                        startLine = lineNumber;


                    }
                    try{
                        excel.insertCellTabCenter(0, lineNumber, zip);
                        excel.insertCellTabCenter(1, lineNumber, order.getString("uai") + "\n" + order.getString("nameEtab") + "\n" + order.getString("city"));

                        excel.insertCellTabCenter(2, lineNumber, CIVILITY + "\n" + address + "\n TEL: " + makeCellWithoutNull(order.getString("phone")));
                        excel.insertCellTabCenterBold(3, lineNumber, formatterDateExcel.format(orderDate));
                        excel.insertCellTabCenter(4, lineNumber, order.getString("market") + " \nCP " + makeCellWithoutNull(instruction.getString("cp_number")));
                        if (order.getBoolean("isregion")) {
                            excel.insertCellTabCenter(5, lineNumber,
                                    "OPE : " + order.getString("operation") + "\nDDE : R-" + order.getInteger("id").toString());
                        } else {
                            excel.insertCellTabCenter(5, lineNumber,
                                    "OPE : " + order.getString("operation") + "\nDDE : C-" + order.getInteger("id").toString());
                        }
                    }catch (NullPointerException e){
                        log.error("@LystoreWorker["+ this.getClass() +"] error in inserting neo params of an order ");
                        throw e;
                    }
                    try{
                        excel.insertCellTabCenter(6, lineNumber, formatStrToCell(campaign, 5));
                        excel.insertCellTabCenter(7, lineNumber, formatStrToCell(order.getInteger("amount").toString(), 5));
                        excel.insertCellTabDouble(8, lineNumber, safeGetDouble(order, "total", "RecapMarketGestion"));
                        excel.insertCellTabCenter(9, lineNumber, formatStrToCell(order.getString("name_equipment"), 5));
                        excel.insertCellTabCenter(10, lineNumber, order.getString("cite_mixte"));
                        excel.insertCellTabCenter(11, lineNumber, formatStrToCell(order.getString("market"), 5));
                        excel.insertCellTabCenter(12, lineNumber,formatStrToCell( makeCellWithoutNull(order.getString("comment")), 5));
                    }catch (NullPointerException e){
                        log.error("@LystoreWorker["+ this.getClass() +"] error in getting param from sql");
                        throw e;
                    }
                    lineNumber++;
                }catch (NullPointerException e){
                    log.error("@LystoreWorker["+ this.getClass() +"] error in second for loop data : \n" + orders.getJsonObject(j));
                    throw e;
                }
            }
            excel.insertHeader(0, lineNumber, zip);
            excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
            lineNumber++;
            }catch (NullPointerException e){
                log.error("@LystoreWorker["+ this.getClass() +"] error in first loop : \n" );
                throw e;
            }
            if(i<=1){
                excel.autoSize(13);
            }
        }
    }


    private void mergeCurrentLine(boolean underscore) {
        if (underscore) {
            CellRangeAddress merge = new CellRangeAddress(lineNumber, lineNumber, 0, 1);
            sheet.addMergedRegion(merge);
            excel.setRegionUnderscoreHeader(merge, sheet);
        } else {
            CellRangeAddress merge = new CellRangeAddress(lineNumber, lineNumber, 0, 1);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }

    }


    private void insertHeaders() {


        excel.insertHeader(0, lineNumber, DPT);
        excel.insertHeader(1, lineNumber, RNE_lYC);

        excel.insertHeader(2, lineNumber, ADDR);
        excel.insertHeader(3, lineNumber, CPDATE);
        excel.insertHeader(4, lineNumber, MARKET_CP);
        excel.insertHeader(5, lineNumber, OP_DDE_NUMBER);
        excel.insertHeader(6, lineNumber, CAMPAIGN);
        excel.insertHeader(7, lineNumber, AMOUNT);
        excel.insertHeader(8, lineNumber, TOTAL_PRICE);
        excel.insertHeader(9, lineNumber, EQUIPMENT_NAME);
        excel.insertHeader(10, lineNumber, TYPE);
        excel.insertHeader(11, lineNumber, MARKET_NUMBER);
        excel.insertHeader(12, lineNumber, REGION_COMMENT);
        lineNumber++;
    }

    private void setLabel(String market) {
        excel.insertBlackOnGreenHeader(0, lineNumber, market);
        sizeMergeRegion(lineNumber, 0, 12);
        lineNumber += 2;

    }



    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                "             ROUND((( SELECT CASE          " +
                "            WHEN orders.price_proposal IS NOT NULL THEN 0     " +
                "            WHEN orders.override_region IS NULL THEN 0 " +
                "            WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0         " +
                "            ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)         " +
                "            END           " +
                "             FROM   " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "              where oco.id_order_client_equipment = orders.id " +
                "             ) + orders.\"price TTC\" " +
                "              ) * orders.amount   ,2 ) " +
                "             as Total, contract.name as market, contract_type.code as code,    " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,  orders.id as id,  " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment, orders.isregion,   " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  true as isregion, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation ," +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             UNION      " +
                "             (select oce.id ,  false as isregion," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id )      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         " +
                "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "  OR (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                "     OR                     (spa.structure_type = '" + LYCEE + "' AND" +
                " ( specific_structures.type is null OR  specific_structures.type ='" + LYCEE + "') ))    "+
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +


                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name ," +
                "             orders.id,orders.isregion " +
                "             order by campaign,code,market_id, id_structure,program,code  " +
                "  )    SELECT  values.market as market,    array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket       " +
                "  from  values      " +
                "  Group by values.market   " +
                "  Order by values.market   ;";

        sqlHandler(handler);
    }
}
