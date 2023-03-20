package fr.openent.crre.service.impl;

import fr.openent.crre.model.TransactionElement;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.Arrays;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Neo4j.class}) //Prepare the static class you want to test
public class DefaultStructureServiceTest {

    private DefaultStructureService defaultStructureService;
    private Neo4j neo;

    @Before
    public void setup() {
        this.neo = Mockito.spy(Neo4j.getInstance());
        PowerMockito.spy(Neo4j.class);
        PowerMockito.when(Neo4j.getInstance()).thenReturn(neo);
        this.defaultStructureService = PowerMockito.spy(new DefaultStructureService(null, null));
    }

    @Test
    public void getTransactionUpdateAmountLicenceTest(TestContext ctx) {
        TransactionElement transactionElement1 = this.defaultStructureService.getTransactionUpdateAmountLicence("structureId", "+", 8413, true);
        TransactionElement transactionElement2 = this.defaultStructureService.getTransactionUpdateAmountLicence("structureId2", "/", 6461358, false);
        String expectedQuery1 = "UPDATE  null.licences SET consumable_amount = consumable_amount + ?  WHERE id_structure = ? ;";
        String expectedParams1 = "[8413,\"structureId\"]";
        String expectedQuery2 = "UPDATE  null.licences SET amount = amount / ?  WHERE id_structure = ? ;";
        String expectedParams2 = "[6461358,\"structureId2\"]";
        ctx.assertEquals(transactionElement1.getQuery(), expectedQuery1);
        ctx.assertEquals(transactionElement1.getParams().toString(), expectedParams1);
        ctx.assertEquals(transactionElement2.getQuery(), expectedQuery2);
        ctx.assertEquals(transactionElement2.getParams().toString(), expectedParams2);
    }

    @Test
    public void getStructuresFilterTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL AND s.contract IN {type} AND s.id IN {structures} RETURN s.id as idStructure; ";
        String expectedParams = "{\"type\":[\"structureType1\",\"structureType2\"],\"structures\":[\"structureId1\",\"structureId2\"]}";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonObject params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();

            return null;
        }).when(this.neo).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));
        this.defaultStructureService.getStructuresFilter(Arrays.asList("structureType1", "structureType2"), Arrays.asList("structureId1", "structureId2"));
        async.awaitSuccess(10000);
    }
}
