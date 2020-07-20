package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.TagService;
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
                "FROM " + Crre.crreSchema + ".tag " +
                "INNER JOIN " + Crre.crreSchema + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id " +
                "UNION " +
                "SELECT id, name, color, count(rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Crre.crreSchema + ".tag " +
                "LEFT JOIN " + Crre.crreSchema + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id;";

        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void create(JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Crre.crreSchema + ".tag(name, color) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(tag.getString("name"))
                .add(tag.getString("color"));

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void update(Integer id, JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Crre.crreSchema + ".tag " +
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
