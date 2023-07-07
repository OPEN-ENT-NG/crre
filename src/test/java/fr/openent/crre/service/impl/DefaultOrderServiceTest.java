package fr.openent.crre.service.impl;

import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.model.FilterModel;
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

import java.util.*;

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

    @Test
    public void searchTest(TestContext ctx) {
        Async async = ctx.async();

        Map<String, List<String>> filters = new HashMap<>();
        filters.put("id_user", Arrays.asList("8555", "4846"));
        filters.put("id_campaign", Arrays.asList("64353", "46813"));
        filters.put("other_field", Arrays.asList(";DROP CASCADE"));

        List<String> equipementIdList = Arrays.asList("a198f4c", "ade86f");

        String expectedQuery = "SELECT oe.*, bo.*, bo.name as basket_name, bo.name_user as user_name, oe.amount as amount," +
                " oe.id as id, tc.name as type_name, to_json(c.* ) campaign " +
                "FROM null.order_client_equipment oe " +
                "LEFT JOIN null.basket_order bo ON (bo.id = oe.id_basket) " +
                "LEFT JOIN null.campaign c ON (c.id = oe.id_campaign) " +
                "LEFT JOIN null.type_campaign tc ON (tc.id = c.id_type) " +
                "WHERE oe.creation_date BETWEEN ? AND ? AND oe.id_campaign = ? AND " +
                "(lower(bo.name) ~* ? OR lower(bo.name_user) ~* ? OR oe.equipment_key IN (?,?)) AND oe.status = 'WAITING' " +
                "AND oe.id_structure = ?  AND ( bo.id_user IN (?,?) AND oe.id_campaign IN (?,?)) ORDER BY creation_date DESC OFFSET ? LIMIT ? ";
        String expectedParams = "[\"startDate\",\"endDate\",18,\"query_search\",\"query_search\",\"a198f4c\",\"ade86f\"," +
                "\"idStructure\",\"8555\",\"4846\",\"64353\",\"46813\",135,15]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderService.search("query_search", filters, "idStructure", equipementIdList, 18, "startDate", "endDate", 9);
        async.awaitSuccess(10000);
    }

    @Test
    public void listOrderTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT o_u.*," +
                " to_jsonb(basket.*) basket, to_jsonb(campaign.*) campaign, to_jsonb(project.*) project, to_jsonb(students.*)" +
                " students FROM crre.order_universal as o_u LEFT JOIN crre.basket_order basket on o_u.id_basket = basket.id" +
                " LEFT JOIN crre.project project on o_u.id_project = project.id LEFT JOIN crre.campaign campaign on campaign" +
                ".id = o_u.id_campaign LEFT JOIN crre.students students on students.id_structure = o_u.id_structure WHERE" +
                " (prescriber_validation_date BETWEEN ? AND ?) AND campaign.id IN (?,?) AND o_u.id_structure IN (?,?) AND" +
                " basket.id IN (?,?) AND o_u.prescriber_id IN (?,?) AND o_u.status IN (?,?) ORDER BY o_u.prescriber_validation_date ASC";
        String expectedParams = "[\"startDate\",\"endDate\",185,56,\"structureId1\",\"structureId2\",\"basketId1\"," +
                "\"basketId2\",\"userId1\",\"userId2\",\"SENT\",\"DONE\"]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        FilterModel filterModel = new FilterModel()
                .setIdsCampaign(Arrays.asList(185, 56))
                .setIdsStructure(Arrays.asList("structureId1", "structureId2"))
                .setIdsUser(Arrays.asList("userId1", "userId2"))
                .setIdsBasket(Arrays.asList("basketId1", "basketId2"))
                .setStartDate("startDate")
                .setEndDate("endDate")
                .setStatus(Arrays.asList(OrderStatus.SENT, OrderStatus.DONE))
                .setOrderByForOrderList(DefaultOrderService.OrderByOrderListEnum.PRESCRIBER_VALIDATION_DATE)
                .setOrderDescForOrderList(false);

        this.defaultOrderService.listOrder(filterModel);
    }
}