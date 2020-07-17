package fr.openent.lystore.export.instructions.RME;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class RecapEPLETab extends TabHelper {

    private int yProgramLabel = 0;
    private final int xProgramLabel = 0;
    private String programLabel = "Programme : ";
    private String totalLabel = "Montant : ";
    private String contractType = "Nature comptable : ";
    private String actionLabel = "Action : ";
    private String orderLabel = "Libellé Demande";
    private String orderComment = "Demande Commentaire";
    private String orderAmount = "Quantité Accordée";
    private String orderTotal = "Somme Montant Accordé";
    private String id_structureStr = "id_structure";
    private boolean notFirstTab = false;
    private boolean notFirstPart = false;

    private int program_id = -1, action_id = -1, contract_id = -1;
    private int xlabel = 0;
    private int yTotalLabel = 3;

    private ArrayList<String> structuresId;
    private StructureService structureService;
    private int nbLine = 0;
    private String totalLabelInt = "";

    public RecapEPLETab(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, TabName.EPLE.toString());
        structuresId = new ArrayList<>();
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setInstructionNumber(makeCellWithoutNull(instruction.getString("cp_number")));
        getDatas(event -> {
            try {
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve  datas"));
                    return;
                }
                if (checkEmpty()) {
                    Row row = sheet.getRow(1);
                    sheet.removeRow(row);
                    row = sheet.getRow(3);
                    sheet.removeRow(row);
                    handler.handle(new Either.Right<>(true));
                } else {
                    getAndSetDatas(handler);
                }
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
    }

    private void getAndSetDatas(Handler<Either<String, Boolean>> handler) {
        JsonObject program;
        for (int i = 0; i <  datas.size(); i++) {
            program =  datas.getJsonObject(i);
            makeCellWithoutNull( program.getString("comment"));
            if (!structuresId.contains(program.getString(id_structureStr)))
                structuresId.add(structuresId.size(), program.getString(id_structureStr));
        }
        log.info("Array structures id to send SIZE : " + structuresId.size());
        getStructures(new JsonArray(structuresId), getStructureHandler(structuresId,handler));
    }
    @Override
    protected void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        setTab();
    }
    private void setTab() {
        JsonObject program;
        String id_structure = "";

        datas = sort(datas);
        for (int i = 0; i <  datas.size(); i++) {
            program =  datas.getJsonObject(i);

            if (!(id_structure.equals(program.getString(id_structureStr)))) {
                id_structure = newTab(program, false);
            }
            sheet.createRow(yProgramLabel + 3);
            excel.insertCellTab(0, yProgramLabel + 3, program.getString("label"));
            excel.insertCellTab(1, yProgramLabel + 3, makeCellWithoutNull(program.getString("comment")));
            excel.insertCellTabInt(2, yProgramLabel + 3, Integer.parseInt(program.getInteger("amount").toString()));
            excel.insertCellTabDouble(3, yProgramLabel + 3, Double.parseDouble(program.getString("total")));
            yProgramLabel++;
            nbLine++;

        }
        if ( datas.size() > 0) {
            newTab( datas.getJsonObject( datas.size() - 1), true);
            settingSumLabel();
        }
    }

    private JsonArray sort(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        values = sortByCity(values, false);

            List<JsonObject> jsonValues = new ArrayList<JsonObject>();
            for (int i = 0; i < values.size(); i++) {
                jsonValues.add(values.getJsonObject(i));
            }

            Collections.sort(jsonValues, new Comparator<JsonObject>() {
                private static final String KEY_NAME = "program_name";

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
                            valA = a.getString(KEY_NAME);
                        }
                        if (b.containsKey(KEY_NAME)) {
                            valB = b.getString(KEY_NAME);
                        }
                    } catch (NullPointerException e) {
                        log.error("error when sorting structures during export");
                    }
                    if (valA.compareTo(valB) == 0) {
                        if (a.containsKey("action_code")) {
                            cityA = a.getString("action_code");
                        }
                        if (b.containsKey("action_code")) {
                            cityB = b.getString("action_code");
                        }
                        if (cityA.compareTo(cityB) == 0) {
                            if (a.containsKey("contract_code")) {
                                nameA = a.getString("contract_code");
                            }
                            if (b.containsKey("contract_code")) {
                                nameB = b.getString("contract_code");
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


    private String newTab(JsonObject program, boolean isLast) {


        String id_structure = "", zipCode, city, uai, nameEtab;
        CellRangeAddress merge;
        if (notFirstTab) {
            merge = new CellRangeAddress(yProgramLabel + 3, yProgramLabel + 5, 0, 1);
            sheet.addMergedRegion(merge);
            merge = new CellRangeAddress(yProgramLabel + 4, yProgramLabel + 5, 2, 3);
            sheet.addMergedRegion(merge);
            excel.insertHeader(2, yProgramLabel + 3, excel.sumLabel);
            excel.setTotalX(yProgramLabel + 2 - nbLine, yProgramLabel + 2, 3, yProgramLabel + 3);
            addTotalLabelInt(excel.getCellReference(yProgramLabel + 3, 3) + " +");
            nbLine = 0;


        } else {
            notFirstTab = true;
        }
        if (!isLast) {
            yProgramLabel += 3;

            if (program_id != program.getInteger("program_id") || action_id != program.getInteger("action_id") || contract_id != program.getInteger("contract_type_id")) {
                program_id = program.getInteger("program_id");
                action_id = program.getInteger("action_id");
                contract_id = program.getInteger("contract_type_id");
                if (notFirstPart) { //adding sum
                    settingSumLabel();
                }
                setLabelHead(program);
                notFirstPart = true;
            }
//            //adding  new label
            id_structure = program.getString(id_structureStr);
            nameEtab = program.getString("nameEtab");
            uai = program.getString("uai");
            zipCode = program.getString("zipCode");
            city = program.getString("city");
            excel.insertHeader( 0, yProgramLabel + 3,zipCode + " - " + city + " - " + nameEtab + " (" + uai + ")");
            merge = new CellRangeAddress(yProgramLabel + 3, yProgramLabel + 3, 0, 3);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
            excel.insertHeader( 0, yProgramLabel + 4,orderLabel);
            excel.insertHeader( 1, yProgramLabel + 4,orderComment);
            excel.insertHeader( 2, yProgramLabel + 4,orderAmount);
            excel.insertHeader( 3, yProgramLabel + 4,orderTotal);
            yProgramLabel += 2;
        }
        return id_structure;

    }

    private void addTotalLabelInt(String s) {
        String cellToAdd = excel.getCellReference(yProgramLabel + 3, 3) + " +";
        cellToAdd = cellToAdd.replace("'Récap. EPLE'!", "");
        if (totalLabelInt.length() + cellToAdd.length() < LIMIT_FORMULA_SIZE) {
            totalLabelInt += cellToAdd;
        } else {
            totalLabelInt = totalLabelInt.substring(0, totalLabelInt.length() - 1);
            excel.insertFormula(1589, yProgramLabel + 3, totalLabelInt);
            totalLabelInt = excel.getCellReference(yProgramLabel + 3, 1589) + " +" + cellToAdd;
        }
    }

    private void settingSumLabel() {
        totalLabelInt = totalLabelInt.substring(0, totalLabelInt.length() - 1);
        excel.insertFormula(yTotalLabel, xlabel, totalLabelInt);
        totalLabelInt = "";
    }

    public void setLabelHead(JsonObject program) {
        yProgramLabel += 4;

        excel.insertLabelHead( xProgramLabel,yProgramLabel,
                programLabel + program.getString("program_name") + " " + program.getString("program_label"));
        excel.insertLabelHead( xProgramLabel,yProgramLabel + 2,
                actionLabel + program.getString("action_code") + " - " + program.getString("action_name"));
        excel.insertLabelHead (xProgramLabel + 1,yProgramLabel,
                contractType + program.getString("contract_code") + " - " + program.getString("contract_name"));
        excel.insertLabelHead(xProgramLabel + 2,yProgramLabel, totalLabel);
        xlabel = yProgramLabel;
        yProgramLabel += 2;

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
                "             ELSE orders.name      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region, orders.isregion,    " +
                "             program_action.id_program, orders.amount ,contract.id as market_id, " +
                "               contract_type.code                     AS contract_code,  " +
                "                          contract_type.NAME         AS contract_name,  " +
                "                          contract_type.id           AS contract_type_id,  " +
                "                          program_action.action      AS action_code,  " +
                "                          program_action.description AS action_name,  " +
                "                          program_action.id          AS action_id,  " +
                "                          program.NAME               AS program_name,  " +
                "                          program.id                 AS program_id,  " +
                "                          program.label              AS program_label,  " +
                "                          orders.amount                 AS amount,  " +
                "                          orders.NAME                   AS label,  " +
                "                          orders.comment                AS comment,        " +
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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id    and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id )                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         "+
                "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "     OR  (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                "      OR (spa.structure_type = '" + LYCEE + "' AND ( specific_structures.type is null OR specific_structures.type = '"+ LYCEE+"' )))    "+
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                        "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                "               GROUP BY    " +
                "           contract_name,  " +
                "                       contract_type.id,  " +
                "                        action_code,  " +
                "                        action_name,  " +
                "      action_id,  " +
                "                         program_name,  " +
                "                          program_id,  " +
                "                          program_label,  " +
                "              orders.comment, " +
                "              program.NAME,  " +
                "              code,  " +
                "              specific_structures.type ,  " +
                "              orders.amount ,  " +
                "              orders.NAME,  " +
                "              orders.equipment_key ,  " +
                "              orders.id_operation,  " +
                "              orders.id_structure ,  " +
                "              orders.id,  " +
                "              contract.id ,  " +
                "              label.label ,  " +
                "              program_action.id_program ,  " +
                "              orders.id_order_client_equipment,  " +
                "              orders.\"price TTC\",  " +
                "              orders.price_proposal,  " +
                "              orders.override_region,  " +
                "              orders.isregion " +
                        "             order by  program,code,orders.id_operation     )        " +
                        "SELECT  values.*" +
                "  from values " +
                "  order by program_name, " +
                "  action_code, " +
                "  contract_code, " +
                "  id_structure; ";


      sqlHandler(handler);
    }

}

