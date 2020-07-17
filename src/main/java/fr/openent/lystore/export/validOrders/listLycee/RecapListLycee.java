package fr.openent.lystore.export.validOrders.listLycee;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class RecapListLycee extends TabHelper {
    private String numberValidation;
    private ArrayList<Integer> totalsXQty = new ArrayList<>();
    public RecapListLycee(Workbook workbook, String numberValidation) {
        super(workbook,"Liste Commandes avec Prix");
        this.numberValidation = numberValidation;
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
        getStructures(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                boolean errorCatch= false;
                if (repStructures.isRight()) {
                    String errorMessage = "";
                    try {
                        JsonArray structures = repStructures.right().getValue();
                        setStructuresFromDatas(structures);
                        if (datas.isEmpty()) {
                            handler.handle(new Either.Left<>("No data in database"));
                        } else {
                            datas = sortByCity(datas, false);
                            writeArray(handler);
                        }
                    }catch (Exception e){
                        errorCatch = true;
                        errorMessage = e.getMessage();
                    }
                    if(errorCatch)
                        handler.handle(new Either.Left<>("\n Error Exception in RecapListLycee : "+ errorMessage));
                    else
                        handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));
                }
            }
        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        setTitle();

        String oldIdStruct = "";
        int currentI = 2,initx = 2 ;
        String typeEquipment;
        String oldUai = "";
        for(int i=0;i<datas.size();i++){
            JsonObject data = datas.getJsonObject(i);
            String idStruct = data.getString("id_structure");
            if (!oldIdStruct.equals(idStruct)){
                oldIdStruct = idStruct;
                if(initx != currentI){
                    mergeStructures(currentI, initx);
                    excel.insertWithStyle(0,currentI,"Total " + oldUai,excel.labelHeadStyle);
                }
                if(i!=0){
                    int oldinitx = initx;
                    initx = inserTotal(initx,currentI);
                    fillYellowCells(currentI);
                    mergeFinalCells(currentI, oldinitx);
                    currentI ++;
                }
                insertStructureInfos(currentI, data);
            }

            excel.insertWithStyle(4,currentI,makeCellWithoutNull(data.getString("name")),excel.tabStringStyleCenterBold);
            excel.insertWithStyle(5,currentI, Integer.parseInt(data.getString("amount")),excel.tabStringStyleRight);

            typeEquipment = data.getString("typeequipment");
            Double priceAmount = Double.parseDouble(data.getString("price")) * Double.parseDouble(data.getString("amount"));
            if(typeEquipment.equals("EQUIPEMENT")) {
                excel.insertWithStyle(6,currentI,priceAmount ,excel.tabStringStyleRight);
            }else{
                excel.insertWithStyle(7,currentI,priceAmount,excel.tabStringStyleRight);
            }
            excel.insertWithStyle(9,currentI,priceAmount + (priceAmount * Double.parseDouble(data.getString("tax_amount"))/100),excel.tabCurrencyStyle);
            oldUai = data.getString("uai");
            currentI ++;
        }

        //handle last struct
        fillYellowCells(currentI);
        excel.insertWithStyle(0,currentI ,"Total " + oldUai,excel.labelHeadStyle);
        inserTotal(initx,currentI);
        mergeStructures(currentI, initx);
        mergeFinalCells(currentI, initx);
        insertFinalTotal(currentI+2);
        excel.autoSize(20);
        insertHeader();

    }

    private void fillYellowCells(int currentI) {
        excel.insertWithStyle(1,currentI ,"",excel.yellowTab);
        excel.insertWithStyle(2,currentI ,"",excel.yellowTab);
        excel.insertWithStyle(3,currentI ,"",excel.yellowTab);
        excel.insertWithStyle(4,currentI ,"",excel.yellowTab);
    }

    private void mergeStructures(int currentI, int initx) {
        if(currentI-1 != initx) {
        sizeMergeRegionLinesWithStyle(1,initx,currentI - 1 ,excel.tabStringStyleCenterBold);
        sizeMergeRegionLinesWithStyle(2,initx,currentI - 1 ,excel.tabStringStyleCenterBold);
        sizeMergeRegionLinesWithStyle(3,initx,currentI - 1 ,excel.tabStringStyleCenterBold);
//        sizeMergeRegionLinesWithStyle(4,initx,currentI - 1 ,excel.tabStringStyleCenterBold);
        }
    }

    private void mergeFinalCells(int currentI, int initx) {
        excel.insertWithStyle(10,initx,"",excel.dateFormatStyle);
        sizeMergeRegionLinesWithStyle(10,initx,currentI,excel.dateFormatStyle);

        excel.insertWithStyle(11,initx,"",excel.dateFormatStyle);
        sizeMergeRegionLinesWithStyle(11,initx,currentI,excel.dateFormatStyle);//D

        excel.insertWithStyle(12,initx,"",excel.dateFormatStyle);
        sizeMergeRegionLinesWithStyle(12,initx,currentI,excel.dateFormatStyle);

        excel.insertWithStyle(13,initx,"",excel.standardTextStyle);
        sizeMergeRegionLinesWithStyle(13,initx,currentI,excel.standardTextStyle);//D
        excel.insertWithStyle(14,initx,"",excel.dateFormatStyle);

        sizeMergeRegionLinesWithStyle(14,initx,currentI,excel.dateFormatStyle);
        excel.insertWithStyle(15,initx,"",excel.currencyFormatStyle);

        sizeMergeRegionLinesWithStyle(15,initx,currentI,excel.currencyFormatStyle);
        excel.insertWithStyle(16,initx,"",excel.standardTextStyle);
        sizeMergeRegionLinesWithStyle(16,initx,currentI,excel.standardTextStyle);
        excel.insertWithStyle(17,initx,"",excel.numberFormatStyle);
        sizeMergeRegionLinesWithStyle(17,initx,currentI,excel.numberFormatStyle);
        String formula = "(" + excel.getCellReference(initx,17) + "*" + excel.getCellReference(currentI,8)+")/1000";
        excel.insertFormulaWithStyle(18, initx, formula,excel.currencyFormatStyle);
        sizeMergeRegionLines(18,initx,currentI);
    }

    private void insertStructureInfos(int currentI, JsonObject data) {
        excel.insertWithStyle(1,currentI,makeCellWithoutNull(data.getString("uai")),excel.tabStringStyleCenterBold);
        excel.insertWithStyle(2,currentI,makeCellWithoutNull(data.getString("nameEtab")),excel.tabStringStyleCenterBold);
        excel.insertWithStyle(3,currentI,makeCellWithoutNull(data.getString("city")),excel.tabStringStyleCenterBold);
    }

    private void insertHeader() {
        String title ="MARCHE N°" + datas.getJsonObject(0).getString("market_reference") +" - "+ datas.getJsonObject(0).getString("market_name") + ", \n";
        if(true == false)
            title+= "BON DE COMMANDE N°"
                    ;
        excel.insertWithStyle(2,0,title,excel.blackOnBlueHeader);
        sizeMergeRegionWithStyle(0,2,5,excel.blackOnBlueHeader);
    }


    private void insertFinalTotal(int line) {
        String formulaQty = "";
        String formulaEquipmentHT = "";
        String formulaPrestaHT = "";
        String formulaTotalHT = "";
        String formulaTotalTTC = "";



        excel.insertWithStyle(0,line ,"Total général  ",excel.labelHeadStyle);

        for(int i = 0; i< totalsXQty.size(); i++){
            int x = totalsXQty.get(i);
            String qtyRef =  excel.getCellReference(x,5);
            String priceEquipmentHTRef =  excel.getCellReference(x,6);
            String pricePrestaHTRef =  excel.getCellReference(x,7);
            String priceHTCellRef =  excel.getCellReference(x,8);
            String priceTTCCellRef =  excel.getCellReference(x,9);

            qtyRef = qtyRef.replace("'Liste Commandes avec Prix'!", "");
            priceEquipmentHTRef = priceEquipmentHTRef.replace("'Liste Commandes avec Prix'!", "");
            pricePrestaHTRef = pricePrestaHTRef.replace("'Liste Commandes avec Prix'!", "");
            priceHTCellRef = priceHTCellRef.replace("'Liste Commandes avec Prix'!", "");
            priceTTCCellRef = priceTTCCellRef.replace("'Liste Commandes avec Prix'!", "");

            if (formulaTotalTTC.length() + priceTTCCellRef.length() < LIMIT_FORMULA_SIZE) {
                formulaQty += qtyRef + "+";
                formulaPrestaHT += pricePrestaHTRef + "+";
                formulaEquipmentHT += priceEquipmentHTRef + "+";
                formulaTotalHT += priceHTCellRef + "+";
                formulaTotalTTC += priceTTCCellRef + "+";

            } else { //Substring
                formulaQty = formulaQty.substring(0, formulaQty.length() - 1);
                excel.insertFormula(1589 + i, 40, formulaQty);
                formulaQty = excel.getCellReference(40,1589 + i ) + " +" + qtyRef +"+";

                formulaEquipmentHT = formulaEquipmentHT.substring(0, formulaEquipmentHT.length() - 1);
                excel.insertFormula(1589 + i, 41, formulaEquipmentHT);
                formulaEquipmentHT = excel.getCellReference(41,1589 + i ) + " +" + priceEquipmentHTRef +"+";

                formulaPrestaHT = formulaPrestaHT.substring(0, formulaPrestaHT.length() - 1);
                excel.insertFormula(1589 + i, 42, formulaPrestaHT);
                formulaPrestaHT = excel.getCellReference(42,1589 + i ) + " +" + pricePrestaHTRef +"+";

                formulaTotalHT = formulaTotalHT.substring(0, formulaTotalHT.length() - 1);
                excel.insertFormula(1589 + i, 43, formulaTotalHT);
                formulaTotalHT = excel.getCellReference(43,1589 + i ) + " +" + priceHTCellRef +"+";

                formulaTotalTTC = formulaTotalTTC.substring(0, formulaTotalTTC.length() - 1);
                excel.insertFormula(1589 + i, 44, formulaTotalTTC);
                formulaTotalTTC = excel.getCellReference(44,1589 + i ) + " +" + priceTTCCellRef +"+";
            }
        }
        formulaQty = formulaQty.substring(0, formulaQty.length() - 1);
        formulaEquipmentHT = formulaEquipmentHT.substring(0, formulaEquipmentHT.length() - 1);
        formulaPrestaHT = formulaPrestaHT.substring(0, formulaPrestaHT.length() - 1);
        formulaTotalHT = formulaTotalHT.substring(0, formulaTotalHT.length() - 1);
        formulaTotalTTC = formulaTotalTTC.substring(0, formulaTotalTTC.length() - 1);

        insertPircesAndFormula(line, formulaQty, formulaEquipmentHT, formulaPrestaHT, formulaTotalHT, formulaTotalTTC);

    }

    private void insertPircesAndFormula(int line, String formulaQty, String formulaEquipmentHT, String formulaPrestaHT, String formulaTotalHT, String formulaTotalTTC) {
        excel.insertFormulaWithStyle(5, line, formulaQty,excel.labelHeadStyle);
        excel.insertFormula(6, line, formulaEquipmentHT);
        excel.insertFormula(7, line, formulaPrestaHT);
        excel.insertFormula(8, line, formulaTotalHT);
        excel.insertFormula(9, line, formulaTotalTTC);
    }

    private int inserTotal(int initx, int currentI) {
        //faire le TTC
        excel.fillTab(5,9, initx,currentI );
        excel.setTotalXWithStyle(initx,currentI - 1,5,currentI,excel.yellowTab);
        excel.setTotalWithStyle(initx, currentI , 6, 8,currentI,8,excel.tabCurrencyStyle,excel.yellowTabPrice,excel.yellowTabPrice);
        excel.setTotalXWithStyle(initx,currentI-1,9,currentI,excel.yellowTabPrice);
        totalsXQty.add(currentI);
        return currentI + 1;
    }


    private void setTitle() {
        excel.insertWithStyle(1,1,"UAI        ",excel.yellowLabel);
        excel.insertWithStyle(2,1,"Nom de l'établissement",excel.yellowLabel);
        excel.insertWithStyle(3,1,"Commune",excel.yellowLabel);
        excel.insertWithStyle(4,1,"Equipement",excel.yellowLabel);
        excel.insertWithStyle(5,1,"Qté",excel.yellowLabel);
        // PRIX TOTAL EQUIPEMENT1T 	 PRIX TOTAL PRESTATION HT 	 PRIX TOTAL HT 	 PRIX TOTAL TTC 	DATE ARC	DATE LIMITE -LIVRAISON	DATE CSF	FACTURE	DATE FACTURE	FACTURE / RECAP	MONTANT FACTURE
        excel.insertWithStyle(6,1,"PRIX TOTAL FOURNITURE HT",excel.yellowLabel);
        excel.insertWithStyle(7,1,"PRIX TOTAL MISE EN SERVICE HT",excel.yellowLabel);
        excel.insertWithStyle(8,1,"PRIX TOTAL HT ",excel.yellowLabel);
        excel.insertWithStyle(9,1,"PRIX TOTAL TTC",excel.yellowLabel);
        excel.insertWithStyle(10,1,"DATE ARC",excel.yellowLabel);
        excel.insertWithStyle(11,1,"DATE LIMITE -LIVRAISON",excel.yellowLabel);
        excel.insertWithStyle(12,1,"DATE CSF",excel.yellowLabel);
        excel.insertWithStyle(13,1,"NUMERO FACTURE ",excel.yellowLabel);
        excel.insertWithStyle(14,1,"DATE FACTURE",excel.yellowLabel);
        excel.insertWithStyle(15,1,"MONTANT FACTURE",excel.yellowLabel);
        excel.insertWithStyle(16,1,"FACTURE / RECAP ",excel.yellowLabel);
        excel.insertWithStyle(17,1,"RETARD DE LIVRAISON ",excel.yellowLabel);
        excel.insertWithStyle(18,1,"PENALITE DE RETARD ",excel.yellowLabel);
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT oce.price,  " +
                "       oce.tax_amount,  " +
                "       oce.NAME,  " +
                "       oce.id_contract,  " +
                "       Sum(oce.amount) AS amount,  " +
                "       oce.id_structure,  " +
                "       et.NAME  as typeequipment," +
                "       market.name as market_name," +
                "       market.reference as market_reference " +
                "       FROM   "+ Lystore.lystoreSchema +".order_client_equipment oce  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "               ON oce.id_type = et.id  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".contract market  " +
                "               ON oce.id_contract = market.id  " +
                "WHERE  number_validation = ?  " +
                "GROUP  BY oce.equipment_key,  " +
                "          oce.price,  " +
                "          oce.tax_amount,  " +
                "          oce.NAME,  " +
                "          oce.id_contract,  " +
                "          oce.id_structure,  " +
                "          et.NAME ," +
                "          market_name," +
                "          market_reference " +
                "UNION  " +
                "SELECT opt.price,  " +
                "       opt.tax_amount,  " +
                "       opt.NAME,  " +
                "       opt.id_contract,  " +
                "       Sum(opt.amount) AS amount,  " +
                "       opt.id_structure,  " +
                "       opt.typeequipment," +
                "       opt.market_name," +
                "       opt.market_reference      " +
                "FROM   (SELECT options.price,  " +
                "               options.tax_amount,  " +
                "               options.NAME,  " +
                "               oce.id_contract,  " +
                "               oce.amount,  " +
                "               options.id_order_client_equipment,  " +
                "               oce.id_structure,  " +
                "               market.name as market_name," +
                "               market.reference as market_reference, " +
                "               et.NAME AS typeequipment  " +
                "        FROM   "+ Lystore.lystoreSchema +".order_client_options options  " +
                "               INNER JOIN "+ Lystore.lystoreSchema +".order_client_equipment oce  " +
                "                       ON ( options.id_order_client_equipment = oce.id )  " +
                "               INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "                       ON options.id_type = et.id " +
                "               INNER JOIN " + Lystore.lystoreSchema + ".contract market " +
                "                       ON oce.id_contract = market.id "  +
                "        WHERE  number_validation = ? ) AS opt  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".order_client_equipment equipment  " +
                "               ON ( opt.id_order_client_equipment = equipment.id )  " +
                "GROUP  BY opt.NAME,  " +
                "          opt.price,  " +
                "          opt.tax_amount,  " +
                "          opt.id_contract,  " +
                "          opt.id_structure,  " +
                "          opt.typeequipment ," +
                "           opt.market_name," +
                "           market_reference";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
    }
}

