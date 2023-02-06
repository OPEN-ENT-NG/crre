package fr.openent.crre.helpers;

import fr.openent.crre.model.BasketOrderItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EquipmentHelper {
    public static JsonObject getDefaultEquipment(BasketOrderItem basketOrderItem) {
        return new JsonObject()
                .put("urlcouverture", "/crre/public/img/pages-default.png")
                .put("disponibilite", new JsonArray().add(new JsonObject().put("valeur", "Non disponible à long terme")))
                .put("titre", "Manuel introuvable dans le catalogue")
                .put("ean", basketOrderItem.getIdItem())
                .put("inCatalog", false)
                .put("price", 0.0);
    }
}
