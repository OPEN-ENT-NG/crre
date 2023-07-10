package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.BasketOrderItem;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.ServiceFactory;
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
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class, TransactionHelper.class, SqlHelper.class}) //Prepare the static class you want to test
public class DefaultBasketOrderItemServiceTest {
    private ServiceFactory serviceFactory;
    private DefaultBasketOrderItemService defaultBasketItemService;
    private Sql sql;
    private DefaultBasketOrderService basketOrderService;

    @Before
    public void setup() {
        this.serviceFactory = Mockito.mock(ServiceFactory.class);
        this.basketOrderService = Mockito.mock(DefaultBasketOrderService.class);
        Mockito.when(this.serviceFactory.getBasketOrderService()).thenReturn(this.basketOrderService);
        this.defaultBasketItemService = PowerMockito.spy(new DefaultBasketOrderItemService(this.serviceFactory));
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

        String expectedQuery = "SELECT * FROM null.basket_order_item basket" +
                " WHERE basket.id_campaign = ?" +
                " AND basket.id_structure = ?" +
                " AND basket.owner_id = ?" +
                " AND basket.id_item IN (?,?)" +
                " GROUP BY (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure)" +
                " ORDER BY basket.id DESC;";
        String expectedParams = "[1,\"idStructure\",\"userId\",\"18\",\"19\"]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketItemService.listBasketOrderItem(1, "idStructure", userInfos.getUserId(), Arrays.asList("18", "19"));

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

        this.defaultBasketItemService.create(basketOrderItem, userInfos)
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

        this.defaultBasketItemService.delete(10)
                .onSuccess(res -> {
                    ctx.assertEquals(res.toString(), expectedRes);
                    async.complete();
                })
                .onFailure(ctx::fail);

        async.awaitSuccess(10000);
    }

    @Test
    public void deleteListTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "DELETE FROM null.basket_order_item WHERE id IN (?,?,?) RETURNING *";
        String expectedParams = "[18,294,896]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketItemService.delete(Arrays.asList(18, 294, 896));

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

        this.defaultBasketItemService.updateAmount(userInfos, 60, 29);

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

        this.defaultBasketItemService.updateComment(26, "comment");

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

        this.defaultBasketItemService.updateReassort(26, false);

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
        this.defaultBasketItemService.listBasketItemForOrder(75, "structureId", "userId", basketIdList);

        async.awaitSuccess(10000);
    }

    @Test
    public void takeOrderTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        Mockito.doReturn(new TransactionElement("query", new JsonArray())).when(this.basketOrderService)
                .getTransactionInsertBasketName(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyDouble(), Mockito.anyInt());

        AtomicBoolean firstCall = new AtomicBoolean(true);
        PowerMockito.doAnswer(invocation -> {
            if (firstCall.get()) {
                String expectedQuery = "query";
                String expectedParams = "[]";
                List<TransactionElement> transactionElementList = invocation.getArgument(0);
                ctx.assertEquals(1, transactionElementList.size());
                ctx.assertEquals(expectedQuery, transactionElementList.get(0).getQuery());
                ctx.assertEquals(expectedParams, transactionElementList.get(0).getParams().toString());
                transactionElementList.get(0).setResult(new JsonArray().add(new JsonObject().put(Field.ID, 8)));
                firstCall.set(false);
                return Future.succeededFuture();
            } else {
                String expectedQuery1 = "INSERT INTO null.order_client_equipment (id, amount, id_campaign, id_structure," +
                        " status, equipment_key, comment, user_id, id_basket, reassort) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String expectedParams1 = "[29,30,65,\"idStructure\",\"WAITING\",\"idItem1\",\"comment1\",\"userId\",8,false]";
                String expectedQuery2 = "INSERT INTO null.order_client_equipment (id, amount, id_campaign, id_structure," +
                        " status, equipment_key, comment, user_id, id_basket, reassort) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String expectedParams2 = "[26,21,65,\"idStructure\",\"WAITING\",\"idItem2\",\"comment2\",\"userId\",8,true]";
                String expectedQueryDelete = " DELETE FROM null.basket_order_item  WHERE id_campaign = ? AND id_structure = ?" +
                        " AND owner_id = ? AND basket_order_item.id IN (?,?) RETURNING *";
                String expectedParamsDelete = "[65,\"idStructure\",\"userId\",21,262]";
                List<TransactionElement> transactionElementList = invocation.getArgument(0);
                ctx.assertEquals(3, transactionElementList.size());
                ctx.assertEquals(expectedQuery1, transactionElementList.get(0).getQuery());
                ctx.assertEquals(expectedParams1, transactionElementList.get(0).getParams().toString());
                ctx.assertEquals(expectedQuery2, transactionElementList.get(1).getQuery());
                ctx.assertEquals(expectedParams2, transactionElementList.get(1).getParams().toString());
                ctx.assertEquals(expectedQueryDelete, transactionElementList.get(2).getQuery());
                ctx.assertEquals(expectedParamsDelete, transactionElementList.get(2).getParams().toString());
                return Future.succeededFuture();
            }

        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());


        List<BasketOrderItem> basketOrderItemList = Arrays.asList(
                new BasketOrderItem()
                        .setAmount(30)
                        .setId(21)
                        .setReassort(false)
                        .setIdOrder(29)
                        .setIdCampaign(65)
                        .setIdStructure("idStructure")
                        .setIdItem("idItem1")
                        .setComment("comment1"),
                new BasketOrderItem()
                        .setAmount(21)
                        .setId(262)
                        .setReassort(true)
                        .setIdOrder(26)
                        .setIdCampaign(65)
                        .setIdStructure("idStructure")
                        .setIdItem("idItem2")
                        .setComment("comment2"));
        UserInfos user = new UserInfos();
        user.setUserId("userId");
        this.defaultBasketItemService.takeOrder(basketOrderItemList, 65, user, "idStructure", "basketName")
                .onSuccess(result -> {
                    ctx.assertEquals(result, 8);
                    async.complete();
                });
        async.awaitSuccess(10000);
    }
}
