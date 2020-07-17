package fr.openent.lystore.utils;

import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class SqlQueryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlQueryUtils.class);

    private SqlQueryUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static StringBuilder prepareMultipleIds (List<Integer> ids) {
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                filter.append("OR ");
            }
            filter.append("id = ? ");
        }

        return filter;
    }

    public static List<Integer> getIntegerIds (List<String> params) {
        List<Integer> ids = new ArrayList<>();
        for (String param : params) {
            ids.add(Integer.parseInt(param));
        }
        return ids;
    }

    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    public static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("id", id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching transaction");
            either = new Either.Left<>("");
        }
        return either;
    }
    /**
     * Returns a array multiply by numberDuplicate on itself
     *
     * @param numberDuplicate Integer
     * @param arrayDuplicate JsonArray
     * @return result JsonArray ex: in (2, [1,2,3]), out [1,2,3,1,2,3]
     */
    public static JsonArray multiplyArray (Integer numberDuplicate, JsonArray arrayDuplicate){
        JsonArray result = new JsonArray();
        for(int i = 0; i < numberDuplicate; i++) {
            for(int j = 0;j<arrayDuplicate.size();j++) {
                result.add(arrayDuplicate.getInteger(j));
            }
        }
        return result;
    }
    /**
     * Returns a array to object with join data by a id between two arrays and add the name join
     *
     * @param dataLeftJoin JsonArray
     * @param dataRightJoin JsonArray
     * @param nameJoin String
     * @return result JsonArray
     */
    public static JsonArray addDataByIdJoin (JsonArray dataLeftJoin, JsonArray dataRightJoin, String nameJoin){
        JsonArray result = new JsonArray();
        for (int i = 0; i < dataLeftJoin.size(); i++) {
            JsonObject elementLeft = dataLeftJoin.getJsonObject(i);
            for (int j = 0; j < dataRightJoin.size(); j++){
                JsonObject elementRight = dataRightJoin.getJsonObject(j);
                if(elementLeft.getInteger("id").equals(elementRight.getInteger("id"))){
                    elementLeft.put(nameJoin, elementRight.getString(nameJoin));
                }
            }
            result.add(elementLeft);
        }
        return result;
    }
    /**
     * Returns an array id to jsonArray object
     *
     * @param resultRequest JsonArray
     * @return result JsonArray ex '[1,2,3,4,5,6]'
     */
    public static JsonArray getArrayAllIdsResult (JsonArray resultRequest){
        JsonArray result = new JsonArray();
        for (int i = 0; i < resultRequest.size(); i++) {
            JsonObject operation = resultRequest.getJsonObject(i);
            result.add(operation.getInteger("id"));
        }
        return result;
    }

    /**
     * Returns an array merge
     *
     * @param arrayList multi JsonArrays
     * @return result JsonArray ex '[1,2,3] and [4,5,6] and more to [1,2,3,4,5,6]'
     */
    public static JsonArray mergeArraysInOne (JsonArray... arrayList){
        JsonArray result = new JsonArray();
        for(JsonArray arrayIn : arrayList){
            for (int i = 0; i < arrayIn.size(); i++) {
                if(arrayIn.getValue(i) != null) {
                    result.add(arrayIn.getValue(i));
                }
            }
        }
        return result;
    }
}
