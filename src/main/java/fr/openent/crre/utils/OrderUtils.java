package fr.openent.crre.utils;

import fr.wseduc.webutils.I18n;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static fr.wseduc.webutils.http.Renders.getHost;

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

    public static String getValidOrdersCSVExportHeader(HttpServerRequest request) {
        return I18n.getInstance().
                translate("UAI", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("crre.structure.name", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("city", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("phone", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("EQUIPMENT", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("crre.amount", getHost(request), I18n.acceptLanguage(request)) +
                "\n";
    }

    public static Map<String, String> retrieveUaiNameStructure(JsonArray structures) {
        final Map<String, String> structureMap = new HashMap<>();
        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.getJsonObject(i);
            String uaiNameStructure = structure.getString("uai") + " - " + structure.getString("name");
            structureMap.put(structure.getString("id"), uaiNameStructure);
        }

        return structureMap;
    }

    public static void putInfosInData(String nbrBc, String nbrEngagement, JsonObject data, JsonObject managmentInfo, JsonObject order, JsonArray certificates, JsonObject contract) {
        JsonObject certificate;
        for (int i = 0; i < certificates.size(); i++) {
            certificate = certificates.getJsonObject(i);
            certificate.put("agent", managmentInfo.getJsonObject("userInfo"));
            certificate.put("supplier",
                    managmentInfo.getJsonObject("supplierInfo"));
            addStructureToOrders(certificate.getJsonArray("orders"),
                    certificate.getJsonObject("structure"));
        }
        data.put("supplier", managmentInfo.getJsonObject("supplierInfo"))
                .put("agent", managmentInfo.getJsonObject("userInfo"))
                .put("order", order)
                .put("certificates", certificates)
                .put("contract", contract)
                .put("nbr_bc", nbrBc)
                .put("nbr_engagement", nbrEngagement);
    }

    private static void addStructureToOrders(JsonArray orders, JsonObject structure) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("structure", structure);
        }
    }
}
