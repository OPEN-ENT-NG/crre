package fr.openent.crre.helpers;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.BasketOrderItem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class EquipmentHelper {
    public static final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
    public static final DecimalFormat df2 = new DecimalFormat("#.##", dfs);
    public static JsonObject getDefaultEquipment(BasketOrderItem basketOrderItem) {
        return new JsonObject()
                .put(Field.URLCOUVERTURE, "/crre/public/img/pages-default.png")
                .put(Field.DISPONIBILITE, new JsonArray().add(new JsonObject().put(Field.VALEUR, "Non disponible à long terme")))
                .put(Field.TITRE, "Manuel introuvable dans le catalogue")
                .put(Field.EAN, basketOrderItem.getIdItem())
                .put(Field.INCATALOG, false)
                .put(Field.PRICE, 0.0)
                .put(Field.VALID, false);
    }

    public static Double getRoundedPrice(Double price) {
        return Double.parseDouble(df2.format(price));
    }
}
