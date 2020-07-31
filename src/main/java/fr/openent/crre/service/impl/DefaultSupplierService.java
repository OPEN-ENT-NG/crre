package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.SupplierService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class DefaultSupplierService extends SqlCrudService implements SupplierService {

    public DefaultSupplierService(String schema, String table) {
        super(schema, table);
    }

    public void getSuppliers(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    public void createSupplier(JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".supplier (email, address, name, phone) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";
        JsonArray params = addParamsSupplier(agent);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonArray addParamsSupplier(JsonObject person) {
        return new fr.wseduc.webutils.collections.JsonArray()
                .add(person.getString("email"))
                .add(person.getString("address"))
                .add(person.getString("name"))
                .add(person.getString("phone"));
    }

    public void updateSupplier(Integer id, JsonObject supplier, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".supplier " +
                "SET email = ?, address = ?, name = ?, phone = ? " +
                "WHERE id = ? RETURNING *;";
        JsonArray params = addParamsSupplier(supplier);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void deleteSupplier(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds("supplier", ids, handler);
    }

    @Override
    public void getSupplier(String id, Handler<Either<String, JsonObject>> handler) {
        super.retrieve(id, handler);
    }

    @Override
    public void getSupplierByValidationNumbers(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT distinct supplier.id " +
                "FROM crre.order_client_equipment " +
                "INNER JOIN crre.contract ON (order_client_equipment.id_contract = contract.id) " +
                "INNER JOIN crre.supplier ON (contract.id_supplier = supplier.id) " +
                "WHERE order_client_equipment.number_validation IN " + Sql.listPrepared(validationNumbers.getList())+
                " LIMIT 1";

        this.sql.prepared(query, validationNumbers, SqlResult.validUniqueResultHandler(handler, new String[0]));
    }
}
