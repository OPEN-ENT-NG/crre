package fr.openent.crre.service.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
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
public class DefaultUserServiceTest {
    DefaultUserService defaultUserService;
    Vertx vertx;
    private Neo4j neo4j;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.neo4j = Mockito.spy(Neo4j.getInstance());
        PowerMockito.spy(Neo4j.class);
        PowerMockito.when(Neo4j.getInstance()).thenReturn(neo4j);
        this.defaultUserService = new DefaultUserService();
    }

    @Test
    public void getOrderClientEquipmentListTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "MATCH (s:Structure)<--()--(u:User)-->(g:Group)-->(r:Role)-[:AUTHORIZE]->(w:WorkflowAction{displayName:'crre.validator'})" +
                " WHERE s.id IN {structureIdList} WITH r,u,s MATCH (wa:WorkflowAction{displayName:'crre.administrator'})" +
                " WHERE NOT ((r)-[:AUTHORIZE]->(wa)) return distinct u.id,s.id";
        String expectedParams = "{\"structureIdList\":[\"591\",\"220\",\"75\"]}";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonObject params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        this.defaultUserService.getValidatorUser(Arrays.asList("591", "220", "75"));

        async.awaitSuccess(10000);
    }
}