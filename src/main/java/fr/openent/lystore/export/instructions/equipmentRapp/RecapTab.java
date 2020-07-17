package fr.openent.lystore.export.instructions.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.Collections;

public class RecapTab extends TabHelper {
    String type = "";
    protected int operationsRowNumber = 2;
    private String actionStr = "actions";
    private JsonObject programLabel = new JsonObject();


    public RecapTab(Workbook workbook, JsonObject instruction, String type) {
        super(workbook, instruction, "RECAP - " + type);
        this.type = type;
        excel.setDefaultFont();
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
                        //Delete tab if empty
                        try{

                            setLabels();
                            setArray(datas);
                            handler.handle(new Either.Right<>(true));

                        }catch(Exception ee){
                            logger.error(ee.getMessage());
                            logger.error(ee.getStackTrace());
                            handler.handle(new Either.Left<>("error when creating excel"));
                        }
                    }
                }
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });

    }

    //    /**
//     * Set labels of the tabs
//     */
    @Override
    protected void setLabels() {
        int cellLabelColumn = 0;
        int programRowNumber = 0;
        String previousProgram = "";
        int initProgramX = 0;
        int endProgramX = 0;
        cellColumn = 2;
        ArrayList<String> programsActionList = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {

            JsonObject operation = datas.getJsonObject(i);
            String actionsStrToArray = operation.getString(actionStr);
            String labelOperation = operation.getString("label");

            excel.insertLabel(cellLabelColumn, this.operationsRowNumber, operation.getLong("id").toString());
            excel.insertLabel(cellLabelColumn + 1, this.operationsRowNumber, labelOperation);


            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;

            for (int j = 0; j < actions.size(); j++) {
                try{
                JsonObject action = actions.getJsonObject(j);
                String program = action.getString("program");
                String code = action.getString("code");
                String key = program + " - " + code;
                if (!programsActionList.contains(key))
                    programsActionList.add(key);
                }catch (NullPointerException e){
                    log.error("@LystoreWorker["+ this.getClass() +"] error in second for loop data : \n" + actions.getJsonObject(j));
                    throw e;
                }
            }
            Collections.sort(programsActionList);


            operationsRowNumber++;
        }

        for (int j = 0; j < programsActionList.size(); j++) {
            String key = programsActionList.get(j);
            //getting program and code separated
            String segments[] = key.split(" - ");
            String program = segments[0];
            String code = segments[1];
            if (!programLabel.containsKey(key)) {
                programLabel.put(key, programLabel.size());
            }

            if (previousProgram.equals(program)) {
                endProgramX = cellColumn;
            } else {
                previousProgram = program;
                if (initProgramX < endProgramX) {
                    CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, initProgramX, endProgramX);
                    sheet.addMergedRegion(merge);
                    excel.setRegionHeader(merge, sheet);
                }
                initProgramX = cellColumn;
                excel.insertHeader(cellColumn, programRowNumber, program);
            }
            excel.insertHeader(cellColumn, programRowNumber + 1, code);
            cellColumn++;

        }
        if (initProgramX < endProgramX) {
            CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, initProgramX, endProgramX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }
    }

    @Override
    protected void setArray(JsonArray datas) {

        for (int i = 0; i < datas.size(); i++) {
            JsonObject operation = datas.getJsonObject(i);

            String actionsStrToArray = operation.getString(actionStr);
            JsonArray actions = new JsonArray(actionsStrToArray);
            JsonObject oldTotals = new JsonObject();

            for (int j = 0; j < actions.size(); j++) {

                JsonObject action = actions.getJsonObject(j);
                String key = action.getString("program") + " - " + action.getString("code");
                if (!oldTotals.containsKey(key)) {
                    oldTotals.put(key, safeGetDouble(action, "total", "RecapTab"));
                } else {
                    oldTotals.put(key, safeGetDouble(action, "total", "RecapTab") + safeGetDouble(oldTotals, key, "RecapTab"));
                }
                excel.insertCellTabDouble(programLabel.getInteger(key) + 2,
                        2 + i,
                        safeGetDouble(oldTotals, key, "RecapTab"));
            }
            if(i <= 1 ){
                excel.autoSize(programLabel.size() + 3);
            }
        }

        excel.fillTab(2, programLabel.size() + 2, 2, operationsRowNumber);
        excel.insertHeader(1, operationsRowNumber, excel.totalLabel);

        for (int i = 0; i < programLabel.size(); i++) {
            excel.setTotalX(2, operationsRowNumber - 1, i + 2, operationsRowNumber);
        }

        excel.insertHeader(programLabel.size() + 2, 1, excel.totalLabel);

        for (int i = 0; i <= datas.size(); i++) {
            excel.setTotalY(2, programLabel.size() + 1, 2 + i, programLabel.size() + 2);
        }

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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
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
                        "             order by  program,code   " +
                        "  )    SELECT values.id_operation as id, values.operation as label,    array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket       " +
                        "  from  values      " +
                        "  Group by values.id_operation, values.operation   " +
                        "  Order by values.operation ;";

        sqlHandler(handler);

    }


}

