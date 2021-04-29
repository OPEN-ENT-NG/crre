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

    public static void dealWithPriceTTC_HT(JsonObject orderMap, JsonObject equipment, JsonObject order, boolean old) {
        DecimalFormat df2 = new DecimalFormat("#.##");
        if(!old) {
            JsonObject priceInfo = getPriceTtc(equipment);
            Double priceht = priceInfo.getDouble("prixht");
            Double price = priceInfo.getDouble("priceTTC");
            double priceHT = priceht * orderMap.getInteger("amount");
            double priceTTC = price * orderMap.getInteger("amount");
            orderMap.put("priceht", priceht);
            orderMap.put("tva5", priceInfo.getDouble("partTVA5"));
            orderMap.put("tva20", priceInfo.getDouble("partTVA20"));
            orderMap.put("unitedPriceTTC", price);
            orderMap.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
            orderMap.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
        } else {
            Double price = Double.parseDouble(order.getString("equipment_price"));
            Double priceht = order.getDouble("equipment_priceht");
            double priceTTC = price * orderMap.getInteger("amount");
            double priceHT = priceht * orderMap.getInteger("amount");
            orderMap.put("priceht", priceht);
            orderMap.put("tva5", order.getDouble("equipment_tva5"));
            orderMap.put("tva20", order.getDouble("equipment_tva20"));
            orderMap.put("unitedPriceTTC", price);
            orderMap.put("totalPriceHT", Double.parseDouble(df2.format(priceHT)));
            orderMap.put("totalPriceTTC", Double.parseDouble(df2.format(priceTTC)));
        }
    }

    public static void extractedEquipmentInfo(JsonObject order, JsonObject equipment) {
        order.put("name", equipment.getString("titre"));
        order.put("image", equipment.getString("urlcouverture"));
        order.put("ean", equipment.getString("ean"));
        order.put("editor", equipment.getString("editeur"));
        order.put("diffusor", equipment.getString("distributeur"));
        order.put("type", equipment.getString("type"));
    }

    public static String convertPriceString(double price) {
        return (price == 0) ? "GRATUIT" : Double.toString(price);
    }

}
