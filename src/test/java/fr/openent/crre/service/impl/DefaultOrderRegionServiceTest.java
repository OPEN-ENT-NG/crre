package fr.openent.crre.service.impl;

import fr.openent.crre.core.constants.Field;
import fr.openent.crre.core.enums.OrderStatus;
import fr.openent.crre.model.*;
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
        List<CRRELibraryElementModel> orderList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<String> list = Arrays.asList("0", "1", "2", "3", "4", "6", "7", "9", "10", "14", "15", "20", "35", "55", "57", "58", "59", "70", "71", "72", "52", "1000");
            orderList.add(new CRRELibraryElementModel().setEtat(list.get(i % list.size())).setCGIId(String.valueOf(i + 1)));
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
        FilterItemModel filterItemModel = new FilterItemModel();
        filterModel.setStatus(Arrays.asList(OrderStatus.SENT, OrderStatus.RESUBMIT));

        String expectedQuery = "SELECT to_jsonb(campaign.*) campaign, campaign.name AS campaign_name, campaign.use_credit, " +
                "p.title AS title, to_jsonb(o_c_e.*) AS order_parent, bo.name AS basket_name, bo.id AS basket_id, st.seconde, " +
                "st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro, st.secondetechno, st.premieretechno, " +
                "st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, o_r_e.id, o_r_e.id_structure, o_r_e.amount, " +
                "o_r_e.creation_date, o_r_e.modification_date, o_r_e.owner_name, o_r_e.owner_id, o_r_e.status, o_r_e.equipment_key, " +
                "o_r_e.cause_status, o_r_e.comment, o_r_e.id_project, o_r_e.id_order_client_equipment, o_r_e.reassort, NULL as total_free, " +
                "null as image, null as name, null as price, null as offers, null as editeur, null as distributeur, null as _index, " +
                "null as status_name, -1 as status_id, false as old FROM  null.project p " +
                "LEFT JOIN null.\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project AND o_r_e.status IN (?,?) " +
                "LEFT JOIN null.order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment " +
                "LEFT JOIN null.basket_order AS bo ON (bo.id = o_c_e.id_basket) " +
                "LEFT JOIN  null.campaign ON (o_r_e.id_campaign = campaign.id) " +
                "LEFT JOIN null.students AS st ON (o_r_e.id_structure = st.id_structure) " +
                "LEFT JOIN null.structure AS struct ON (o_r_e.id_structure = struct.id_structure) " +
                "WHERE o_r_e.id_project IN (?,?) AND o_r_e.equipment_key IS NOT NULL  " +
                "GROUP BY o_r_e.id, campaign.name, campaign.use_credit, campaign.*, p.title, o_c_e.id, bo.name, bo.id, st.seconde, " +
                "st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro, st.secondetechno, st.premieretechno, " +
                "st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name, status_id " +
                "UNION " +
                "SELECT to_jsonb(campaign.*) campaign, " +
                "campaign.name AS campaign_name, campaign.use_credit, p.title AS title, to_jsonb(o_c_e_o.*) AS order_parent, bo.name AS basket_name, " +
                "bo.id AS basket_id, st.seconde, st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro, " +
                "st.secondetechno, st.premieretechno, st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2 , " +
                "o_r_e_o.id, o_r_e_o.id_structure, o_r_e_o.amount,o_r_e_o.creation_date, o_r_e_o.modification_date, " +
                "o_r_e_o.owner_name, o_r_e_o.owner_id, o_r_e_o.status, o_r_e_o.equipment_key, o_r_e_o.cause_status, " +
                "o_r_e_o.comment, o_r_e_o.id_project, o_r_e_o.id_order_client_equipment, o_r_e_o.reassort, " +
                "o_r_e_o.total_free, o_r_e_o.equipment_image as image, o_r_e_o.equipment_name as name, " +
                "o_r_e_o.equipment_price as price, to_jsonb(o_c_e_o.offers) as offers, o_r_e_o.equipment_editor as editeur, " +
                "o_r_e_o.equipment_diffusor as distributeur, o_r_e_o.equipment_format as _index, s.name as status_name, " +
                "s.id as status_id, true as old " +
                "FROM  null.project p " +
                "LEFT JOIN null.\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project AND o_r_e_o.status IN (?,?) " +
                "LEFT JOIN null.order_client_equipment_old AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment " +
                "LEFT JOIN null.basket_order AS bo ON (bo.id = o_c_e_o.id_basket) LEFT JOIN  null.campaign ON (o_r_e_o.id_campaign = campaign.id) " +
                "LEFT JOIN null.students AS st ON (o_r_e_o.id_structure = st.id_structure) LEFT JOIN  null.status AS s ON s.id = o_r_e_o.id_status " +
                "LEFT JOIN null.structure AS struct ON (o_r_e_o.id_structure = struct.id_structure) WHERE o_r_e_o.id_project IN (?,?)  " +
                "GROUP BY o_r_e_o.id, campaign.name, campaign.use_credit, campaign.*, p.title, o_c_e_o.id, bo.name, bo.id, st.seconde, " +
                "st.premiere, st.terminale, st.secondepro, st.premierepro, st.terminalepro, st.secondetechno, st.premieretechno, " +
                "st.terminaletechno, st.cap1, st.cap2, st.cap3, st.bma1, st.bma2, status_name, status_id;";
        String expectedParams = "[\"SENT\",\"RESUBMIT\",18,2846,\"SENT\",\"RESUBMIT\",18,2846]";

        PowerMockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);
            ctx.assertEquals(query, expectedQuery);
            ctx.assertEquals(params.toString(), expectedParams);
            async.complete();
            return null;
        }).when(this.sql).prepared(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        this.defaultOrderRegionService.getAllOrderRegionByProject(Arrays.asList(18, 2846), filterModel, filterItemModel, new ArrayList<>(), new ArrayList<>());
        async.awaitSuccess(10000);
    }

    @Test
    public void searchTest(TestContext ctx) {
        Async async = ctx.async();
        FilterModel filterModel = new FilterModel();
        filterModel.setStartDate("startDate");
        filterModel.setEndDate("endDate");
        filterModel.setStatus(Arrays.asList(OrderStatus.SENT, OrderStatus.RESUBMIT));
        filterModel.setSearchingText("searching text");
        filterModel.setIdsStructure(Arrays.asList("idStructure1", "idStructure2"));
        filterModel.setRenew(true);
        filterModel.setPage(5);
        FilterItemModel filterItem = new FilterItemModel();

        String expectedQuery = "SELECT DISTINCT p.*, COALESCE (o_r_e_o.creation_date, o_r_e.creation_date) as creationDate," +
                " count(o_r_e.*) + count(o_r_e_o.*) AS nbOrders , MAX(s.name) as structure_name, MAX(s.uai) as uai" +
                " FROM  null.project p LEFT JOIN null.\"order-region-equipment-old\" o_r_e_o ON p.id = o_r_e_o.id_project AND o_r_e_o.status IN (?,?)" +
                " LEFT JOIN null.\"order-region-equipment\" o_r_e ON p.id = o_r_e.id_project AND o_r_e.status IN (?,?)" +
                " LEFT JOIN null.order_client_equipment_old AS o_c_e_o ON o_c_e_o.id = o_r_e_o.id_order_client_equipment" +
                " LEFT JOIN null.order_client_equipment AS o_c_e ON o_c_e.id = o_r_e.id_order_client_equipment" +
                " LEFT JOIN null.basket_order AS b ON (b.id = o_c_e.id_basket OR b.id = o_c_e_o.id_basket)" +
                " LEFT JOIN null.structure AS s ON (o_r_e.id_structure = s.id_structure OR o_r_e_o.id_structure = s.id_structure)" +
                " WHERE ((o_r_e.creation_date BETWEEN ? AND ? AND o_r_e.equipment_key IS NOT NULL) OR (o_r_e_o.creation_date BETWEEN ? AND ?))" +
                " AND (lower(s.uai) ~* ? OR lower(s.name) ~* ? OR lower(s.city) ~* ? OR lower(s.region) ~* ? OR lower(s.public) ~* ? OR lower(s.catalog) ~* ?" +
                " OR lower(p.title) ~* ? OR lower(o_r_e.owner_name) ~* ? OR lower(o_r_e_o.owner_name) ~* ? OR lower(b.name) ~* ?" +
                " OR lower(o_r_e_o.equipment_name) ~* ? OR lower(o_r_e_o.equipment_key) ~* ? OR lower(o_r_e.equipment_key) ~* ?" +
                " OR o_r_e.equipment_key IN (?,?))  AND (o_r_e.id_structure IN (?,?) OR o_r_e_o.id_structure IN (?,?) ) AND o_r_e_o.owner_id ~* 'renew'" +
                "  GROUP BY p.id, creationDateOFFSET ? LIMIT ? ";
        String expectedParams = "[\"SENT\",\"RESUBMIT\",\"SENT\",\"RESUBMIT\",\"startDate\",\"endDate\",\"startDate\",\"endDate\"," +
                "\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\"," +
                "\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\",\"searching text\"," +
                "\"searching text\",\"equipement1\",\"equipement2\",\"idStructure1\",\"idStructure2\",\"idStructure1\",\"idStructure2\"," +
                "50,10]";

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

    @Test
    public void insertOrderListTest(TestContext ctx) throws Exception {
        OrderUniversalModel orderUniversalModel1 = new OrderUniversalModel()
                .setOrderRegionId(183)
                .setAmount(13)
                .setValidatorValidationDate("validationDate")
                .setValidatorName("validatorName")
                .setValidatorId("validatorId")
                .setStatus(OrderStatus.DONE)
                .setEquipmentKey("equipmentKey")
                .setOrderClientId(45)
                .setProject(new ProjectModel().setId(8))
                .setReassort(false)
                .setTotalFree(4)
                .setEquipmentName("equipmentName")
                .setEquipmentImage("equipmentImage")
                .setEquipmentPrice(843.1)
                .setEquipmentGrade("grade")
                .setEquipmentEditor("editor")
                .setEquipmentDiffusor("diffusor")
                .setEquipmentCatalogueType("catalogueType")
                .setCampaign(new Campaign().setId(156))
                .setIdStructure("idStructure")
                .setComment("comment");

        OrderUniversalModel orderUniversalModel2 = new OrderUniversalModel()
                .setOrderRegionId(5651)
                .setAmount(156)
                .setValidatorValidationDate("validationDate2")
                .setValidatorName("validatorName2")
                .setValidatorId("validatorId2")
                .setStatus(OrderStatus.SENT)
                .setEquipmentKey("equipmentKey2")
                .setOrderClientId(56)
                .setProject(new ProjectModel().setId(5))
                .setReassort(false)
                .setTotalFree(15)
                .setEquipmentName("equipmentName2")
                .setEquipmentImage("equipmentImage2")
                .setEquipmentPrice(45273.7)
                .setEquipmentGrade("grade2")
                .setEquipmentEditor("editor2")
                .setEquipmentDiffusor("diffusor2")
                .setEquipmentCatalogueType("catalogueType2")
                .setCampaign(new Campaign().setId(15))
                .setIdStructure("idStructure2")
                .setComment("comment2");

        List<OrderUniversalModel> orderList = Arrays.asList(orderUniversalModel1, orderUniversalModel2);

        String expectedQuery = "INSERT INTO null.\"order-region-equipment-old\" (id, amount, creation_date,  owner_name," +
                " owner_id, status, equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade," +
                " equipment_editor, equipment_diffusor, equipment_format, id_campaign, id_structure, comment, id_order_client_equipment," +
                " id_project, reassort, id_status, total_free) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ,(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
        String expectedParams = "[183,13,\"validationDate\",\"validatorName\",\"validatorId\",\"DONE\",\"equipmentKey\"," +
                "\"equipmentName\",\"equipmentImage\",843.1,\"grade\",\"editor\",\"diffusor\",\"catalogueType\",156,\"idStructure\"," +
                "\"comment\",45,8,false,null,4,5651,156,\"validationDate2\",\"validatorName2\",\"validatorId2\",\"SENT\"," +
                "\"equipmentKey2\",\"equipmentName2\",\"equipmentImage2\",45273.7,\"grade2\",\"editor2\",\"diffusor2\"," +
                "\"catalogueType2\",15,\"idStructure2\",\"comment2\",56,5,false,null,15]";



        TransactionElement transactionElement = Whitebox.invokeMethod(this.defaultOrderRegionService, "insertOrderList", orderList);
        ctx.assertEquals(expectedQuery, transactionElement.getQuery());
        ctx.assertEquals(expectedParams, transactionElement.getParams().toString());
    }

    @Test
    public void insertOldClientOrderListTest(TestContext ctx) throws Exception {
        OrderUniversalModel orderUniversalModel1 = new OrderUniversalModel()
                .setOrderClientId(183)
                .setAmount(13)
                .setPrescriberValidationDate("validationDate")
                .setPrescriberId("prescriberId")
                .setEquipmentKey("equipmentKey")
                .setBasket(new BasketOrder().setId(95))
                .setReassort(false)
                .setOffers(new ArrayList<>())
                .setEquipmentTva5(198.25)
                .setEquipmentTva20(16.65)
                .setEquipmentPriceht(510.2)
                .setEquipmentName("equipmentName")
                .setEquipmentImage("equipmentImage")
                .setEquipmentPrice(843.1)
                .setEquipmentGrade("grade")
                .setEquipmentEditor("editor")
                .setEquipmentDiffusor("diffusor")
                .setEquipmentCatalogueType("catalogueType")
                .setCampaign(new Campaign().setId(156))
                .setIdStructure("idStructure")
                .setComment("comment")
                .setStatus(OrderStatus.SENT)
                .setProject(new ProjectModel().setId(15));

        OrderUniversalModel orderUniversalModel2 = new OrderUniversalModel()
                .setOrderClientId(42)
                .setAmount(52)
                .setPrescriberValidationDate("validationDate2")
                .setPrescriberId("prescriberId2")
                .setEquipmentKey("equipmentKey2")
                .setBasket(new BasketOrder().setId(42))
                .setReassort(false)
                .setOffers(new ArrayList<>())
                .setEquipmentTva5(24.5)
                .setEquipmentTva20(423.4)
                .setEquipmentPriceht(32.1)
                .setEquipmentName("equipmentName2")
                .setEquipmentImage("equipmentImage2")
                .setEquipmentPrice(245.5)
                .setEquipmentGrade("grade2")
                .setEquipmentEditor("editor2")
                .setEquipmentDiffusor("diffusor2")
                .setEquipmentCatalogueType("catalogueType2")
                .setCampaign(new Campaign().setId(72))
                .setIdStructure("idStructure2")
                .setComment("comment2")
                .setStatus(OrderStatus.ARCHIVED)
                .setProject(new ProjectModel().setId(84));

        List<OrderUniversalModel> orderList = Arrays.asList(orderUniversalModel1, orderUniversalModel2);

        String expectedQuery = "INSERT INTO null.\"order_client_equipment_old\" (id, amount, creation_date, user_id, status," +
                " equipment_key, equipment_name, equipment_image, equipment_price, equipment_grade, equipment_editor, equipment_diffusor," +
                " equipment_format, id_campaign, id_structure, comment, id_basket, reassort, offers, equipment_tva5, equipment_tva20," +
                " equipment_priceht) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)," +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String expectedParams = "[183,13,\"validationDate\",\"prescriberId\",\"SENT\",\"equipmentKey\",\"equipmentName\"," +
                "\"equipmentImage\",843.1,\"grade\",\"editor\",\"diffusor\",\"catalogueType\",156,\"idStructure\",\"comment\"," +
                "95,false,[],211.25,29.65,510.2,42,52,\"validationDate2\",\"prescriberId2\",\"ARCHIVED\",\"equipmentKey2\"," +
                "\"equipmentName2\",\"equipmentImage2\",245.5,\"grade2\",\"editor2\",\"diffusor2\",\"catalogueType2\",72," +
                "\"idStructure2\",\"comment2\",42,false,[],76.5,475.4,32.1]";

        TransactionElement transactionElement = Whitebox.invokeMethod(this.defaultOrderRegionService, "insertOldClientOrderList", orderList);
        ctx.assertEquals(expectedQuery, transactionElement.getQuery());
        ctx.assertEquals(expectedParams, transactionElement.getParams().toString());
    }
}