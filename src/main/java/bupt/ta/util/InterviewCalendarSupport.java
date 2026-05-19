package bupt.ta.util;

import bupt.ta.model.Application;
import bupt.ta.model.Job;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds simple iCalendar payloads for interview appointments.
 */
public final class InterviewCalendarSupport {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );
    /** Same calendar day, e.g. 2026-04-20 14:00-17:00 or with optional seconds. */
    private static final Pattern SAME_DAY_TIME_RANGE = Pattern.compile(
            "(\\d{4})-(\\d{1,2})-(\\d{1,2})[T ](\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\s*-\\s*(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    /** First yyyy-M-d with a clock time (embedded in longer free-text notices). */
    private static final Pattern FLEX_DATE_TIME = Pattern.compile(
            "\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})[T ](\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b");
    private static final DateTimeFormatter ICS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICS_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final int DEFAULT_DURATION_MINUTES = 45;

    private InterviewCalendarSupport() {
    }

    private static LocalDateTime ldtOrNull(int y, int mo, int d, int h, int mi, int sec) {
        try {
            return LocalDateTime.of(y, mo, d, h, mi, sec);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseIntGroup(Matcher m, int group) {
        return Integer.parseInt(m.group(group));
    }

    /**
     * Parsed window for an interview: {@code explicitEnd} is null unless a same-day time range was detected.
     */
    private static final class ParsedInterview {
        final LocalDateTime start;
        final LocalDateTime explicitEnd;

        ParsedInterview(LocalDateTime start, LocalDateTime explicitEnd) {
            this.start = start;
            this.explicitEnd = explicitEnd;
        }
    }

    private static ParsedInterview parseInterview(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();

        Matcher range = SAME_DAY_TIME_RANGE.matcher(value);
        if (range.find()) {
            int y = parseIntGroup(range, 1);
            int mo = parseIntGroup(range, 2);
            int d = parseIntGroup(range, 3);
            int h1 = parseIntGroup(range, 4);
            int mi1 = parseIntGroup(range, 5);
            int s1 = range.group(6) != null ? parseIntGroup(range, 6) : 0;
            int h2 = parseIntGroup(range, 7);
            int mi2 = parseIntGroup(range, 8);
            int s2 = range.group(9) != null ? parseIntGroup(range, 9) : 0;
            LocalDateTime start = ldtOrNull(y, mo, d, h1, mi1, s1);
            LocalDateTime end = ldtOrNull(y, mo, d, h2, mi2, s2);
            if (start != null && end != null && end.isAfter(start)) {
                return new ParsedInterview(start, end);
            }
            if (start != null) {
                return new ParsedInterview(start, null);
            }
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(value, formatter);
                return new ParsedInterview(parsed, null);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted format.
            }
        }

        Matcher flex = FLEX_DATE_TIME.matcher(value);
        if (flex.find()) {
            int y = parseIntGroup(flex, 1);
            int mo = parseIntGroup(flex, 2);
            int d = parseIntGroup(flex, 3);
            int h = parseIntGroup(flex, 4);
            int mi = parseIntGroup(flex, 5);
            int s = flex.group(6) != null ? parseIntGroup(flex, 6) : 0;
            LocalDateTime start = ldtOrNull(y, mo, d, h, mi, s);
            if (start != null) {
                return new ParsedInterview(start, null);
            }
        }
        return null;
    }

    /**
     * @param raw interview time text from an application notice
     * @return parsed start time, or {@code null} when unparseable
     */
    public static LocalDateTime parseInterviewTime(String raw) {
        ParsedInterview p = parseInterview(raw);
        return p != null ? p.start : null;
    }

    /**
     * @param application application carrying {@link bupt.ta.model.Application#getInterviewTime()}
     * @param job         related job (for summary text)
     * @return ICS calendar document bytes as a UTF-8 string
     * @throws IllegalArgumentException when interview time cannot be parsed
     */
    public static String buildCalendarFile(Application application, Job job) {
        ParsedInterview window = parseInterview(application != null ? application.getInterviewTime() : null);
        if (application == null || window == null || window.start == null) {
            throw new IllegalArgumentException(
                    "Interview time could not be parsed. Use yyyy-MM-dd HH:mm (or include that date-time in the notice text). "
                            + "Optional same-day range: yyyy-MM-dd HH:mm-HH:mm.");
        }

        LocalDateTime start = window.start;
        LocalDateTime end = window.explicitEnd != null && window.explicitEnd.isAfter(start)
                ? window.explicitEnd
                : start.plusMinutes(DEFAULT_DURATION_MINUTES);
        String jobTitle = job != null && job.getTitle() != null && !job.getTitle().trim().isEmpty()
                ? job.getTitle().trim()
                : "TA interview";
        String moduleCode = job != null && job.getModuleCode() != null ? job.getModuleCode().trim() : "";
        String summary = moduleCode.isEmpty()
                ? "Interview - " + jobTitle
                : "Interview - " + jobTitle + " (" + moduleCode + ")";

        StringBuilder description = new StringBuilder();
        description.append("Teaching Assistant recruitment interview");
        if (!moduleCode.isEmpty()) {
            description.append("\\nModule: ").append(escape(moduleCode));
        }
        if (job != null && job.getModuleName() != null && !job.getModuleName().trim().isEmpty()) {
            description.append("\\nModule name: ").append(escape(job.getModuleName().trim()));
        }
        if (application.getInterviewAssessment() != null && !application.getInterviewAssessment().trim().isEmpty()) {
            description.append("\\nAssessment: ").append(escape(application.getInterviewAssessment().trim()));
        }
        description.append("\\nApplication ID: ").append(escape(application.getId() != null ? application.getId() : "unknown"));

        String location = application.getInterviewLocation() != null ? application.getInterviewLocation().trim() : "";
        String uid = (application.getId() != null ? application.getId() : "interview")
                + "@bupt-ta-recruitment";

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//BUPT TA Recruitment//Interview Calendar//EN\r\n");
        ics.append("CALSCALE:GREGORIAN\r\n");
        ics.append("METHOD:PUBLISH\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(escape(uid)).append("\r\n");
        ics.append("DTSTAMP:").append(LocalDateTime.now(ZoneOffset.UTC).format(ICS_UTC)).append("\r\n");
        ics.append("DTSTART:").append(start.format(ICS_LOCAL)).append("\r\n");
        ics.append("DTEND:").append(end.format(ICS_LOCAL)).append("\r\n");
        ics.append("SUMMARY:").append(escape(summary)).append("\r\n");
        if (!location.isEmpty()) {
            ics.append("LOCATION:").append(escape(location)).append("\r\n");
        }
        ics.append("DESCRIPTION:").append(description).append("\r\n");
        ics.append("STATUS:CONFIRMED\r\n");
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");
        return ics.toString();
    }

    /**
     * @param application application used for a unique suffix
     * @param job         job posting (module code or title preferred)
     * @return safe {@code .ics} download filename
     */
    public static String buildFilename(Application application, Job job) {
        String base = job != null && job.getModuleCode() != null && !job.getModuleCode().trim().isEmpty()
                ? job.getModuleCode().trim()
                : (job != null && job.getTitle() != null ? job.getTitle().trim() : "interview");
        String normalized = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (normalized.isEmpty()) {
            normalized = "interview";
        }
        String appId = application != null && application.getId() != null ? application.getId().trim() : "event";
        return normalized + "-" + appId + ".ics";
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }
}
