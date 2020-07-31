package fr.openent.crre.export.validOrders.listLycee;

import fr.openent.crre.Crre;
import fr.openent.crre.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.apache.poi.ss.usermodel.Workbook;

public class ListLycee extends TabHelper {
    private final String numberValidation;
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
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount , id_structure  " +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE number_validation = ? ";
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract,  id_structure " +
                "UNION " +
                "SELECT opt.price, opt.tax_amount, opt.name, opt.id_contract, SUM(opt.amount) as amount " +
                ", equipment.id_structure " +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment , equipment.id_structure "+
                "FROM " + Crre.crreSchema + ".order_client_options options " +
                "INNER JOIN " + Crre.crreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE number_validation = ? "+
                ") as opt";
        query += " INNER JOIN " + Crre.crreSchema + ".order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" ;
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract , equipment.id_structure";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
        }
}
