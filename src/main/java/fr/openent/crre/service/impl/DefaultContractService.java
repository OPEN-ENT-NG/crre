package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.ContractService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultContractService extends SqlCrudService implements ContractService {

    public DefaultContractService(String schema, String table) {
        super(schema, table);
    }

    public void getContracts(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT contract.id as id, contract.name as name, annual_min, annual_max, " +
                "start_date, nb_renewal, " +
                "id_contract_type, max_brink, id_supplier, id_agent, reference, renewal_end, end_date, " +
                "supplier.name as supplier_display_name, contract.file as file " +
                "FROM " + Crre.crreSchema + ".contract INNER JOIN " + Crre.crreSchema +
                ".supplier on (contract.id_supplier = supplier.id) " +
                "ORDER BY contract.name ASC";

        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void createContract(JsonObject contract, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".contract(name, annual_min, " +
                "annual_max, start_date, nb_renewal, id_contract_type, max_brink, id_supplier, id_agent, " +
                "reference, end_date, renewal_end, file) " +
                "VALUES (?, ?, ?, to_date(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, to_date(?, 'YYYY-MM-DD')," +
                " to_date(?, 'YYYY-MM-DD'), ?) " +
                "RETURNING id;";
        JsonArray params = new JsonArray();
        addValues(contract, params);
        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void addValues(JsonObject contract, JsonArray params) {
        params.add(contract.getString("name"))
                .add(contract.getDouble("annual_min"))
                .add(contract.getDouble("annual_max"))
                .add(contract.getString("start_date"))
                .add(contract.getInteger("nb_renewal"))
                .add(contract.getInteger("id_contract_type"))
                .add(contract.getDouble("max_brink"))
                .add(contract.getInteger("id_supplier"))
                .add(contract.getInteger("id_agent"))
                .add(contract.getString("reference"))
                .add(contract.getString("end_date"))
                .add(contract.getString("renewal_end"))
                .add(contract.getBoolean("file"));
    }

    public void updateContract(JsonObject contract, Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".contract " +
                "SET name = ?, annual_min = ?, annual_max = ?, start_date = to_date(?, 'YYYY-MM-DD'), nb_renewal = ?," +
                "id_contract_type = ?, max_brink = ?, id_supplier = ?, id_agent = ?, " +
                "reference = ?, end_date = to_date(?, 'YYYY-MM-DD'), renewal_end = to_date(?, 'YYYY-MM-DD'), file = ? " +
                "WHERE id = ?;";
        JsonArray params = new JsonArray();
        addValues(contract, params);
        params.add(id);

        this.sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public void deleteContract(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds(this.table, ids, handler);
    }


    public  void getContractId(Integer id, Handler<Either<String, JsonArray>> handler){
        String query = " Select contract.* "+
                "From " + Crre.crreSchema + ".contract"+
                "Where contract.id = ?";
        JsonArray params= new JsonArray().add(id);

        this.sql.prepared(query,params,SqlResult.validResultHandler(handler));

    }

    @Override
    public void getContract(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT contract.* " +
                "FROM " + Crre.crreSchema + ".order_client_equipment " +
                "INNER JOIN " + Crre.crreSchema + ".contract " +
                "ON (order_client_equipment.id_contract = contract.id) " +
                "WHERE order_client_equipment.number_validation IN " + Sql.listPrepared(ids.getList()) +
                " GROUP BY contract.id " +
                " LIMIT 1";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < ids.size(); i++) {
            params.add(ids.getString(i));
        }

        this.sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
