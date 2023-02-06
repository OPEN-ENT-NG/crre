package fr.openent.crre.service.impl;

import fr.openent.crre.helpers.SqlHelper;
import fr.openent.crre.helpers.TransactionHelper;
import fr.openent.crre.model.StructureGroupModel;
import fr.openent.crre.model.TransactionElement;
import fr.wseduc.webutils.eventbus.ResultMessage;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
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
@PrepareForTest({Sql.class, DefaultStructureGroupService.class, TransactionHelper.class, SqlHelper.class}) //Prepare the static class you want to test
public class DefaultStructureGroupServiceTest {

    private DefaultStructureGroupService defaultStructureGroupService;
    private Sql sql;

    @Before
    public void setup() {
        this.defaultStructureGroupService = PowerMockito.spy(new DefaultStructureGroupService(null));
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(DefaultStructureGroupService.class);
        PowerMockito.spy(TransactionHelper.class);
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
    }

    @Test
    public void listStructureGroupsTest(TestContext ctx) {
        Async async = ctx.async();

        StructureGroupModel structureGroupModel1 = new StructureGroupModel()
                .setId(1)
                .setName("name1")
                .setDescription("description1")
                .setStructures(Arrays.asList("idStructure1", "idStructure2"));
        StructureGroupModel structureGroupModel2 = new StructureGroupModel()
                .setId(2)
                .setName("name2")
                .setDescription("description2")
                .setStructures(Arrays.asList("idStructure3", "idStructure4"));

        JsonObject result = new JsonObject()
                .put("fields", new JsonArray("[\"id\",\"name\",\"description\",\"structures\"]"))
                .put("results", new JsonArray(Arrays.asList(
                        new JsonArray(Arrays.asList(1, "name1", "description1", new JsonArray().add("idStructure1").add("idStructure2"))),
                        new JsonArray(Arrays.asList(2, "name2", "description2", new JsonArray().add("idStructure3").add("idStructure4")))
                )));
        Message<JsonObject> message = new ResultMessage(result);

        String expectedQuery = "SELECT id, name, description, array_to_json(array_agg(id_structure)) as structures " +
                "FROM null.structure_group INNER JOIN null.rel_group_structure on structure_group.id = rel_group_structure.id_structure_group group by (id, name , description ) " +
                "ORDER BY id;";
        String expectedParams = "[]";

        Mockito.doAnswer(invocation -> {
            String query = invocation.getArgument(0);
            JsonArray values = invocation.getArgument(1);
            Handler<Message<JsonObject>> handler = invocation.getArgument(2);
            ctx.assertEquals(expectedQuery, query);
            ctx.assertEquals(expectedParams, values.toString());
            handler.handle(message);
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any());
        this.defaultStructureGroupService.listStructureGroups()
                .onSuccess(res -> {
                    ctx.assertEquals(res.size(), 2);
                    ctx.assertEquals(res.get(0).toJson(), structureGroupModel1.toJson());
                    ctx.assertEquals(res.get(1).toJson(), structureGroupModel2.toJson());
                    async.complete();
                });

        async.awaitSuccess(10000);
    }

    @Test
    public void createTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        StructureGroupModel structureGroupModel = new StructureGroupModel().setId(10).setName("name").setDescription("description")
                .setStructures(Arrays.asList("idStructure1", "idStructure2"));
        String expectedQuery1 = "INSERT INTO null.structure_group(id, name, description) VALUES (?,?,?) RETURNING id;";
        String expectedParams1 = "[10,\"name\",\"description\"]";
        String expectedQuery2 = "INSERT INTO null.rel_group_structure(id_structure,id_structure_group) VALUES (?,?),(?,?) ON CONFLICT DO NOTHING RETURNING id_structure;";
        String expectedParams2 = "[\"idStructure1\",10,\"idStructure2\",10]";

        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList("structure1"))).when(this.defaultStructureGroupService, "getOldIdStructureList");
        PowerMockito.spy(SqlHelper.class);
        PowerMockito.doReturn(Future.succeededFuture(10)).when(SqlHelper.class, "getNextVal", Mockito.any());

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 2);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(statements.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(statements.get(1).getParams().toString(), expectedParams2);
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        PowerMockito.doReturn(Arrays.asList("idStructure3")).when(this.defaultStructureGroupService, "getNewIdStructure", Mockito.any(), Mockito.any());

        PowerMockito.doAnswer(invocation -> {
            List<String> newIds = invocation.getArgument(0);
            ctx.assertEquals(newIds.size(), 1);
            ctx.assertEquals(newIds.get(0), "idStructure3");
            async.complete();
            return null;
        }).when(this.defaultStructureGroupService, "getStudentsByStructures", Mockito.any());

        this.defaultStructureGroupService.create(structureGroupModel);

        async.awaitSuccess(10000);
    }

    @Test
    public void updateTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        StructureGroupModel structureGroupModel = new StructureGroupModel().setId(10).setName("name").setDescription("description")
                .setStructures(Arrays.asList("idStructure1", "idStructure2"));
        String expectedQuery1 = "UPDATE null.structure_group SET name = ?, description = ? WHERE id = ?;";
        String expectedParams1 = "[\"name\",\"description\",10]";
        String expectedQuery2 = "DELETE FROM null.rel_group_structure WHERE id_structure_group = ?;";
        String expectedParams2 = "[10]";
        String expectedQuery3 = "INSERT INTO null.rel_group_structure(id_structure,id_structure_group) VALUES (?,?),(?,?) ON CONFLICT DO NOTHING RETURNING id_structure;";
        String expectedParams3 = "[\"idStructure1\",10,\"idStructure2\",10]";

        PowerMockito.doReturn(Future.succeededFuture(Arrays.asList("structure1"))).when(this.defaultStructureGroupService, "getOldIdStructureList");

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 3);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(statements.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(statements.get(1).getParams().toString(), expectedParams2);
            ctx.assertEquals(statements.get(2).getQuery(), expectedQuery3);
            ctx.assertEquals(statements.get(2).getParams().toString(), expectedParams3);
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        PowerMockito.doReturn(Arrays.asList("idStructure3")).when(this.defaultStructureGroupService, "getNewIdStructure", Mockito.any(), Mockito.any());

        PowerMockito.doAnswer(invocation -> {
            List<String> newIds = invocation.getArgument(0);
            ctx.assertEquals(newIds.size(), 1);
            ctx.assertEquals(newIds.get(0), "idStructure3");
            async.complete();
            return null;
        }).when(this.defaultStructureGroupService, "getStudentsByStructures", Mockito.any());

        this.defaultStructureGroupService.update(10, structureGroupModel);

        async.awaitSuccess(10000);
    }

    @Test
    public void deleteTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        String expectedQuery1 = "DELETE FROM null.rel_group_structure WHERE id_structure_group IN (?,?)";
        String expectedParams1 = "[10,11]";
        String expectedQuery2 = "DELETE FROM null.structure_group WHERE id IN (?,?)";
        String expectedParams2 = "[10,11]";
        String expectedResult = "{\"id\":[10,11]}";

        PowerMockito.doAnswer(invocation -> {
            List<TransactionElement> statements = invocation.getArgument(0);
            ctx.assertEquals(statements.size(), 2);
            ctx.assertEquals(statements.get(0).getQuery(), expectedQuery1);
            ctx.assertEquals(statements.get(0).getParams().toString(), expectedParams1);
            ctx.assertEquals(statements.get(1).getQuery(), expectedQuery2);
            ctx.assertEquals(statements.get(1).getParams().toString(), expectedParams2);
            return Future.succeededFuture();
        }).when(TransactionHelper.class, "executeTransaction", Mockito.any(), Mockito.any());

        this.defaultStructureGroupService.delete(Arrays.asList(10, 11))
                .onSuccess(res -> {
                    ctx.assertEquals(res.toString(), expectedResult);
                    async.complete();
                });

        async.awaitSuccess(10000);
    }
}
