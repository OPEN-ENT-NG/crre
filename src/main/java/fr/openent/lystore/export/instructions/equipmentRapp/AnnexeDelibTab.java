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

public class AnnexeDelibTab extends TabHelper {
    private String type;
    private JsonObject programMarket;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */
    public AnnexeDelibTab(Workbook wb, JsonObject instruction, String type) {
        super(wb, instruction, "ANNEXE DELIB");
        this.type = type;
        programMarket = new JsonObject();
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
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));

        }
        getStructures(new JsonArray(structuresId),getStructureHandler(structuresId,handler));
    }

    @Override
    protected void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        setArray(datas);
    }

    @Override
    protected void setArray(JsonArray datas) {
        setLabels();
        datas = sortByCity(datas, true);
        if (datas.isEmpty()) {
            return;
        }
        int lineToInsert = 5;
        JsonObject idPassed = new JsonObject();
        String key = "", oldkey = "";
        Double oldTotal = 0.d;
        for (int i = 0; i < datas.size(); i++) {
            try{

                JsonObject action = datas.getJsonObject(i);
                key = action.getString("program") + " - " + action.getString("code");
                int columnToInsert = programMarket.getInteger(key);
                if (!checkIdPassed(idPassed, action.getString("id_structure"))) {
                    idPassed.put(action.getString("id_structure"), 1);
                    lineToInsert++;
                    excel.insertCellTab(0, lineToInsert, action.getString("zipCode"));
                    excel.insertCellTab(1, lineToInsert, action.getString("city"));
                    excel.insertCellTab(2, lineToInsert, action.getString("nameEtab"));
                    excel.insertCellTab(3, lineToInsert, action.getString("uai"));
                    oldkey = "";
                }
                if (!oldkey.equals(key)) {
                    oldTotal = 0.d;
                }
                oldkey = key;
                oldTotal += Double.parseDouble(action.getString("total"));

                excel.insertCellTabDouble(columnToInsert + 4, lineToInsert, oldTotal);
            }catch (NullPointerException e){
                log.error("@LystoreWorker["+ this.getClass() +"] error in second for loop data : \n" + datas.getJsonObject(i));
                throw e;
            }
            if(i<=1)
                excel.autoSize(arrayLength + 1);
        }

        excel.fillTab(0, arrayLength, 6, lineToInsert + 1);
        for (int i = 6; i <= lineToInsert; i++) {
            excel.setTotalY(4, 4 + programMarket.size() - 1, i, 4 + programMarket.size());
        }

        for (int i = 0; i <= programMarket.size(); i++) {
            excel.insertHeader(3, lineToInsert + 1, excel.totalLabel);
            excel.setTotalX(6, lineToInsert, 4 + i, lineToInsert + 1);

        }
    }

    private boolean checkIdPassed(JsonObject idPassed, String id) {
        return idPassed.containsKey(id);
    }

    @Override
    protected void setLabels() {
        ArrayList<String> programsActionList = new ArrayList<>();
        int initProgramX = 0;
        int endProgramX = 0;
        String previousProgram = "";
        excel.insertHeader(1, 1, "ANNEXE A LA DELIBERATION");
        CellRangeAddress merge = new CellRangeAddress(1, 1, 1, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);

        excel.insertHeader(1, 4, "COMMUNE");
        excel.insertHeader(1, 5, "");

        excel.insertHeader(2, 4, "LYCEE");
        excel.insertHeader(2, 5, "");
        excel.insertHeader(3, 4, "UAI");
        excel.insertHeader(3, 5, "");

        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String programMarketStr = data.getString("program") + " - " + data.getString("code");
            if (!programsActionList.contains(programMarketStr))
                programsActionList.add(programMarketStr);
        }
        Collections.sort(programsActionList);
        // Getting merged region
        for (int i = 0; i < programsActionList.size(); i++) {
            String progM = programsActionList.get(i);
            //getting program and code separated
            String segments[] = progM.split(" - ");

            //merge region if same program
            if (previousProgram.equals(segments[0])) {
                endProgramX = 4 + programMarket.size();

            } else {
                previousProgram = segments[0];
                if (initProgramX < endProgramX) {
                    merge = new CellRangeAddress(4, 4, initProgramX, endProgramX);
                    sheet.addMergedRegion(merge);
                    excel.setRegionHeader(merge, sheet);
                }
                initProgramX = 4 + programMarket.size();
                excel.insertHeader(4 + programMarket.size(), 4, segments[0]);
            }
            excel.insertHeader(4 + programMarket.size(), 5, segments[1]);
            programMarket.put(progM, i);
        }

        if (initProgramX < endProgramX) {
            merge = new CellRangeAddress(4, 4, initProgramX, endProgramX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }

        arrayLength += programMarket.size();
        excel.insertHeader(4 + programMarket.size(), 4, excel.totalLabel);
        excel.insertHeader(4 + programMarket.size(), 5, "");

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
                "             as Total, contract.name as market, contract_type.code as code, campaign.name as campaign,   " +
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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id  and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id )                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             INNER JOIN " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
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
                        "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region ,campaign" +
                        "             order by orders.id_structure    )        " +
                        "  SELECT values.*    " +
                        " from values  " +
                        " order by id_structure,program  " +
                        " ;  ";


        sqlHandler(handler);

    }
}