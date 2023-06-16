package fr.openent.crre.helpers;

import fr.openent.crre.model.FilterItemModel;
import fr.openent.crre.service.impl.DefaultBasketOrderItemService;
import io.vertx.core.Future;
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

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({ElasticSearchHelper.class}) //Prepare the static class you want to test
public class ElasticSearchHelperTest {

    @Before
    public void setup() {
        PowerMockito.spy(ElasticSearchHelper.class);
    }

    @Test
    public void prepareFilterESTest(TestContext ctx) throws Exception {
        Async async = ctx.async();
        FilterItemModel filters = new FilterItemModel(getJsonObject());
        String queryExpected = "{\"query\":{\"bool\":{\"filter\":[{\"terms\":{\"distributeur\":[\"distributor1\",\"distributor2\"]}}," +
                "{\"terms\":{\"publiccible\":[\"target1\",\"target2\"]}},{\"terms\":{\"editeur\":[\"editeur1\",\"editeur2\"]}}," +
                "{\"terms\":{\"_index\":[\"catalog1\",\"catalog2\"]}},{\"nested\":{\"path\":\"classes\",\"query\":{\"bool\":" +
                "{\"filter\":{\"terms\":{\"classes.libelle\":[\"classes1\",\"classes2\"]}}}}}}," +
                "{\"nested\":{\"path\":\"niveaux\",\"query\":{\"bool\":{\"filter\":{\"terms\":{\"niveaux.libelle\":[\"grade1\",\"grades2\"]}}}}}}," +
                "{\"nested\":{\"path\":\"disciplines\",\"query\":{\"bool\":{\"filter\":{\"terms\":{\"disciplines.libelle\":[\"disciplines1\",\"disciplines2\"]}}}}}}]," +
                "\"minimum_should_match\":1,\"should\":[{\"regexp\":{\"ean\":\".*word search.*\"}},{\"regexp\":{\"titre\":\".*word search.*\"}}," +
                "{\"regexp\":{\"editeur\":\".*word search.*\"}},{\"regexp\":{\"disciplines\":\".*word search.*\"}}," +
                "{\"regexp\":{\"niveaux\":\".*word search.*\"}},{\"regexp\":{\"auteur\":\".*word search.*\"}}," +
                "{\"regexp\":{\"ark\":\".*word search.*\"}}]}},\"from\":0,\"size\":100000,\"sort\":[{\"_index\":\"asc\"},{\"titre\":\"asc\"}]," +
                "\"_source\":[\"ID\"]}";
        String proExpected = "[true]";
        String consoExpected = "[false]";
        String ressourceExpected = "[false]";

        PowerMockito.doAnswer(invocation -> {
            JsonObject query = invocation.getArgument(0);
            JsonArray pro = invocation.getArgument(1);
            JsonArray conso = invocation.getArgument(2);
            JsonArray ressource = invocation.getArgument(3);

            ctx.assertEquals(query.toString(), queryExpected);
            ctx.assertEquals(pro.toString(), proExpected);
            ctx.assertEquals(conso.toString(), consoExpected);
            ctx.assertEquals(ressource.toString(), ressourceExpected);
            async.complete();

            return Future.succeededFuture();
        }).when(ElasticSearchHelper.class, "execute", Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        ElasticSearchHelper.searchfilter(filters, Arrays.asList("ID"));
        async.awaitSuccess(10000);
    }

    private JsonObject getJsonObject() {
        return new JsonObject("{\n" +
                "\t\t\"searchingText\": \"word search\",\n" +
                "\t\t\"disciplines\": [\"disciplines1\", \"disciplines2\"],\n" +
                "\t\t\"classes\": [\"classes1\", \"classes2\"],\n" +
                "\t\t\"grades\": [\"grade1\", \"grades2\"],\n" +
                "\t\t\"editors\": [\"editeur1\", \"editeur2\"],\n" +
                "\t\t\"distributors\": [\"distributor1\", \"distributor2\"],\n" +
                "\t\t\"catalogs\": [\"catalog1\", \"catalog2\"],\n" +
                "\t\t\"itemTypes\": [\"Manuel\"],\n" +
                "\t\t\"structureSectors\": [\"Lycée professionnel\"],\n" +
                "\t\t\"targets\": [\"target1\", \"target2\"]\n" +
                "\t}");
    }
}