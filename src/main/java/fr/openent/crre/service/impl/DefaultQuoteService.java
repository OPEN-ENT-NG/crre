package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.QuoteService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DefaultQuoteService extends SqlCrudService implements QuoteService {

    private final Integer PAGE_SIZE = 15;

    public DefaultQuoteService(String table) {
        super(table);
    }

    @Override
    public void getAllQuote(Integer page, Handler<Either<String, JsonArray>> defaultResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "" +
                "SELECT id, title, owner_name, owner_id, nb_structures, quotation, " +
                "creation_date at time zone 'europe/paris' as creation_date " +
                "FROM " + Crre.crreSchema + ".quote AS q ";
        query = query + "ORDER BY q.creation_date DESC ";
        if (page != null) {
            query += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(query, values, SqlResult.validResultHandler(defaultResponseHandler));
    }

    @Override
    public void getQuote(Integer id, Handler<Either<String, JsonObject>> defaultResponseHandler) {
        String query = "SELECT * FROM " + Crre.crreSchema + ".quote WHERE id = ?;";
        sql.prepared(query, new JsonArray().add(id), SqlResult.validUniqueResultHandler(defaultResponseHandler));
    }

    @Override
    public void insertQuote(UserInfos user, Integer nbEtab, String csvFile, String title, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "WITH next_id AS (SELECT nextval('" + Crre.crreSchema + ".quote_id_seq')) " +
                "INSERT INTO " + Crre.crreSchema + ".quote(id, title, owner_name, owner_id, nb_structures, attachment) " +
                "VALUES ((SELECT nextval FROM next_id), CONCAT('" + title + "-', (SELECT nextval FROM next_id)), ?, ?, ?, ?) " +
                "RETURNING title;";
        params.add(user.getUsername())
              .add(user.getUserId())
              .add(nbEtab)
              .add(csvFile);
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void search(String query, Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String sqlquery = "SELECT id, title, owner_name, owner_id, nb_structures, quotation, " +
                "creation_date at time zone 'europe/paris' as creation_date " +
                "FROM  " + Crre.crreSchema + ".quote q " +
                "WHERE lower(q.title) ~* ? OR lower(q.owner_name) ~* ? ";

        values.add(query);
        values.add(query);
        sqlquery = sqlquery + "ORDER BY q.creation_date DESC ";
        if (page != null) {
            sqlquery += "OFFSET ? LIMIT ? ";
            values.add(PAGE_SIZE * page);
            values.add(PAGE_SIZE);
        }
        sql.prepared(sqlquery, values, SqlResult.validResultHandler(arrayResponseHandler));
    }
}

