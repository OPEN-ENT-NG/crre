package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface EquipmentService {

    /**
     * Get an Equipment information's
     * @param idEquipment equipment identifier
     * @param handler function handler returning data
     */
    void equipment(String idEquipment, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler);

    void searchWord(String word, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler);

    void filterWord(HashMap<String, ArrayList<String>> test, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler);

    void searchAll(Handler<Either<String, JsonArray>> handler);

    void searchFilter(HashMap<String, ArrayList<String>> result, String query, List<String> resultFieldsExpected, Handler<Either<String, JsonArray>> handler);
}
