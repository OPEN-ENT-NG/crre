package fr.openent.crre.service.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class DefaultOrderRegionServiceTest {
    DefaultOrderRegionService defaultOrderRegionService;
    Vertx vertx;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.defaultOrderRegionService = new DefaultOrderRegionService("order_region_table");
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.crre");
    }

    @Test
    public void testUpdateOldOrdersWithTransaction(TestContext ctx) {
        Async async = ctx.async();
        JsonArray orderList = new JsonArray();
        for(int i = 0; i < 100; i++) {
            List<String> list = Arrays.asList("0", "1", "2","3", "4", "6", "7","9", "10", "14", "15", "20","35", "55", "57", "58", "59","70", "71", "72", "52", "1000");
            orderList.add(new JsonObject().put("status", list.get(i%list.size())).put("id", String.valueOf(i+1)));
        }

        String expectedQuery = "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;";
        String expectedParams = "[\"55\",\"14\",\"36\",\"58\",\"80\",\"57\",\"15\",\"37\",\"59\",\"81\",\"35\",\"13\"," +
                "\"35\",\"57\",\"79\",\"58\",\"16\",\"38\",\"60\",\"82\",\"14\",\"10\",\"32\",\"54\",\"76\",\"98\",\"59\"," +
                "\"17\",\"39\",\"61\",\"83\",\"15\",\"11\",\"33\",\"55\",\"77\",\"99\",\"1000\",\"22\",\"44\",\"66\",\"88\"," +
                "\"0\",\"1\",\"23\",\"45\",\"67\",\"89\",\"1\",\"2\",\"24\",\"46\",\"68\",\"90\",\"2\",\"3\",\"25\",\"47\"," +
                "\"69\",\"91\",\"3\",\"4\",\"26\",\"48\",\"70\",\"92\",\"4\",\"5\",\"27\",\"49\",\"71\",\"93\",\"6\",\"6\"," +
                "\"28\",\"50\",\"72\",\"94\",\"7\",\"7\",\"29\",\"51\",\"73\",\"95\",\"70\",\"18\",\"40\",\"62\",\"84\"," +
                "\"9\",\"8\",\"30\",\"52\",\"74\",\"96\",\"71\",\"19\",\"41\",\"63\",\"85\",\"72\",\"20\",\"42\",\"64\"," +
                "\"86\",\"52\",\"21\",\"43\",\"65\",\"87\",\"20\",\"12\",\"34\",\"56\",\"78\",\"100\",\"10\",\"9\",\"31\"," +
                "\"53\",\"75\",\"97\"]";

        this.vertx.eventBus().consumer("fr.openent.crre", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        this.defaultOrderRegionService.updateOldOrdersWithTransaction(orderList);

        async.awaitSuccess(10000);
    }
}