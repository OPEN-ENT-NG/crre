package fr.openent.crre.utils;

import fr.openent.crre.core.constants.Field;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class OrderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderUtils.class);

    private OrderUtils() {
        throw new IllegalAccessError("OrderUtils Utility class");
    }

    public static JsonObject getPriceTtc(JsonObject equipmentJson) {
        JsonObject result = new JsonObject();
        try {
            Double prixht = 0.0, price_TTC;
            JsonArray tvas = new JsonArray();
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
            DecimalFormat df2 = new DecimalFormat("#.##", dfs);

            if (equipmentJson.containsKey("type") && equipmentJson.getString("type", "").equals("articlenumerique") &&
                    equipmentJson.getJsonArray("offres") != null && equipmentJson.getJsonArray("offres").size() > 0 &&
                    equipmentJson.getJsonArray("offres").getJsonObject(0).getDouble("prixht") != null &&
                    equipmentJson.getJsonArray("offres").getJsonObject(0).getJsonArray("tvas") != null) {
                prixht = equipmentJson.getJsonArray("offres").getJsonObject(0).getDouble("prixht",0.0);
                tvas = equipmentJson.getJsonArray("offres").getJsonObject(0).getJsonArray("tvas", new JsonArray());
            } else if (equipmentJson.getJsonArray("tvas") != null && equipmentJson.getDouble("prixht") != null){
                prixht = equipmentJson.getDouble("prixht",0.0);
                tvas = equipmentJson.getJsonArray("tvas", new JsonArray());
            }
            price_TTC = prixht;
            for (Object tva : tvas) {
                JsonObject tvaJson = (JsonObject) tva;
                Double taxFloat = tvaJson.containsKey("taux") ? tvaJson.getDouble("taux", 0.0) : 0.0;
                Double pourcent = tvaJson.containsKey("pourcent") ? tvaJson.getDouble("pourcent",0.0) : 0.0;
                Double priceToAdd = (((prixht) * pourcent / 100) * taxFloat) / 100;
                price_TTC += priceToAdd;
                if (taxFloat.equals(5.5)) {
                    result.put("partTVA5", Double.parseDouble(df2.format(priceToAdd)));
                } else if (taxFloat.equals(20.0)) {
                    result.put("partTVA20", Double.parseDouble(df2.format(priceToAdd)));
                }
            }
            result.put("prixht", prixht).put("priceTTC", Double.parseDouble(df2.format(price_TTC)));
            return result;
        } catch (Exception e) {
            LOGGER.error("An error occurred getPriceTtc in Utils : " + e);
            e.printStackTrace();
            return result;
        }
    }

    public static void dealWithPriceTTC_HT(JsonObject orderMap, JsonObject equipment, JsonObject order, boolean old) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat df2 = new DecimalFormat("#.##", dfs);
        double priceht, price;

        if (!old) {
            JsonObject priceInfo = getPriceTtc(equipment);
            price = priceInfo.containsKey("priceTTC") ? priceInfo.getDouble("priceTTC",0.0) : 0.0;
            priceht = priceInfo.containsKey("prixht") ? priceInfo.getDouble("prixht",0.0) : 0.0;

            orderMap.put("tva5", priceInfo.getDouble("partTVA5") != null ?
                    Double.parseDouble(df2.format(priceInfo.getDouble("partTVA5"))) : null);
            orderMap.put("tva20",  priceInfo.getDouble("partTVA20") != null ?
                    Double.parseDouble(df2.format(priceInfo.getDouble("partTVA20"))) : null);
        } else {
            price = Double.parseDouble(order.containsKey("equipment_price") ? order.getString("equipment_price","0.0") : "0.0");
            priceht = order.containsKey("equipment_priceht") ? order.getDouble("equipment_priceht",0.0) : 0.0;

            orderMap.put("tva5", order.getDouble("equipment_tva5") != null ?
                    Double.parseDouble(df2.format(order.getDouble("equipment_tva5"))) : null);
            orderMap.put("tva20",  order.getDouble("equipment_tva20") != null ?
                    Double.parseDouble(df2.format(order.getDouble("equipment_tva20"))) : null);
        }

        double priceHT = priceht * (orderMap.containsKey("amount") ? orderMap.getInteger("amount",0) : 0);
        double priceTTC = price * (orderMap.containsKey("amount") ? orderMap.getInteger("amount",0) : 0);

        orderMap.put("priceht", priceht);
        orderMap.put("unitedPriceTTC", Double.parseDouble(df2.format(price)));
        orderMap.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
        orderMap.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
    }

    public static void extractedEquipmentInfo(JsonObject order, JsonObject equipment) {
        order.put(Field.NAME, equipment.containsKey("titre") ? equipment.getString("titre","") : "");
        order.put("image", equipment.containsKey("urlcouverture") ? equipment.getString("urlcouverture","") : "");
        order.put("ean", equipment.containsKey("ean") ? equipment.getString("ean","") : "");
        order.put("editor", equipment.containsKey("editeur") ? equipment.getString("editeur","") : "");
        order.put("diffusor", equipment.containsKey("distributeur") ? equipment.getString("distributeur","") : "");
        order.put("type", equipment.containsKey("type") ? equipment.getString("type","") : "");
    }

    public static String convertPriceString(double price) {
        return (price == 0) ? "GRATUIT" : Double.toString(price);
    }

    public static Integer formatPage(HttpServerRequest request) {
        Integer page = 0;
        if (request.getParam("page") == null) {
            page = 0;
        } else if (request.getParam("page").equals("null")) {
            page = null;
        } else if (request.getParam("page") != null) {
            page = Integer.parseInt(request.getParam("page"));
        }
        return page;
    }

}
