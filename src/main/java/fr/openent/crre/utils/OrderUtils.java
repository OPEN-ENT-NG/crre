package fr.openent.crre.utils;

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
    public static final DecimalFormat df2 = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

    private OrderUtils() {
        throw new IllegalAccessError("OrderUtils Utility class");
    }

    public static JsonObject getPriceTtc(JsonObject equipmentJson) {
        JsonObject result = new JsonObject();
        try {
            Double prixht = 0.0, price_TTC;
            JsonArray tvas = new JsonArray();

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
            result.put("prixht", Double.parseDouble(df2.format(prixht))).put("priceTTC", Double.parseDouble(df2.format(price_TTC)));
            return result;
        } catch (Exception e) {
            LOGGER.error("An error occurred getPriceTtc in Utils : " + e);
            e.printStackTrace();
            return result;
        }
    }

    public static String convertPriceString(double price) {
        return (price == 0) ? "GRATUIT" : Double.toString(price);
    }

}
