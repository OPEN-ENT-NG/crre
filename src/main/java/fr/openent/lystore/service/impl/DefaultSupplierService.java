package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.SupplierService;
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
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".supplier (email, address, name, phone) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(agent.getString("email"))
                .add(agent.getString("address"))
                .add(agent.getString("name"))
                .add(agent.getString("phone"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateSupplier(Integer id, JsonObject supplier, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".supplier " +
                "SET email = ?, address = ?, name = ?, phone = ? " +
                "WHERE id = ? RETURNING *;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(supplier.getString("email"))
                .add(supplier.getString("address"))
                .add(supplier.getString("name"))
                .add(supplier.getString("phone"))
                .add(id);

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
                "FROM lystore.order_client_equipment " +
                "INNER JOIN lystore.contract ON (order_client_equipment.id_contract = contract.id) " +
                "INNER JOIN lystore.supplier ON (contract.id_supplier = supplier.id) " +
                "WHERE order_client_equipment.number_validation IN " + Sql.listPrepared(validationNumbers.getList())+
                " LIMIT 1";

        this.sql.prepared(query, validationNumbers, SqlResult.validUniqueResultHandler(handler, new String[0]));
    }
}
