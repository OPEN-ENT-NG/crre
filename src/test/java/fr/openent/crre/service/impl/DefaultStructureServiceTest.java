package fr.openent.crre.service.impl;

import fr.openent.crre.model.TransactionElement;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;

@RunWith(VertxUnitRunner.class)
public class DefaultStructureServiceTest {

    private DefaultStructureService defaultStructureService;

    @Before
    public void setup() {
        this.defaultStructureService = PowerMockito.spy(new DefaultStructureService(null, null));
    }

    @Test
    public void getTransactionUpdateAmountLicenceTest(TestContext ctx) {
        TransactionElement transactionElement1 = this.defaultStructureService.getTransactionUpdateAmountLicence("structureId", "+", 8413, true);
        TransactionElement transactionElement2 = this.defaultStructureService.getTransactionUpdateAmountLicence("structureId2", "/", 6461358, false);
        String expectedQuery1 = "UPDATE  null.licences SET consumable_amount = consumable_amount + ?  WHERE id_structure = ? ;";
        String expectedParams1 = "[8413,\"structureId\"]";
        String expectedQuery2 = "UPDATE  null.licences SET amount = amount / ?  WHERE id_structure = ? ;";
        String expectedParams2 = "[6461358,\"structureId2\"]";
        ctx.assertEquals(transactionElement1.getQuery(), expectedQuery1);
        ctx.assertEquals(transactionElement1.getParams().toString(), expectedParams1);
        ctx.assertEquals(transactionElement2.getQuery(), expectedQuery2);
        ctx.assertEquals(transactionElement2.getParams().toString(), expectedParams2);
    }
}
