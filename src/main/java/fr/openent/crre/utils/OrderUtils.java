package fr.openent.crre.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DecimalFormat;

public class OrderUtils {

    public static JsonObject getPriceTtc(JsonObject equipmentJson) {
        Double prixht, price_TTC;
        JsonArray tvas;
        JsonObject result = new JsonObject();
        DecimalFormat df2 = new DecimalFormat("#.##");

        if(equipmentJson.getString("type").equals("articlenumerique")){
            prixht = equipmentJson.getJsonArray("offres").getJsonObject(0).getDouble("prixht");
            tvas = equipmentJson.getJsonArray("offres").getJsonObject(0).getJsonArray("tvas");
        }else{
            prixht = equipmentJson.getDouble("prixht");
            tvas = equipmentJson.getJsonArray("tvas");
        }
        price_TTC = prixht;
        for(Object tva : tvas){
            JsonObject tvaJson = (JsonObject) tva;
            Double taxFloat = tvaJson.getDouble("taux");
            Double pourcent = tvaJson.getDouble("pourcent");
            Double priceToAdd  = (((prixht)*pourcent/100) *  taxFloat) / 100;
            price_TTC += priceToAdd;
            if(taxFloat.equals(5.5)){
                result.put("partTVA5",Double.parseDouble(df2.format(priceToAdd)));
            }else if(taxFloat.equals(20.0)) {
                result.put("partTVA20", Double.parseDouble(df2.format(priceToAdd)));
            }
        }
        result.put("prixht",prixht).put("priceTTC",Double.parseDouble(df2.format(price_TTC)));
        return result;
    }

    public static void dealWithPriceTTC_HT(JsonObject order) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        Double price = Double.parseDouble(order.getString("equipment_price"));
        Double priceht = order.getDouble("equipment_priceht");
        double priceTTC = price * order.getInteger("amount");
        double priceHT = priceht * order.getInteger("amount");
        order.put("priceht", priceht);
        order.put("tva5", order.getDouble("equipment_tva5"));
        order.put("tva20", order.getDouble("equipment_tva20"));
        order.put("unitedPriceTTC", price);
        order.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
        order.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
    }

    public static void extractedEquipmentInfo(JsonObject order) {
        order.put("name", order.getString("titre"));
        order.put("image", order.getString("urlcouverture"));
        order.put("ean", order.getString("ean"));
        order.put("editor", order.getString("editeur"));
        order.put("diffusor", order.getString("distributeur"));
        order.put("type", order.getString("type"));
    }

    public static String convertPriceString(double price) {
        return (price == 0) ? "GRATUIT" : Double.toString(price);
    }

}
