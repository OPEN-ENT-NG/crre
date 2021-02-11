package fr.openent.crre.service.impl;

import fr.openent.crre.service.EquipmentService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;

import java.util.ArrayList;
import java.util.HashMap;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;


public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    public DefaultEquipmentService(String schema, String table) {
        super(schema, table);
    }

    public void searchWord(String word, Handler<Either<String, JsonArray>> handler) {
        plainTextSearch(word, handler);
    }

    public void searchAll(Handler<Either<String, JsonArray>> handler) {
        search_All(handler);
    }

    public void searchFilter(HashMap<String, ArrayList<String>> result, String query, Handler<Either<String, JsonArray>> handler) {
        searchfilter(result, query, handler);
    }

    public void filterWord(HashMap<String, ArrayList<String>> test, Handler<Either<String, JsonArray>> handler) {
        filter(test, handler);
    }

    public void equipment(String idEquipment,  Handler<Either<String, JsonArray>> handler){
        searchById(idEquipment, handler);
    }
}

