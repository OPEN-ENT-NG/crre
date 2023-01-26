package fr.openent.crre.helpers;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DateHelperTest {

    @Test
    public void convertStringDateToOtherFormatTest(TestContext ctx) {
        ctx.assertEquals(DateHelper.convertStringDateToOtherFormat("30/04/2000", DateHelper.DAY_FORMAT, DateHelper.SQL_FORMAT), "2000-04-30");
        ctx.assertEquals(DateHelper.convertStringDateToOtherFormat("2000-03-19", DateHelper.SQL_FORMAT, DateHelper.MAIL_FORMAT), "19032000-000000");
        ctx.assertEquals(DateHelper.convertStringDateToOtherFormat("18062010-105030", DateHelper.MAIL_FORMAT, DateHelper.DAY_FORMAT), "18/06/2010");
    }
}
