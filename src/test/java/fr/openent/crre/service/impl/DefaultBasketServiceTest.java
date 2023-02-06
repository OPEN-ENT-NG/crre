package fr.openent.crre.service.impl;

import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.BasketOrderItem;
import fr.openent.crre.model.TransactionElement;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class, TransactionHelper.class, SqlHelper.class}) //Prepare the static class you want to test
public class DefaultBasketServiceTest {

    private DefaultBasketService defaultBasketService;
    private Sql sql;

    @Before
    public void setup() {
        this.defaultBasketService = PowerMockito.spy(new DefaultBasketService());
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(TransactionHelper.class);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
    }

    @Test
    public void listBasketOrderItemTest(TestContext ctx) {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");

        String expectedQuery = "SELECT id, amount, comment , processing_date, id_campaign, id_structure, id_item, reassort" +
                " FROM null.basket_order_item basket WHERE basket.id_campaign = ? AND basket.id_structure = ?" +
                " AND basket.owner_id = ? GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure)" +
                " ORDER BY basket.id DESC;";
        String expectedParams = "[1,\"idStructure\",\"userId\"]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.listBasketOrderItem(1, "idStructure", userInfos);

        async.awaitSuccess(10000);
    }

    @Test
    public void getMyBasketOrdersTest(TestContext ctx) {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");

        String expectedQuery = "SELECT distinct b.* FROM null.basket_order b INNER JOIN null.order_client_equipment oce on (oce.id_basket = b.id) WHERE b.created BETWEEN ? AND ? AND b.id_user = ? AND b.id_campaign = ? ORDER BY b.id DESC OFFSET ? LIMIT ? ";
        String expectedParams = "[\"startDate\",\"endDate\",\"userId\",30,15,15]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.getMyBasketOrders(userInfos, 1, 30, "startDate", "endDate", false);

        async.awaitSuccess(10000);
    }

    @Test
    public void createTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        PowerMockito.spy(SqlHelper.class);
        PowerMockito.doReturn(Future.succeededFuture(35)).when(SqlHelper.class, "getNextVal", Mockito.any());
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");
        userInfos.setUsername("userName");
        BasketOrderItem basketOrderItem = new BasketOrderItem()
                .setAmount(84)
                .setProcessingDate("processingDate")
                .setIdItem("846165")
                .setIdCampaign(82)
                .setIdStructure("idStructure");

        String expectedQuery = "INSERT INTO null.basket_order_item(id, amount, processing_date, id_item, id_campaign," +
                " id_structure, owner_id, owner_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        String expectedParams = "[35,84,\"processingDate\",\"846165\",82,\"idStructure\",\"userId\",\"userName\"]";
        String expectedRes = "{\"id\":35,\"idCampaign\":82}";

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 1);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams);
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        this.defaultBasketService.create(basketOrderItem, userInfos)
                .onSuccess(res -> {
                    ctx.assertEquals(res.toString(), expectedRes);
                    async.complete();
                })
                .onFailure(ctx::fail);

        async.awaitSuccess(10000);
    }

    @Test
    public void deleteTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");

        String expectedQuery = "DELETE FROM null.basket_order_item WHERE id= ? ;";
        String expectedParams = "[10]";
        String expectedRes = "{\"id\":10}";

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 1);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams);
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        this.defaultBasketService.delete(10)
                .onSuccess(res -> {
                    ctx.assertEquals(res.toString(), expectedRes);
                    async.complete();
                })
                .onFailure(ctx::fail);

        async.awaitSuccess(10000);
    }

    @Test
    public void updateAmountTest(TestContext ctx) {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");

        String expectedQuery = "UPDATE null.basket_order_item  SET  amount = ?  WHERE id = ? AND owner_id = ?; ";
        String expectedParams = "[29,60,\"userId\"]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.updateAmount(userInfos, 60, 29);

        async.awaitSuccess(10000);
    }

    @Test
    public void updateCommentTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "UPDATE null.basket_order_item  SET comment = ? WHERE id = ?; ";
        String expectedParams = "[\"comment\",26]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.updateComment(26, "comment");

        async.awaitSuccess(10000);
    }

    @Test
    public void updateReassortTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "UPDATE null.basket_order_item  SET reassort = ? WHERE id = ?; ";
        String expectedParams = "[false,26]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.updateReassort(26, false);

        async.awaitSuccess(10000);
    }

    @Test
    public void listBasketItemForOrderTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT basket.id ,basket.amount, basket.comment, basket.processing_date, basket.id_campaign," +
                " basket.id_structure, basket.reassort, basket.id_item, nextval('null.order_client_equipment_id_seq' )" +
                " as id_order FROM null.basket_order_item basket WHERE basket.id_campaign = ? AND basket.owner_id = ? " +
                "AND basket.id_structure = ? AND basket.id IN (?,?,?) GROUP BY (basket.id, basket.amount, " +
                "basket.processing_date,basket.id_campaign, basket.id_structure);";
        String expectedParams = "[75,\"userId\",\"structureId\",84,715,1315]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        List<Integer> basketIdList = Arrays.asList(84, 715, 1315);
        this.defaultBasketService.listBasketItemForOrder(75, "structureId", "userId", basketIdList);

        async.awaitSuccess(10000);
    }

    @Test
    public void searchTest(TestContext ctx) {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId("userId");
        userInfos.setStructures(Arrays.asList("structure1", "structure2"));

        String expectedQuery = "SELECT distinct bo.* FROM null.basket_order bo INNER JOIN null.order_client_equipment AS" +
                " oe ON (bo.id = oe.id_basket) WHERE bo.created BETWEEN ? AND ? AND bo.id_user = ? AND bo.id_campaign = ?" +
                " AND (lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_key IN (?,?)) AND bo.id_structure" +
                " IN ( ?,?) ORDER BY bo.id DESC OFFSET ? LIMIT ? ";
        String expectedParams = "[\"startDate\",\"endDate\",\"userId\",13,\"query\",\"query\",\"ean1\",\"ean2\",\"structure1\",\"structure2\",60,15]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());
        JsonArray equipTab = new JsonArray().add(new JsonObject().put("ean", "ean1")).add(new JsonObject().put("ean", "ean2"));

        this.defaultBasketService.search("query", userInfos, equipTab, 13, "startDate", "endDate", 4, false);

        async.awaitSuccess(10000);
    }
}
