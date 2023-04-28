package fr.openent.crre.service.impl;

import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.service.EquipmentService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.openent.crre.helpers.ElasticSearchHelper.*;


public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    public DefaultEquipmentService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void searchWord(String word, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {
        plainTextSearch(word, resultFieldsExpected, handler);
    }

    @Override
    public void searchAll(Handler<Either<String, JsonArray>> handler) {
        search_All(handler);
    }

    @Override
    public Future<JsonArray> searchFilter(FilterItemModel filterItemModel, List<String> resultFieldsExpected) {
        return searchfilter(filterItemModel, resultFieldsExpected);
    }

    @Override
    public void filterWord(FilterItemModel filterItemModel, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler) {
        filters(filterItemModel, resultFieldsExpected, handler);
    }

    @Override
    public void equipment(String idEquipment, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler){
        searchById(idEquipment, resultFieldsExpected, handler);
    }
}

