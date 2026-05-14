package bupt.ta.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes the MO posting form's estimated interview time.
 */
public final class InterviewScheduleSupport {
    private static final Pattern STRUCTURED_SCHEDULE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2})(?:\\s*-\\s*(\\d{2}:\\d{2}))?.*$");

    private InterviewScheduleSupport() {
    }

    public static String normalizeFromForm(String dateRaw, String startRaw, String endRaw) {
        String date = trim(dateRaw);
        String start = trim(startRaw);
        String end = trim(endRaw);

        if (date.isEmpty() || start.isEmpty() || end.isEmpty()) {
            throw new IllegalArgumentException("Estimated interview time requires date, start time, and end time.");
        }

        LocalDate parsedDate;
        LocalTime parsedStart;
        LocalTime parsedEnd;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Estimated interview date must be a valid date.");
        }
        try {
            parsedStart = LocalTime.parse(start);
            parsedEnd = LocalTime.parse(end);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Estimated interview start and end must be valid times.");
        }

        if (!parsedEnd.isAfter(parsedStart)) {
            throw new IllegalArgumentException("Estimated interview end time must be later than start time.");
        }

        return parsedDate + " " + parsedStart + "-" + parsedEnd;
    }

    public static String dateInputValue(String raw) {
        Matcher m = matcher(raw);
        return m != null ? m.group(1) : "";
    }

    public static String startTimeInputValue(String raw) {
        Matcher m = matcher(raw);
        return m != null ? m.group(2) : "";
    }

    public static String endTimeInputValue(String raw) {
        Matcher m = matcher(raw);
        return m != null && m.group(3) != null ? m.group(3) : "";
    }

    private static Matcher matcher(String raw) {
        String value = trim(raw);
        if (value.isEmpty()) {
            return null;
        }
        Matcher m = STRUCTURED_SCHEDULE.matcher(value);
        return m.matches() ? m : null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
