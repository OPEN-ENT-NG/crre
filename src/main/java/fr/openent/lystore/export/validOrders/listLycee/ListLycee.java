package fr.openent.lystore.export.validOrders.listLycee;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class ListLycee extends TabHelper {
    private String numberValidation;
    public ListLycee(Workbook workbook, String numberValidation) {
        super(workbook,"Sommaire Commande");
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
//
                boolean errorCatch= false;
                if (repStructures.isRight()) {
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
                    }
                    if(errorCatch)
                        handler.handle(new Either.Left<>("Error when writting files"));
                    else
                        handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));
//

                }
            }
//
//
        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
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

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount , id_structure  " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE number_validation = ? ";
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract,  id_structure " +
                "UNION " +
                "SELECT opt.price, opt.tax_amount, opt.name, opt.id_contract, SUM(opt.amount) as amount " +
                ", equipment.id_structure " +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment , equipment.id_structure "+
                "FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE number_validation = ? "+
                ") as opt";
        query += " INNER JOIN lystore.order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" ;
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract , equipment.id_structure";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
        }
}
