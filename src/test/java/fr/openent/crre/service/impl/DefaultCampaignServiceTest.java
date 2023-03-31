package fr.openent.crre.service.impl;

import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.Campaign;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.model.TransactionElement;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
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
public class DefaultCampaignServiceTest {

    private DefaultCampaignService defaultCampaignService;
    private Sql sql;

    @Before
    public void setup() {
        this.defaultCampaignService = PowerMockito.spy(new DefaultCampaignService(null, null));
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(TransactionHelper.class);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
    }

    @Test
    public void createTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        PowerMockito.spy(SqlHelper.class);
        PowerMockito.doReturn(Future.succeededFuture(35)).when(SqlHelper.class, "getNextVal", Mockito.any());

        String expectedQuery1 = "INSERT INTO crre.campaign(id, name, description, image, accessible, purse_enabled," +
                " priority_enabled, priority_field, start_date, end_date, automatic_close, reassort, catalog, use_credit," +
                " id_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING *";
        String expectedParams1 = "[35,\"name\",\"description\",\"image\",true,false,true,\"priorityField\",\"startDate\"," +
                "\"endDate\",true,false,\"catalog\",\"useCredit\",84]";
        String expectedQuery2 = "INSERT INTO null.rel_group_campaign(id_campaign, id_structure_group) VALUES (?, ?), (?, ?)";
        String expectedParams2 = "[35,5,35,8]";

        Campaign campaign = new Campaign()
                .setName("name")
                .setDescription("description")
                .setImage("image")
                .setAccessible(true)
                .setPurseEnabled(false)
                .setPriorityEnabled(true)
                .setPriorityField("priorityField")
                .setStartDate("startDate")
                .setEndDate("endDate")
                .setAutomaticClose(true)
                .setReassort(false)
                .setCatalog("catalog")
                .setUseCredit("useCredit")
                .setIdType(84);

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 2);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(statements.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(statements.get(1).getParams().toString(), expectedParams2);
            async.complete();
            return Future.succeededFuture(Arrays.asList(new TransactionElement(null, null).setResult(new JsonArray(Arrays.asList(campaign.toJson())))));
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());


        this.defaultCampaignService.create(campaign, Arrays.asList(new StructureGroupModel().setId(5), new StructureGroupModel().setId(8)));

        async.awaitSuccess(10000);
    }

    @Test
    public void updateTest(TestContext ctx) throws Exception {
        Async async = ctx.async();

        String expectedQuery1 = "UPDATE crre.campaign set name = ?, description = ?, image = ?, accessible = ?, purse_enabled = ?," +
                " priority_enabled = ?, priority_field = ?, start_date = ?, end_date = ?, automatic_close = ?, reassort = ?," +
                " catalog = ?, use_credit = ?, id_type = ? WHERE id = ? RETURNING *";
        String expectedParams1 = "[\"name\",\"description\",\"image\",true,false,true,\"priorityField\",\"startDate\"," +
                "\"endDate\",true,false,\"catalog\",\"useCredit\",84,7]";
        String expectedQuery2 = "DELETE FROM null.rel_group_campaign WHERE id_campaign = ?;";
        String expectedParams2 = "[7]";
        String expectedQuery3 = "INSERT INTO null.rel_group_campaign(id_campaign, id_structure_group) VALUES (?, ?), (?, ?)";
        String expectedParams3 = "[7,5,7,8]";

        Campaign campaign = new Campaign()
                .setId(7)
                .setName("name")
                .setDescription("description")
                .setImage("image")
                .setAccessible(true)
                .setPurseEnabled(false)
                .setPriorityEnabled(true)
                .setPriorityField("priorityField")
                .setStartDate("startDate")
                .setEndDate("endDate")
                .setAutomaticClose(true)
                .setReassort(false)
                .setCatalog("catalog")
                .setUseCredit("useCredit")
                .setIdType(84);

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 3);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(statements.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(statements.get(1).getParams().toString(), expectedParams2);
            ctx.assertEquals(statements.get(2).getQuery(), expectedQuery3);
            ctx.assertEquals(statements.get(2).getParams().toString(), expectedParams3);
            async.complete();
            return Future.succeededFuture(Arrays.asList(new TransactionElement(null, null).setResult(new JsonArray(Arrays.asList(campaign.toJson())))));
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());


        this.defaultCampaignService.update(campaign, Arrays.asList(new StructureGroupModel().setId(5), new StructureGroupModel().setId(8)));

        async.awaitSuccess(10000);
    }
}
