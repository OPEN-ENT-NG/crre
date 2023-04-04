package fr.openent.crre.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DateHelper {
    public static final String MAIL_FORMAT = "ddMMyyyy-HHmmss";
    public static final String SQL_FORMAT = "yyyy-MM-dd";
    public static final String DAY_FORMAT = "dd/MM/yyyy";
    public static final String DAY_FORMAT_DASH = "dd-MM-yyyy";
    public static final String SQL_FULL_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSSZ";

    private static final List<String> DATE_FORMATS = Arrays.asList(
            SQL_FULL_FORMAT, DAY_FORMAT, SQL_FORMAT, MAIL_FORMAT, DAY_FORMAT_DASH
    );

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

    public static String formatDate(String inputDateStr) {
        for (String dateFormat : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(inputDateStr, DateTimeFormatter.ofPattern(dateFormat));
                return DateTimeFormatter.ofPattern(DAY_FORMAT).format(date);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        // No format working => is already in DAY Format
        return inputDateStr;
    }

}
