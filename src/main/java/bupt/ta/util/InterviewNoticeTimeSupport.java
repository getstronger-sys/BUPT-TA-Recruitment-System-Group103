package bupt.ta.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Normalizes the MO interview notice time sent to shortlisted applicants.
 */
public final class InterviewNoticeTimeSupport {
    private InterviewNoticeTimeSupport() {
    }

    public static String normalizeFromForm(String dateRaw, String timeRaw) {
        String date = trim(dateRaw);
        String time = trim(timeRaw);
        if (date.isEmpty() || time.isEmpty()) {
            throw new IllegalArgumentException("Interview notice requires both date and time.");
        }

        LocalDate parsedDate;
        LocalTime parsedTime;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Interview notice date must be a valid date.");
        }
        try {
            parsedTime = LocalTime.parse(time);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Interview notice time must be a valid time.");
        }
        return parsedDate + " " + parsedTime;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
