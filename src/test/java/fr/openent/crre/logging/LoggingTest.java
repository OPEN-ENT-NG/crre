package fr.openent.crre.logging;

import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.collections.JsonArray;
import fr.wseduc.webutils.collections.JsonObject;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
@PrepareForTest({TransactionHelper.class}) //Prepare the static class you want to test
public class LoggingTest {

    @Before
    public void setup() {
        PowerMockito.spy(TransactionHelper.class);
    }

    @Test
    public void insertJsonObjectTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUsername("userName");
        userInfos.setUserId("userId");
        String expectedQuery = "INSERT INTO null.logs(id_user, username, action, context, item, value) VALUES (?, ?, ?, ?, ?, to_json(?::text));";
        String expectedParams = "[\"userId\",\"userName\",\"action\",\"context\",\"id = item\",{\"data\":\"myData\"}]";

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> transactionElementList = invocation.getArgument(0);
            ctx.assertEquals(transactionElementList.size(), 1);
            ctx.assertEquals(transactionElementList.get(0).getQuery(), expectedQuery);
            ctx.assertEquals(transactionElementList.get(0).getParams().toString(), expectedParams);
            async.complete();
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any());

        Logging.insert(userInfos, "context", "action", "item", new JsonObject().put("data", "myData"));

        async.awaitSuccess(10000);
    }

    @Test
    public void insertJsonArrayTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        UserInfos userInfos = new UserInfos();
        userInfos.setUsername("userName");
        userInfos.setUserId("userId");
        String expectedQuery1 = "INSERT INTO null.logs(id_user, username, action, context, item, value) VALUES (?, ?, ?, ?, ?, to_json(?::text));";
        String expectedParams1 = "[\"userId\",\"userName\",\"action\",\"context\",\"id = item\",{\"data\":\"myData\"}]";
        String expectedQuery2 = "INSERT INTO null.logs(id_user, username, action, context, item, value) VALUES (?, ?, ?, ?, ?, to_json(?::text));";
        String expectedParams2 = "[\"userId\",\"userName\",\"action\",\"context\",\"id = item\",{\"data2\":\"myData2\"}]";

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> transactionElementList = invocation.getArgument(0);
            ctx.assertEquals(transactionElementList.size(), 2);
            ctx.assertEquals(transactionElementList.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(transactionElementList.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(transactionElementList.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(transactionElementList.get(1).getParams().toString(), expectedParams2);
            async.complete();
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any());

        JsonArray data = new JsonArray(Arrays.asList(new JsonObject().put("data", "myData"), new JsonObject().put("data2", "myData2")));

        Logging.insert(userInfos, "context", "action", "item", data);

        async.awaitSuccess(10000);
    }
}
