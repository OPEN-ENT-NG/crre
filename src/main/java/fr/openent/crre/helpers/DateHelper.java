package fr.openent.crre.helpers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateHelper {
    public static final String MAIL_FORMAT = "ddMMyyyy-HHmmss";

    public static final String PARIS_TIMEZONE = "Europe/Paris";

    private DateHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String now(String format, String timezone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        return simpleDateFormat.format(new Date());
    }
}
