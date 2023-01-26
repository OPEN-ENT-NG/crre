package fr.openent.crre.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateHelper {
    public static final String MAIL_FORMAT = "ddMMyyyy-HHmmss";
    public static final String SQL_FORMAT = "yyyy-MM-dd";
    public static final String DAY_FORMAT = "dd/MM/yyyy";
    public static final String DAY_FORMAT_DASH = "dd-MM-yyyy";

    public static final String PARIS_TIMEZONE = "Europe/Paris";

    private DateHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String now(String format, String timezone) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));

        return simpleDateFormat.format(new Date());
    }

    public static String convertStringDateToOtherFormat(String stringDate, String actualFormat, String newFormat) {
        Date date;
        try {
            date = new SimpleDateFormat(actualFormat).parse(stringDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new SimpleDateFormat(newFormat).format(date);
    }
}
