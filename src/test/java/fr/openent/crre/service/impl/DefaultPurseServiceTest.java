package fr.openent.crre.service.impl;

import fr.openent.crre.model.TransactionElement;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;

@RunWith(VertxUnitRunner.class)
public class DefaultPurseServiceTest {

    private DefaultPurseService defaultPurseService;

    @Before
    public void setup() {
        this.defaultPurseService = PowerMockito.spy(new DefaultPurseService());
    }

    @Test
    public void getTransactionUpdatePurseAmountTest(TestContext ctx) {
        TransactionElement transactionElement1 = this.defaultPurseService.getTransactionUpdatePurseAmount(80.5d, "idStructure", "+", true);
        TransactionElement transactionElement2 = this.defaultPurseService.getTransactionUpdatePurseAmount(805846.5d, "idStructure2", "*", false);
        String expectedQuery1 = "UPDATE  null.purse SET consumable_amount = ROUND((consumable_amount + ? )::numeric ,2)::double precision WHERE id_structure = ? ;";
        String expectedParams1 = "[80.5,\"idStructure\"]";
        String expectedQuery2 = "UPDATE  null.purse SET amount = ROUND((amount * ? )::numeric ,2)::double precision WHERE id_structure = ? ;";
        String expectedParams2 = "[805846.5,\"idStructure2\"]";
        ctx.assertEquals(transactionElement1.getQuery(), expectedQuery1);
        ctx.assertEquals(transactionElement1.getParams().toString(), expectedParams1);
        ctx.assertEquals(transactionElement2.getQuery(), expectedQuery2);
        ctx.assertEquals(transactionElement2.getParams().toString(), expectedParams2);
    }
}
