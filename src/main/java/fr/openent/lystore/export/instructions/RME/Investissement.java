package fr.openent.lystore.export.instructions.RME;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;

public abstract class Investissement extends TabHelper {

    JsonArray operations;
    private String actionStr = "actions";
    public Investissement(Workbook wb, JsonObject instruction, String TabName) {
        super(wb, instruction, TabName);
    }


    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setInstructionNumber(makeCellWithoutNull(instruction.getString("cp_number")));
        getDatas(event -> {
            try{
                if (event.isLeft()) {
                    log.error("Failed to retrieve programs");
                    handler.handle(new Either.Left<>("Failed to retrieve programs"));
                } else {
                    if (checkEmpty()) {
                        Row row = sheet.getRow(1);
                        sheet.removeRow(row);
                        row = sheet.getRow(2);
                        sheet.removeRow(row);
                        row = sheet.getRow(4);
                        sheet.removeRow(row);
                        row = sheet.getRow(7);
                        sheet.removeRow(row);
                        row = sheet.getRow(8);
                        sheet.removeRow(row);
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




    /**
     * Set labels of the tabs
     */
    @Override
    protected void setLabels() {
        int cellLabelColumn = 0;

        if (this.instruction.getJsonArray("operations").isEmpty()) {
            return;
        }
        operations = this.instruction.getJsonArray("operations");
        for (int i = 0; i < operations.size(); i++) {
            JsonObject operation = operations.getJsonObject(i);
            taby.add(operation.getInteger("id"));
            excel.insertLabel(cellLabelColumn, this.operationsRowNumber,operation.getString("label"));
            this.operationsRowNumber++;
        }
        excel.insertHeader(cellLabelColumn,this.operationsRowNumber,  excel.totalLabel);

    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        sqlHandler(handler);
    }

    /**
     * Set the headers of tab for investissement
     *
     * @param programs
     */
    @Override
    protected void setArray(JsonArray programs) {
        int posx = 0;
        String code = "-1";

        int numberActions;
        int programRowNumber = 6;
        if (programs.isEmpty()) {
            return;
        }

        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);

            JsonArray actions = new JsonArray(program.getString(actionStr));
            excel.insertHeader( cellColumn,programRowNumber, program.getString("name"));
            numberActions = nbAction(actions);
            //check if merged region necessary
            if (numberActions != 1) {
                CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, cellColumn, cellColumn + numberActions - 1);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);
            }

            posx += treatActions(actions, code, posx, programRowNumber);
            excel.fillTab(xTab, this.cellColumn, yTab, this.operationsRowNumber);
        }
        excel.setTotal(yTab, operationsRowNumber, xTab, cellColumn);
        CellRangeAddress totalMerge = new CellRangeAddress(programRowNumber, programRowNumber + 2, cellColumn, cellColumn);
        sheet.addMergedRegion(totalMerge);
        excel.setRegionHeader(totalMerge, sheet);
        excel.insertHeader(cellColumn,programRowNumber,  excel.totalLabel);

    }

    private int treatActions(JsonArray actions, String code, int posx, int programRowNumber) {
        JsonObject totalArray = new JsonObject();
        ArrayList<String> keys= new ArrayList<>();
        for (int j = 0; j < actions.size(); j++) {
            JsonObject action = actions.getJsonObject(j);
            if (!code.equals(action.getString("code"))) {
                code = action.getString("code");
                if (!tabx.containsKey(action.getInteger("id_program").toString() + "-" + action.getString("code"))) {
                    tabx.put(action.getInteger("id_program").toString() + "-" + action.getString("code"), posx);
                    posx++;
                }

                excel.insertHeader( cellColumn,programRowNumber + 1, action.getString("contract_name"));
                excel.insertHeader( cellColumn,programRowNumber + 2, action.getString("code"));
                this.cellColumn++;

            }

            for (int oIndex = 0; oIndex < operations.size(); oIndex++) {
                JsonObject operation = operations.getJsonObject(oIndex);
                if (action.getInteger("id_operation").equals(operation.getInteger("id"))) {
                   int  amountOfKey= oIndex + yTab;
                    String key= posx + "-"+ amountOfKey;
                    if(!totalArray.containsKey(key)) {
                        keys.add(key);
                        totalArray.put(key, action.getDouble("total"));
                    }
                    else{
                        totalArray.put(key, action.getDouble("total") + totalArray.getDouble(key));
                    }
                }
            }
        }

        for (String key : keys) {
            String[] coordonates = key.split("-");
            String x = coordonates[0];
            String y = coordonates[1];
            excel.insertCellTabDouble(Integer.parseInt(x), Integer.parseInt(y), totalArray.getDouble(key));
        }

        return posx;
    }

    private int nbAction(JsonArray actions) {
        int nbActions = 0;
        String idAction = "-1";
        for (int i = 0; i < actions.size(); i++) {
            if (!idAction.equals(actions.getJsonObject(i).getString("code"))) {
                idAction = actions.getJsonObject(i).getString("code");
                nbActions++;
            }
        }
        return nbActions;
    }
}
