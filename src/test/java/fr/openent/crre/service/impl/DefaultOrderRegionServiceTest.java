package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderClientEquipmentType;
import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.model.FilterModel;
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
public class DefaultOrderRegionServiceTest {
    DefaultOrderRegionService defaultOrderRegionService;
    Vertx vertx;
    private Sql sql;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        this.sql.init(vertx.eventBus(), "fr.openent.crre");
        this.defaultOrderRegionService = new DefaultOrderRegionService("order_region_table");
    }

    @Test
    public void testUpdateOldOrdersWithTransaction(TestContext ctx) {
        Async async = ctx.async();
        JsonArray orderList = new JsonArray();
        for (int i = 0; i < 100; i++) {
            List<String> list = Arrays.asList("0", "1", "2", "3", "4", "6", "7", "9", "10", "14", "15", "20", "35", "55", "57", "58", "59", "70", "71", "72", "52", "1000");
            orderList.add(new JsonObject().put("status", list.get(i % list.size())).put("id", String.valueOf(i + 1)));
        }

        String expectedQuery = "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;" +
                "BEGIN;UPDATE null.\"order-region-equipment-old\"  SET id_status = ? WHERE id IN (?,?,?,?,?);COMMIT;";
        String expectedParams = "[\"55\",\"14\",\"36\",\"58\",\"80\",\"57\",\"15\",\"37\",\"59\",\"81\",\"35\",\"13\"," +
                "\"35\",\"57\",\"79\",\"58\",\"16\",\"38\",\"60\",\"82\",\"14\",\"10\",\"32\",\"54\",\"76\",\"98\",\"59\"," +
                "\"17\",\"39\",\"61\",\"83\",\"15\",\"11\",\"33\",\"55\",\"77\",\"99\",\"1000\",\"22\",\"44\",\"66\",\"88\"," +
                "\"0\",\"1\",\"23\",\"45\",\"67\",\"89\",\"1\",\"2\",\"24\",\"46\",\"68\",\"90\",\"2\",\"3\",\"25\",\"47\"," +
                "\"69\",\"91\",\"3\",\"4\",\"26\",\"48\",\"70\",\"92\",\"4\",\"5\",\"27\",\"49\",\"71\",\"93\",\"6\",\"6\"," +
                "\"28\",\"50\",\"72\",\"94\",\"7\",\"7\",\"29\",\"51\",\"73\",\"95\",\"70\",\"18\",\"40\",\"62\",\"84\"," +
                "\"9\",\"8\",\"30\",\"52\",\"74\",\"96\",\"71\",\"19\",\"41\",\"63\",\"85\",\"72\",\"20\",\"42\",\"64\"," +
                "\"86\",\"52\",\"21\",\"43\",\"65\",\"87\",\"20\",\"12\",\"34\",\"56\",\"78\",\"100\",\"10\",\"9\",\"31\"," +
                "\"53\",\"75\",\"97\"]";

        this.vertx.eventBus().consumer("fr.openent.crre", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams, body.getJsonArray("values").toString());
            async.complete();
        });

        this.defaultOrderRegionService.updateOldOrdersWithTransaction(orderList);

        async.awaitSuccess(10000);
    }

    @Test
    public void getTransactionCreateOrdersRegionTest(TestContext ctx) {
        JsonObject data = new JsonObject().put(Field.AMOUNT, 7)
                .put(Field.CREATION_DATE, "creationDate")
                .put(Field.USER_NAME, "userName")
                .put(Field.USER_ID, "userId")
                .put(Field.EQUIPMENT_KEY, "equipment_key")
                .put(Field.ID_CAMPAIGN, 5)
                .put(Field.ID_STRUCTURE, "id_structure")
                .put(Field.COMMENT, "comment")
                .put(Field.ID_ORDER_CLIENT_EQUIPMENT, 8L)
                .put(Field.REASSORT, true);
        TransactionElement transactionElement = this.defaultOrderRegionService.getTransactionCreateOrdersRegion(data, 9);
        String expectedQuery = "INSERT INTO null.\"order-region-equipment\"  (amount, creation_date,  owner_name, owner_id," +
                " status, equipment_key, id_campaign, id_structure, comment, id_order_client_equipment, id_project, reassort)" +
                "   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING * ;";
        String expectedParams = "[7,\"creationDate\",\"userName\",\"userId\",\"IN_PROGRESS\",\"equipment_key\",5,\"id_structure\"," +
                "\"comment\",8,9,true]";
        ctx.assertEquals(transactionElement.getQuery(), expectedQuery);
        ctx.assertEquals(transactionElement.getParams().toString(), expectedParams);
    }

    @Test
    public void getOrdersRegionByIdTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT * FROM null.\"order-region-equipment\"WHERE id IN (?,?,?)";
        String expectedParams = "[184,5946,84]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.getOrdersRegionById(Arrays.asList(184, 5946, 84));

        async.awaitSuccess(10000);
    }

    @Test
    public void updateOrdersStatusTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "UPDATE null.\"order-region-equipment\"  SET  status = ?, cause_status = ? WHERE id in (?,?,?)" +
                " ; UPDATE null.order_client_equipment SET  status = ?, cause_status = ? WHERE id in ( " +
                "SELECT ore.id_order_client_equipment FROM null.\"order-region-equipment\" ore WHERE id in (?,?,?) );";
        String expectedParams = "[\"STATUS\",\"justification\",184,5946,84,\"STATUS\",\"justification\",184,5946,84]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.updateOrdersStatus(Arrays.asList(184, 5946, 84), "status", "justification");

        async.awaitSuccess(10000);
    }

    @Test
    public void getOrderRegionEquipmentInSameProjectTest(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT array_to_json(array_agg(o_r_e.*)), row_to_json(p.*) as project FROM " +
                "crre.\"order-region-equipment\" o_r_e INNER JOIN crre.project p on o_r_e.id_project = " +
                "p.id WHERE p.id IN (?,?,?) GROUP BY p.id";
        String expectedParams = "[846,184,30]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.getOrderRegionEquipmentInSameProject(Arrays.asList(846, 184, 30), false);

        async.awaitSuccess(10000);
    }

    @Test
    public void getOrderRegionEquipmentInSameProjectTest_Old(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT array_to_json(array_agg(o_r_e.*)), row_to_json(p.*) as project FROM " +
                "crre.\"order-region-equipment-old\" o_r_e INNER JOIN crre.project p on o_r_e.id_project = " +
                "p.id WHERE p.id IN (?,?,?) GROUP BY p.id";
        String expectedParams = "[846,184,30]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.getOrderRegionEquipmentInSameProject(Arrays.asList(846, 184, 30), true);

        async.awaitSuccess(10000);
    }

    @Test
    public void getAllOrderRegionByProjectTest(TestContext ctx) {
        Async async = ctx.async();
        FilterModel filterModel = new FilterModel();
        filterModel.setStatus(Arrays.asList(OrderClientEquipmentType.SENT, OrderClientEquipmentType.RESUBMIT));

        String expectedQuery = "SELECT to_jsonb(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit," +
                " p.title AS title, to_jsonb(o_c_e.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde," +
                " st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro, st.secondetechno, st.premieretechno," +
                " st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, o_r_e.id, o_r_e.id_structure, o_r_e.amount, o_r_e.creation_date," +
                " o_r_e.modification_date, o_r_e.owner_name, o_r_e.owner_id, o_r_e.status, o_r_e.equipment_key, o_r_e.cause_status," +
                " o_r_e.comment, o_r_e.id_project, o_r_e.id_order_client_equipment, o_r_e.reassort, NULL as total_free," +
                " null as image, null as name, null as price, null as offers, null as editeur, null as distributeur," +
                " null as _index, null as status_name, -1 as status_id, false as old FROM  null.project p " +
                "LEFT JOIN null.\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project AND o_r_e.status IN (?,?)" +
                " LEFT JOIN null.order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment" +
                " LEFT JOIN null.basket_order AS bo ON (bo.id = o_c_e.id_basket) LEFT JOIN  null.campaign ON (o_r_e.id_campaign = campaign.id)" +
                " LEFT JOIN null.students AS st ON (o_r_e.id_structure = st.id_structure) WHERE o_r_e.id_project IN (?,?)" +
                " AND o_r_e.equipment_key IS NOT NULL GROUP BY o_r_e.id, campaign.name, campaign.use_credit, campaign.*, p.title," +
                " o_c_e.id, bo.name, bo.id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro," +
                " st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name," +
                " status_id UNION SELECT to_jsonb(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, p.title" +
                " AS title, to_jsonb(o_c_e_o.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde, st.premiere," +
                " st.terminale, st.secondepro, st.premierepro, st.terminalepro, st.secondetechno, st.premieretechno, st.terminaletechno," +
                " st.cap1, st.cap2, st.cap3, st.bma1, st.bma2 , o_r_e_o.id, o_r_e_o.id_structure, o_r_e_o.amount,o_r_e_o.creation_date, o_r_e_o.modification_date," +
                " o_r_e_o.owner_name, o_r_e_o.owner_id, o_r_e_o.status, o_r_e_o.equipment_key, o_r_e_o.cause_status, o_r_e_o.comment," +
                " o_r_e_o.id_project, o_r_e_o.id_order_client_equipment, o_r_e_o.reassort, o_r_e_o.total_free, o_r_e_o.equipment_image" +
                " as image, o_r_e_o.equipment_name as name, o_r_e_o.equipment_price as price, to_jsonb(o_c_e_o.offers) as offers," +
                " o_r_e_o.equipment_editor as editeur, o_r_e_o.equipment_diffusor as distributeur, o_r_e_o.equipment_format as _index," +
                " s.name as status_name, s.id as status_id, true as old FROM  null.project p LEFT JOIN null.\"order-region-equipment-old\"" +
                " o_r_e_o ON p.id = o_r_e_o.id_project AND o_r_e_o.status IN (?,?) LEFT JOIN null.order_client_equipment_old" +
                " AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment LEFT JOIN null.basket_order AS bo ON" +
                " (bo.id = o_c_e_o.id_basket) LEFT JOIN  null.campaign ON (o_r_e_o.id_campaign = campaign.id) LEFT JOIN null.students" +
                " AS st ON (o_r_e_o.id_structure = st.id_structure) LEFT JOIN  null.status AS s ON s.id = o_r_e_o.id_status" +
                " WHERE o_r_e_o.id_project IN (?,?) GROUP BY o_r_e_o.id, campaign.name, campaign.use_credit, campaign.*, p.title," +
                " o_c_e_o.id, bo.name, bo.id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro," +
                " st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name," +
                " status_id;";
        String expectedParams = "[\"SENT\",\"RESUBMIT\",18,2846,\"SENT\",\"RESUBMIT\",18,2846]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.getAllOrderRegionByProject(Arrays.asList(18, 2846), filterModel);
        async.awaitSuccess(10000);
    }

    @Test
    public void searchTest(TestContext ctx) {
        Async async = ctx.async();
        FilterModel filterModel = new FilterModel();
        filterModel.setStartDate("startDate");
        filterModel.setEndDate("endDate");
        filterModel.setStatus(Arrays.asList(OrderClientEquipmentType.SENT, OrderClientEquipmentType.RESUBMIT));
        filterModel.setSearchingText("searching text");
        filterModel.setIdsStructure(Arrays.asList("idStructure1", "idStructure2"));
        filterModel.setRenew(true);
        filterModel.setPage(5);
        FilterItemModel filterItem = new FilterItemModel();

        String expectedQuery = "SELECT DISTINCT p.*, COALESCE (o_r_e_o.creation_date, o_r_e.creation_date) as creationDate, " +
                "count(o_r_e.*) + count(o_r_e_o.*) AS nbOrders , MAX(s.name) as orderName, MAX(s.uai) as orderUai " +
                "FROM  null.project p LEFT JOIN null.\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project " +
                "AND o_r_e_o.status IN (?,?) LEFT JOIN null.\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project " +
                "AND o_r_e.status IN (?,?) LEFT JOIN null.order_client_equipment_old AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment " +
                "LEFT JOIN null.order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment " +
                "LEFT JOIN null.basket_order AS b ON (b.id = o_c_e.id_basket OR b.id = o_c_e_o.id_basket) " +
                "LEFT JOIN null.structure AS s ON (o_r_e.id_structure = s.id_structure OR o_r_e_o.id_structure = s.id_structure) " +
                "WHERE ((o_r_e.creation_date BETWEEN ? AND ? AND o_r_e.equipment_key IS NOT NULL) OR (o_r_e_o.creation_date BETWEEN ? AND ?)) " +
                "AND (lower(s.uai) ~* ? OR lower(s.name) ~* ? OR lower(s.city) ~* ? OR lower(s.region) ~* ? OR lower(s.public) ~* ? " +
                "OR lower(s.catalog) ~* ? OR lower(p.title) ~* ? OR lower(o_r_e.owner_name) ~* ? OR lower(o_r_e_o.owner_name) ~* ? " +
                "OR lower(b.name) ~* ? OR o_r_e_o.equipment_name ~* ?  OR o_r_e.equipment_key IN (?,?) OR o_r_e_o.equipment_key IN (?,?))  " +
                "AND (o_r_e.id_structure IN (?,?) OR o_r_e_o.id_structure IN (?,?) ) AND o_r_e_o.owner_id ~* 'renew'  " +
                "GROUP BY p.id, creationDateOFFSET ? LIMIT ? ";
        String expectedParams = "[\"SENT\",\"RESUBMIT\",\"SENT\",\"RESUBMIT\",\"startDate\",\"endDate\",\"startDate\"," +
                "\"endDate\",\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\"," +
                "\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\"," +
                "\"equipement1\",\"equipement2\",\"equipement1\",\"equipement2\",\"idStructure1\",\"idStructure2\",\"idStructure1\"," +
                "\"idStructure2\",50,10]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.search(filterModel, filterItem, Arrays.asList("equipement1", "equipement2"), Arrays.asList("equipement1", "equipement2"));
         async.awaitSuccess(10000);
    }
}