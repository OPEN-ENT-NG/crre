package fr.openent.crre.service.impl;

import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.TransactionElement;
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
public class DefaultBasketOrderServiceTest {

    private DefaultBasketOrderService defaultBasketService;
    private Sql sql;

    @Before
    public void setup() {
        this.defaultBasketService = PowerMockito.spy(new DefaultBasketOrderService());
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(TransactionHelper.class);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
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

        this.defaultBasketService.getMyBasketOrders(userInfos.getUserId(), 1, 30, "startDate", "endDate", false);

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

    @Test
    public void getTransactionInsertBasketNameTest(TestContext ctx) {
        UserInfos userInfos = new UserInfos();
        userInfos.setUsername("userName");
        userInfos.setUserId("userId");
        TransactionElement transactionElement = this.defaultBasketService
                .getTransactionInsertBasketName(userInfos, "idStructure", 8, "basketName", 10.5d, 89);

        String expectedQuery = "INSERT INTO null.basket_order(name, id_structure, id_campaign, name_user, id_user, total," +
                " amount, created)VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) returning id;";
        String expectedParams = "[\"basketName\",\"idStructure\",8,\"userName\",\"userId\",10.5,89]";
        ctx.assertEquals(transactionElement.getQuery(), expectedQuery);
        ctx.assertEquals(transactionElement.getParams().toString(), expectedParams);
    }

    @Test
    public void getBasketOrderListTest(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> idList = Arrays.asList(84, 62, 84);
        String expectedQuery = "SELECT * FROM null.basket_order WHERE id IN (?,?,?);";
        String expectedParams = "[84,62,84]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());

        this.defaultBasketService.getBasketOrderList(idList);

    }
}
