package fr.openent.crre.service.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
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

import java.util.Arrays;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class}) //Prepare the static class you want to test
public class DefaultProjectServiceTest {
    DefaultProjectService defaultProjectService;
    Vertx vertx;
    private Sql sql;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        this.defaultProjectService = new DefaultProjectService();
    }

    @Test
    public void getOrderClientEquipmentListTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT p.* FROM crre.project p LEFT JOIN crre.\"order-region-equipment-old\" o_r_e_o ON " +
                "p.id = o_r_e_o.id_project LEFT JOIN crre.\"order-region-equipment\" o_r_e ON o_r_e.id_project = " +
                "p.id WHERE o_r_e_o.id IN (?,?,?) OR o_r_e.id IN (?,?,?);";
        String expectedParams = "[591,220,75,591,220,75]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultProjectService.getProjectList(Arrays.asList(591, 220, 75));

        async.awaitSuccess(10000);
    }
}