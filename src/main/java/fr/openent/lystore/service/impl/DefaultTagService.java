package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.TagService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class DefaultTagService extends SqlCrudService implements TagService {

    public DefaultTagService(String schema, String table) {
        super(schema, table);
    }

    public void getAll(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, color, count(rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.lystoreSchema + ".tag " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id " +
                "UNION " +
                "SELECT id, name, color, count(rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.lystoreSchema + ".tag " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id;";

        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void create(JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".tag(name, color) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(tag.getString("name"))
                .add(tag.getString("color"));

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void update(Integer id, JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".tag " +
                "SET name = ?, color = ? " +
                "WHERE id = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(tag.getString("name"))
                .add(tag.getString("color"))
                .add(id);

        this.sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds("tag", ids, handler);
    }
}
