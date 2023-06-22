package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.PurseModel;
import fr.openent.crre.model.StructureNeo4jModel;
import fr.openent.crre.model.TransactionElement;
import fr.openent.crre.service.ServiceFactory;
import fr.openent.crre.service.StructureService;
import io.vertx.core.Future;
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
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class, DefaultPurseService.class, TransactionHelper.class}) //Prepare the static class you want to test
public class DefaultPurseServiceTest {
    private Sql sql;
    private DefaultPurseService defaultPurseService;
    private ServiceFactory serviceFactory;
    private DefaultStructureService structureService;

    @Before
    public void setup() {
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(TransactionHelper.class);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        this.serviceFactory = Mockito.mock(ServiceFactory.class);
        this.structureService = Mockito.mock(DefaultStructureService.class);
        Mockito.when(this.serviceFactory.getStructureService()).thenReturn(this.structureService);
        this.defaultPurseService = PowerMockito.spy(new DefaultPurseService(this.serviceFactory));
    }

    @Test
    public void getTransactionUpdatePurseAmountTest(TestContext ctx) {
        TransactionElement transactionElement1 = this.defaultPurseService.getTransactionUpdatePurseAmount(80.5d, "idStructure", "+", true);
        TransactionElement transactionElement2 = this.defaultPurseService.getTransactionUpdatePurseAmount(805846.5d, "idStructure2", "*", false);
        String expectedQuery1 = "UPDATE  null.purse SET consumable_amount = ROUND((consumable_amount + ? )::numeric ,2)::double precision WHERE id_structure = ? ;";
        String expectedParams1 = "[80.5,\"idStructure\"]";
        String expectedQuery2 = "UPDATE  null.purse SET amount = ROUND((amount * ? )::numeric ,2)::double precision WHERE id_structure = ? ;";
        String expectedParams2 = "[805846.5,\"idStructure2\"]";
        ctx.assertEquals(transactionElement1.getQuery(), expectedQuery1);
        ctx.assertEquals(transactionElement1.getParams().toString(), expectedParams1);
        ctx.assertEquals(transactionElement2.getQuery(), expectedQuery2);
        ctx.assertEquals(transactionElement2.getParams().toString(), expectedParams2);
    }

    @Test
    public void getPursesFromSqlTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery = "SELECT * FROM null.purse INNER JOIN null.structure ON structure.id_structure = purse.id_structure WHERE purse.id_structure IN (?,?) OFFSET ? LIMIT ? ";
        String expectedParams = "[\"idStructure1\",\"idStructure2\",90,30]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Whitebox.invokeMethod(this.defaultPurseService, "getPursesFromSql", 3, Arrays.asList("idStructure1", "idStructure2"));

        async.await(10000);
    }

    @Test
    public void searchPursesByUAITest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList(new StructureNeo4jModel().setId("13"))))
                .when(this.structureService).searchStructureByNameUai("uai");
        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList(new PurseModel().setIdStructure("13").setAmount(3.0), new PurseModel().setIdStructure("18"))))
                .when(this.defaultPurseService, "getPursesFromSql", Mockito.any(), Mockito.any());

        this.defaultPurseService.searchPursesByUAI(3, "uai")
                .onSuccess(result -> {
                    ctx.assertEquals(result.size(), 1);
                    ctx.assertEquals(result.get(0).getIdStructure(), "13");
                    ctx.assertEquals(result.get(0).getAmount(), 3.0);
                    ctx.assertEquals(result.get(0).getStructureNeo4jModel().getId(), "13");
                    async.complete();
                })
                .onFailure(ctx::fail);

        async.await(10000);
    }

    @Test
    public void getPursesTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList(new PurseModel().setIdStructure("17").setAmount(3.0), new PurseModel().setIdStructure("18"))))
                .when(this.defaultPurseService, "getPursesFromSql", Mockito.eq(4), Mockito.any());
        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList(new StructureNeo4jModel().setId("17"))))
                .when(this.structureService).getStructureNeo4jById(Mockito.any());

        this.defaultPurseService.getPurses(4, new ArrayList<>())
                .onSuccess(result -> {
                    ctx.assertEquals(result.size(), 1);
                    ctx.assertEquals(result.get(0).getIdStructure(), "17");
                    ctx.assertEquals(result.get(0).getAmount(), 3.0);
                    ctx.assertEquals(result.get(0).getStructureNeo4jModel().getId(), "17");
                    async.complete();
                })
                .onFailure(ctx::fail);

        async.await(10000);
    }

    @Test
    public void updateTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> transactionElementList = invocation.getArgument(0);
            ctx.assertEquals(transactionElementList.size(), 4);
            async.complete();
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        this.defaultPurseService.update("idStructure", new JsonObject().put(Field.INITIAL_AMOUNT, 90.5).put(Field.CONSUMABLE_INITIAL_AMOUNT, 18161.2));

        async.await(10000);
    }

    @Test
    public void incrementAddedInitialAmountFromNewValueTest(TestContext ctx) {
        TransactionElement result1 = this.defaultPurseService.incrementAddedInitialAmountFromNewValue(true, 8.8, "structureId1");
        TransactionElement result2 = this.defaultPurseService.incrementAddedInitialAmountFromNewValue(false, 489646.54, "structureId2");

        String expectedQuery1 = "UPDATE crre.purse SET added_consumable_initial_amount =  added_consumable_initial_amount + ? - consumable_initial_amount WHERE id_structure = ?";
        String expectedParams1 = "[8.8,\"structureId1\"]";

        ctx.assertEquals(result1.getQuery(), expectedQuery1);
        ctx.assertEquals(result1.getParams().toString(), expectedParams1);

        String expectedQuery2 = "UPDATE crre.purse SET added_initial_amount =  added_initial_amount + ? - initial_amount WHERE id_structure = ?";
        String expectedParams2 = "[489646.54,\"structureId2\"]";

        ctx.assertEquals(result2.getQuery(), expectedQuery2);
        ctx.assertEquals(result2.getParams().toString(), expectedParams2);
    }

    @Test
    public void setAddedInitialAmountTest(TestContext ctx) throws Exception {
        TransactionElement result1 = Whitebox.invokeMethod(this.defaultPurseService, "setAddedInitialAmount", "idStructure1", true, 3.1);
        TransactionElement result2 = Whitebox.invokeMethod(this.defaultPurseService, "setAddedInitialAmount", "idStructure2", false, 48961.54);

        String expectedQuery1 = "UPDATE null.purse SET added_consumable_initial_amount = ? WHERE id_structure = ?";
        String expectedParams1 = "[3.1,\"idStructure1\"]";

        ctx.assertEquals(result1.getQuery(), expectedQuery1);
        ctx.assertEquals(result1.getParams().toString(), expectedParams1);

        String expectedQuery2 = "UPDATE null.purse SET added_initial_amount = ? WHERE id_structure = ?";
        String expectedParams2 = "[48961.54,\"idStructure2\"]";

        ctx.assertEquals(result2.getQuery(), expectedQuery2);
        ctx.assertEquals(result2.getParams().toString(), expectedParams2);
    }
}
