package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.core.constants.Field;
import fr.openent.crre.model.TransactionElement;
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

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({Sql.class}) //Prepare the static class you want to test
public class DefaultOrderServiceTest {
    DefaultOrderService defaultOrderService;
    Vertx vertx;
    private Sql sql;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        this.defaultOrderService = new DefaultOrderService("", "");
    }

    @Test
    public void getOrderClientEquipmentListTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT * FROM null.order_client_equipment WHERE id IN (?,?,?);";
        String expectedParams = "[7276,46,6]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderService.getOrderClientEquipmentList(Arrays.asList(7276, 46, 6));

        async.awaitSuccess(10000);
    }

    @Test
    public void getOrderClientEquipmentListFromBasketIdTest(TestContext ctx) {
        Async async = ctx.async();
        List<Integer> idList = Arrays.asList(62, 18, 9);
        String expectedQuery = "SELECT row_to_json(o_c_e.*) " +
        "FROM null.basket_order AS bo " +
                "LEFT JOIN null.order_client_equipment AS o_c_e ON (bo.id = o_c_e.id_basket) " +
                "WHERE o_c_e.id_basket IN " + Sql.listPrepared(idList) + " " +
                "UNION ALL " +
                "SELECT row_to_json(o_c_e_o.*) " +
                "FROM null.basket_order AS bo " +
                "LEFT JOIN null.order_client_equipment_old AS o_c_e_o ON (bo.id = o_c_e_o.id_basket) " +
                "WHERE o_c_e_o.id_basket IN " + Sql.listPrepared(idList) + ";";
        String expectedParams = "[62,18,9,62,18,9]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderService.getOrderClientEquipmentListFromBasketId(Arrays.asList(62, 18, 9));

        async.awaitSuccess(10000);
    }
}