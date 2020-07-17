package fr.openent.lystore.export.instructions.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.Collections;

public class RecapMarket extends TabHelper {
    JsonArray operations;
    JsonObject programMarket;
    private String type;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param type
     */
    public RecapMarket(Workbook wb, JsonObject instruction, String type) {
        super(wb, instruction, "Récapitulatif par marché ");
        this.type = type;
        this.programMarket = new JsonObject();
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {

        excel.setDefaultFont();
        getDatas(event -> {
            try{
                if (event.isLeft()) {
                    log.error("Failed to retrieve programs");
                    handler.handle(new Either.Left<>("Failed to retrieve programs"));
                } else {
                    if (checkEmpty()) {
                        handler.handle(new Either.Right<>(true));
                    } else {
                        JsonArray programs = event.right().getValue();
                        setArray(programs);
                        handler.handle(new Either.Right<>(true));
                    }
                }
            }catch(Exception e)
            {
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
            }
        });

    }


    @Override
    protected void setArray(JsonArray datas) {

        setLabels();

        for (int i = 0; i < datas.size(); i++) {//operations

            JsonObject operation = datas.getJsonObject(i);
            String actionsStrToArray = operation.getString("actions");

            excel.insertLabel(0, operationsRowNumber, operation.getString("label"));

            this.operationsRowNumber++;

            Double oldTotal = 0.d;
            String oldkey = "";

            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) { // datas of the array
                try{
                JsonObject action = actions.getJsonObject(j);
                //get the key to insert the data
                String key = action.getString("market") + " - " + action.getString("program") + " - " + action.getString("code");
                if (programMarket.containsKey(key)) {
                    if (!oldkey.equals(key)) {
                        oldTotal = 0.d;
                    }
                    oldkey = key;
                    oldTotal += safeGetDouble(action, "total", "RecapMarket");
                    excel.insertCellTabDouble(1 + programMarket.getInteger(key), i + 9, oldTotal);
                }
                }catch (NullPointerException e){
                    log.error("@LystoreWorker["+ this.getClass() +"] error in second for loop data : \n" + actions.getJsonObject(j));
                    throw e;
                }
            }
            if(i <= 1) {
                excel.autoSize(arrayLength+1);
            }
        }
        //Setting total
        excel.insertYellowLabel(0, operationsRowNumber, ExcelHelper.totalLabel);
        excel.fillTab(1, programMarket.size() + 1, 9, 9 + datas.size()); // init all empty tab cells

        for (int i = 1; i < programMarket.size() + 1; i++) {
            excel.setTotalX(9, operationsRowNumber - 1, i, operationsRowNumber);
        }

        excel.insertYellowLabel(programMarket.size() + 1, 8, ExcelHelper.totalLabel);

        for (int i = 0; i <= datas.size(); i++) {
            excel.setTotalY(1, programMarket.size(), 9 + i, programMarket.size() + 1);
        }


    }

    @Override
    protected void setLabels() {


        ArrayList<String> programsActionList = new ArrayList<>();
        String previousProgram = "";
        String previousMarket = "";
        CellRangeAddress merge;
        int initProgramX = 0;
        int endProgramX = 0;
        int initMarketX = 0;
        int endMarketX = 0;
        for (int i = 0; i < datas.size(); i++) {

            JsonObject operation = datas.getJsonObject(i);


            String actionStr = operation.getString("actions");
            JsonArray actions = new JsonArray(actionStr);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                //preparing keys
                String programMarketStr = action.getString("market") + " - " + action.getString("program") + " - " + action.getString("code");
                if (!programsActionList.contains(programMarketStr))//inserting the key filter
                    programsActionList.add(programMarketStr);
            }
            // Sorted array
            Collections.sort(programsActionList);


        }
        for (int i = 0; i < programsActionList.size(); i++) {
            String progM = programsActionList.get(i);
            //getting program and code separated
            String segments[] = progM.split(" - ");

            //merge region if same program
            if (previousMarket.equals(segments[0])) { //if same market megred region instead
                endMarketX = 1 + programMarket.size();
                if (previousProgram.equals(segments[1])) { // if same program adding merged instead
                    endMarketX = 1 + programMarket.size();

                } else {

                    previousProgram = segments[1];
                    if (initProgramX < endProgramX) {
                        merge = new CellRangeAddress(operationsRowNumber - 2, operationsRowNumber - 2, initProgramX, endProgramX);
                        sheet.addMergedRegion(merge);
                        excel.setRegionHeader(merge, sheet);
                    }
                    initProgramX = 3 + programMarket.size();
                    excel.insertYellowLabel(1 + programMarket.size(), operationsRowNumber - 2, segments[1]);
                }
            } else {
                previousMarket = segments[0];
                previousProgram = segments[1];
                if (initMarketX < endMarketX) {
                    merge = new CellRangeAddress(operationsRowNumber - 3, operationsRowNumber - 3, initMarketX, endMarketX);
                    sheet.addMergedRegion(merge);
                    excel.setRegionHeader(merge, sheet);
                }
                initMarketX = 1 + programMarket.size();
                excel.insertYellowLabel(1 + programMarket.size(), operationsRowNumber - 3, segments[0]);
                excel.insertYellowLabel(1 + programMarket.size(), operationsRowNumber - 2, segments[1]);
            }
            //always insert contract_type code
            excel.insertYellowLabel(1 + programMarket.size(), operationsRowNumber - 1, segments[2]);

            programMarket.put(progM, i);
        }

        //merge last region if there is one
        if (initProgramX < endProgramX) {
            merge = new CellRangeAddress(operationsRowNumber - 2, operationsRowNumber - 2, initProgramX, endProgramX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }
        if (initMarketX < endMarketX) {
            merge = new CellRangeAddress(operationsRowNumber - 3, operationsRowNumber - 3, initMarketX, endMarketX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }

        arrayLength += programMarket.size();
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
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,    " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,       " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation , " +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             union      " +
                "             (select oce.id," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id  and (orders.override_region != true OR orders.override_region is NULL))   " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id )                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        if (type.equals(CMR))
            query +=
                    "   AND (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "')  ";
        else {
            query +=
                    "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "')  " +
                            "     OR                    " +
                            " (spa.structure_type = '" + LYCEE + "' AND " +
                            "   ( specific_structures.type is null OR  specific_structures.type ='" + LYCEE + "') ))    ";
        }

        query +=
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                        "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                        "     WHERE    ";


        if (type.equals(CMR))
            query += "   specific_structures.type =  '" + CMR + "'   ";
        else {
            query += "  specific_structures.type !=  '" + CMR + "'   " +
                    "  OR specific_structures.type is null " +
                    "  OR specific_structures.type !=  '" + LYCEE + "'   " ;
        }
        query +=
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                        "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                        "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region " +
                        "             order by market, program,code,orders.id_operation     )        " +
                        "SELECT  values.operation as label, array_to_json(array_agg(values)) as actions  " +
                        "FROM values" +

                        " Group by label  ;";

        sqlHandler(handler);
    }
}
