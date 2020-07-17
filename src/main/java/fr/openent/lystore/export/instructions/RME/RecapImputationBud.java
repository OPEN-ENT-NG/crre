package fr.openent.lystore.export.instructions.RME;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class RecapImputationBud extends TabHelper {
    private final int xTab = 0;
    private final int yTab = 7;
    private int nbToMerge = 1;

    public RecapImputationBud(Workbook workbook, JsonObject instruction) {

        super(workbook, instruction, TabName.IMPUTATION_BUDG.toString());
        excel.setDefaultFont();
        excel.setInstructionNumber(makeCellWithoutNull(instruction.getString("cp_number")));
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        getDatas(event -> {
            try{
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve datas"));
                    return;
                }
                if (checkEmpty()) {
                    Row row = sheet.getRow(1);
                    sheet.removeRow(row);
                    row = sheet.getRow(4);
                    sheet.removeRow(row);
                    row = sheet.getRow(6);
                    sheet.removeRow(row);
                    handler.handle(new Either.Right<>(true));
                } else {
                    setArray(datas);
                    handler.handle(new Either.Right<>(true));
                }
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
    }
    @Override
    protected void setArray(JsonArray datas) {
        JsonObject program;
        String oldSection = "";
        for (int i = 0; i < datas.size(); i++) {
            program = datas.getJsonObject(i);
            oldSection = insertSetion(program.getString("section"), oldSection, xTab, yTab + i,false);
            excel.insertCellTab(xTab + 1, yTab + i, program.getInteger("chapter").toString() + " - " + program.getString("chapter_label"));
            excel.insertCellTab(xTab + 2, yTab + i, program.getInteger("functional_code").toString() + " - " + program.getString("code_label"));
            excel.insertCellTab(xTab + 3, yTab + i, program.getString("program_name"));
            excel.insertCellTab(xTab + 4, yTab + i, program.getString("program_label"));
            excel.insertCellTabCenter(xTab + 5, yTab + i, program.getString("action_code"));
            excel.insertCellTab(xTab + 6, yTab + i, program.getString("action_name"));
            excel.insertCellTabDoubleWithPrice(xTab + 7, yTab + i, Double.parseDouble(program.getString("total")));


        }
        insertSetion(";",oldSection,xTab,yTab+datas.size(), true);
    }

    private String insertSetion(String section, String oldSection, int xTab, int y,boolean last) {
        if (!section.equals(oldSection)) {
            if(!last)
                excel.insertHeader(xTab, y, section);
            if (y - nbToMerge != y - 1) {
                CellRangeAddress merge = new CellRangeAddress(y - nbToMerge, y - 1, xTab, xTab);
                sheet.addMergedRegion(merge);
                nbToMerge = 1;
            }

            oldSection = section;

        } else {
                excel.insertHeader(xTab, y, section);
            nbToMerge++;
        }
        return oldSection;
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " WITH values AS  " +
                "(  " +
                "       SELECT orders.id ,  " +
                "              program.section AS section,  " +
                "              program.functional_code,  " +
                "              functional_code.label AS code_label ,  " +
                "              program.chapter,  " +
                "              chapter.label AS chapter_label,  " +
                "              orders.\"price TTC\",  " +
                "              Round((  " +
                "                       (  " +
                "                       SELECT  " +
                "                              CASE  " +
                "                                     WHEN orders.price_proposal IS NOT NULL THEN 0  " +
                "                                     WHEN orders.override_region IS NULL THEN 0  " +
                "                                     WHEN Sum(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0 " +
                "                                     ELSE Sum(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "                              END  " +
                "                       FROM   " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "                       WHERE  oco.id_order_client_equipment = orders.id ) + orders.\"price TTC\" ) * orders.amount ,2 ) AS total, " +
                "              contract.NAME                                                                                           AS market, " +
                "              contract_type.code                                                                                      AS code, " +
                "              program.NAME                                                                                            AS program, " +
                "              CASE  " +
                "                     WHEN orders.id_order_client_equipment IS NOT NULL THEN  " +
                "                            (  " +
                "                                   SELECT oce.NAME  " +
                "                                   FROM   " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "                                   WHERE  oce.id = orders.id_order_client_equipment limit 1)  " +
                "                     ELSE orders.NAME  " +
                "              END AS old_name,  " +
                "              orders.id_structure,  " +
                "              orders.id_operation  AS id_operation,  " +
                "              label.label          AS operation ,  " +
                "              orders.equipment_key AS KEY,  " +
                "              orders.NAME          AS name_equipment,  " +
                "              true                 AS region,  " +
                "              orders.isregion,  " +
                "              program_action.id_program,  " +
                "              orders.amount ,  " +
                "              contract.id                AS market_id,  " +
                "              contract_type.code         AS contract_code,  " +
                "              contract_type.NAME         AS contract_name,  " +
                "              contract_type.id           AS contract_type_id,  " +
                "              program_action.action      AS action_code,  " +
                "              program_action.description AS action_name,  " +
                "              program_action.id          AS action_id,  " +
                "              program.NAME               AS program_name,  " +
                "              program.id                 AS program_id,  " +
                "              program.label              AS program_label,  " +
                "              orders.id_structure,  " +
                "              orders.amount  AS amount,  " +
                "              orders.NAME    AS label,  " +
                "              orders.comment AS comment,  " +
                "              CASE  " +
                "                     WHEN specific_structures.type IS NULL THEN '" + LYCEE + "'  " +
                "                     ELSE specific_structures.type  " +
                "              END AS cite_mixte  " +
                "       FROM   (  " +
                "              (  " +
                "                     SELECT ore.id,  " +
                "                            true      AS isregion,  " +
                "                            ore.price AS \"price TTC\",  " +
                "                            ore.amount,  " +
                "                            ore.creation_date,  " +
                "                            ore.modification_date,  " +
                "                            ore.NAME,  " +
                "                            ore.summary,  " +
                "                            ore.description,  " +
                "                            ore.image,  " +
                "                            ore.status,  " +
                "                            ore.id_contract,  " +
                "                            ore.equipment_key,  " +
                "                            ore.id_campaign,  " +
                "                            ore.id_structure,  " +
                "                            ore.cause_status,  " +
                "                            ore.number_validation,  " +
                "                            ore.id_order,  " +
                "                            ore.comment,  " +
                "                            ore.rank AS \"prio\",  " +
                "                            NULL     AS price_proposal,  " +
                "                            ore.id_project,  " +
                "                            ore.id_order_client_equipment,  " +
                "                            NULL AS program,  " +
                "                            NULL AS action,  " +
                "                            ore.id_operation ,  " +
                "                            NULL AS override_region  " +
                "                     FROM   " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )  " +
                "       UNION  " +
                "                  (  " +
                "                         SELECT oce.id ,  " +
                "                                false AS isregion,  " +
                "                                CASE  " +
                "                                       WHEN price_proposal IS NULL THEN price + (price*tax_amount/100) " +
                "                                       ELSE price_proposal  " +
                "                                END AS \"price TTC\",  " +
                "                                amount,  " +
                "                                creation_date,  " +
                "                                NULL AS modification_date,  " +
                "                                NAME,  " +
                "                                summary,  " +
                "                                description,  " +
                "                                image,  " +
                "                                status,  " +
                "                                id_contract,  " +
                "                                equipment_key,  " +
                "                                id_campaign,  " +
                "                                id_structure,  " +
                "                                cause_status,  " +
                "                                number_validation,  " +
                "                                id_order,  " +
                "                                comment,  " +
                "                                rank AS \"prio\",  " +
                "                                price_proposal,  " +
                "                                id_project,  " +
                "                                NULL AS id_order_client_equipment,  " +
                "                                program,  " +
                "                                action,  " +
                "                                id_operation,  " +
                "                                override_region  " +
                "                         FROM   " + Lystore.lystoreSchema + ".order_client_equipment oce) ) AS orders  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation  " +
                "   ON         (  " +
                "                         orders.id_operation = operation.id  " +
                "              AND        (  " +
                "                                    orders.override_region != true  " +
                "                         OR         orders.override_region IS NULL))  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".label_operation AS label  " +
                "   ON         (  " +
                "                         operation.id_label = label.id)  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction  " +
                "   ON         (  " +
                "                         operation.id_instruction = instruction.id  " +
                "              AND        instruction.id = ?)  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract  " +
                "   ON         (  " +
                "                         orders.id_contract = contract.id )  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type  " +
                "   ON         (  " +
                "                         contract.id_contract_type = contract_type.id)  " +
                "   LEFT JOIN  " + Lystore.lystoreSchema + ".specific_structures  " +
                "   ON         orders.id_structure = specific_structures.id  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action spa  " +
                "   ON         (  " +
                "                         spa.contract_type_id = contract_type.id)  " +
                "   AND        ((  " +
                "                                    spa.structure_type = '" + CMD + "'  " +
                "                         AND        specific_structures.type ='" + CMD + "')  " +
                "              OR         (  " +
                "                                    spa.structure_type = '" + CMR + "'  " +
                "                         AND        specific_structures.type ='" + CMR + "')  " +
                "              OR         (  " +
                "                                    spa.structure_type = '" + LYCEE + "'  " +
                "                         AND        (  " +
                "                                               specific_structures.type IS NULL  " +
                "                                    OR         specific_structures.type = '" + LYCEE + "' )))  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action  " +
                "   ON         (  " +
                "                         spa.program_action_id = program_action.id)  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program  " +
                "   ON         program_action.id_program = program.id  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".functional_code  " +
                "   ON         (  " +
                "                         functional_code.code = program.functional_code )  " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".chapter  " +
                "   ON         (  " +
                "                         chapter.code = program.chapter )  " +
                "   GROUP BY   contract_name,  " +
                "              contract_type.id,  " +
                "              action_code,  " +
                "              action_name,  " +
                "              action_id,  " +
                "              program_name,  " +
                "              program_id,  " +
                "              program_label,  " +
                "              orders.comment,  " +
                "              program.NAME,  " +
                "              specific_structures.type ,  " +
                "              orders.amount ,  " +
                "              orders.NAME,  " +
                "              functional_code.label,  " +
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
                "              orders.isregion ,  " +
                "              chapter_label,  " +
                "              chapter  " +
                "   ORDER BY   orders.id )  " +
                "SELECT   action_code,  " +
                "         action_name,  " +
                "         action_id,  " +
                "         section,  " +
                "         program_name,  " +
                "         program_id,  " +
                "         program_label,  " +
                "         functional_code,  " +
                "         chapter,  " +
                "         chapter_label,  " +
                "         code_label,  " +
                "         sum(total) AS total  " +
                "FROM     VALUES  " +
                "GROUP BY action_code,  " +
                "         action_name,  " +
                "         action_id,  " +
                "         section,  " +
                "         program_name,  " +
                "         program_id,  " +
                "         program_label,  " +
                "         functional_code,  " +
                "         chapter,  " +
                "         chapter_label,  " +
                "         code_label" +
                " ORDER BY section DESC, " +
                "   program_name, " +
                "   action_code";

       sqlHandler(handler);
    }

}
