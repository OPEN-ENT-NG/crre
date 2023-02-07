package fr.openent.crre.service.impl;

import com.sun.java.swing.plaf.motif.MotifBorders;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.Either;
import io.gatling.commons.stats.assertion.In;
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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.*;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class}) //Prepare the static class you want to test
public class DefaultOrderRegionServiceTest {
    DefaultOrderRegionService defaultOrderRegionService;
    Vertx vertx;
    private Sql sql;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        this.sql.init(vertx.eventBus(), "fr.openent.crre");
        this.defaultOrderRegionService = new DefaultOrderRegionService("order_region_table");
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

    @Test
    public void getTransactionCreateOrdersRegionTest(TestContext ctx) {
        JsonObject data = new JsonObject().put(Field.AMOUNT, 7)
                .put(Field.CREATION_DATE, "creationDate")
                .put(Field.USER_NAME, "userName")
                .put(Field.USER_ID, "userId")
                .put(Field.EQUIPMENT_KEY, "equipment_key")
                .put(Field.ID_CAMPAIGN, 5)
                .put(Field.ID_STRUCTURE, "id_structure")
                .put(Field.COMMENT, "comment")
                .put(Field.ID_ORDER_CLIENT_EQUIPMENT, 8L)
                .put(Field.REASSORT, true);
        TransactionElement transactionElement = this.defaultOrderRegionService.getTransactionCreateOrdersRegion(data, 9);
        String expectedQuery = "INSERT INTO null.\"order-region-equipment\"  (amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, id_campaign, id_structure, comment, id_order_client_equipment, id_project, reassort)" +
                "   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING * ;";
        String expectedParams = "[7,\"creationDate\",\"userName\",\"userId\",\"IN PROGRESS\",\"equipment_key\",5,\"id_structure\"," +
                "\"comment\",8,9,true]";
        ctx.assertEquals(transactionElement.getQuery(), expectedQuery);
        ctx.assertEquals(transactionElement.getParams().toString(), expectedParams);
    }

    @Test
    public void createProjectTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO null.project ( title ) VALUES ( ? )  RETURNING *;";
        String expectedParams = "[\"myTitle\"]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.createProject("myTitle");

        async.awaitSuccess(10000);
    }
}