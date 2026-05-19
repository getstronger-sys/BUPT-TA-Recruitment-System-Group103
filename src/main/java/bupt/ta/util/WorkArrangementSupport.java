package bupt.ta.util;

import bupt.ta.model.Job;
import bupt.ta.model.WorkArrangementItem;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds derived Job text fields from structured work arrangements and recalculates planned recruits.
 */
public final class WorkArrangementSupport {

    private static final Pattern HOURS_PATTERN = Pattern.compile(
            "^([0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:\\s*(?:h|hr|hrs|hour|hours))?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MINUTES_PATTERN = Pattern.compile(
            "^([0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)\\s*(?:min|mins|minute|minutes)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$");
    private static final Pattern SPECIFIC_TIME_PATTERN = Pattern.compile(
            "\\b(mon(?:day)?|tue(?:sday)?|tues|wed(?:nesday)?|thu(?:rsday)?|thur|thurs|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\b\\s+([0-2]?\\d:[0-5]\\d)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final String[] WEEKDAYS = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private WorkArrangementSupport() {
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * Parses work-arrangement rows from the MO posting / edit form (same field names as post-job.jsp).
     *
     * @param req servlet request with {@code wa*} parameters
     * @return parsed rows (may be empty)
     */
    public static List<WorkArrangementItem> parseWorkRowsFromRequest(HttpServletRequest req) {
        String[] names = req.getParameterValues("waWorkName");
        if (names == null || names.length == 0) {
            return new ArrayList<>();
        }
        String[] sessionDurs = req.getParameterValues("waSessionDuration");
        String[] occCounts = req.getParameterValues("waOccurrenceCount");
        String[] taCounts = req.getParameterValues("waTaCount");
        String[] legacyTimes = req.getParameterValues("waSpecificTime");
        String[] days = req.getParameterValues("waSpecificDay");
        String[] startTimes = req.getParameterValues("waSpecificStartTime");
        boolean usesStructuredSpecificTime = days != null || startTimes != null;

        List<WorkArrangementItem> out = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String n = trim(names[i]);
            String sd = sessionDurs != null && i < sessionDurs.length ? trim(sessionDurs[i]) : "";
            int oc = parseIntAt(occCounts, i);
            int tc = parseIntAt(taCounts, i);

            String t;
            if (usesStructuredSpecificTime) {
                String day = days != null && i < days.length ? trim(days[i]) : "";
                String startTime = startTimes != null && i < startTimes.length ? trim(startTimes[i]) : "";
                t = canonicalSpecificTime(day, startTime);
            } else {
                t = legacyTimes != null && i < legacyTimes.length ? trim(legacyTimes[i]) : "";
            }

            String normalizedDuration = normalizeDurationHoursForStorage(sd);
            if (!normalizedDuration.isEmpty()) {
                sd = normalizedDuration;
            }

            if (n.isEmpty() && sd.isEmpty() && oc <= 0 && tc <= 0 && t.isEmpty()) {
                continue;
            }
            WorkArrangementItem it = new WorkArrangementItem();
            it.setWorkName(n);
            it.setSessionDuration(sd);
            it.setOccurrenceCount(oc);
            it.setTaCount(tc);
            it.setSpecificTime(t);
            out.add(it);
        }
        return out;
    }

    private static int parseIntAt(String[] values, int index) {
        if (values == null || index >= values.length) {
            return 0;
        }
        try {
            return Integer.parseInt(trim(values[index]));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * @param items rows to validate (mutates duration fields when normalising)
     * @return {@code null} if valid, otherwise a short English message for the MO
     */
    public static String validateWorkRowsForPosting(List<WorkArrangementItem> items) {
        if (items.isEmpty()) {
            return "Add at least one work arrangement row (work name, per-session duration, occurrences, and TA count are required).";
        }
        for (int i = 0; i < items.size(); i++) {
            WorkArrangementItem it = items.get(i);
            if (trim(it.getWorkName()).isEmpty()) {
                return "Work arrangement row " + (i + 1) + ": work name is required.";
            }
            if (trim(it.getResolvedSessionDuration()).isEmpty()) {
                return "Work arrangement row " + (i + 1) + ": per-session duration is required.";
            }
            String normalizedDuration = normalizeDurationHoursForStorage(it.getResolvedSessionDuration());
            if (normalizedDuration.isEmpty()) {
                return "Work arrangement row " + (i + 1) + ": per-session duration must be a positive number of hours.";
            }
            it.setSessionDuration(normalizedDuration);
            if (it.getOccurrenceCount() < 1) {
                return "Work arrangement row " + (i + 1) + ": number of occurrences must be at least 1.";
            }
            if (it.getTaCount() < 1) {
                return "Work arrangement row " + (i + 1) + ": TA count must be at least 1.";
            }

            String specificTime = trim(it.getSpecificTime());
            if (!specificTime.isEmpty()) {
                ParsedSpecificTime parsed = parseSpecificTime(specificTime);
                if (parsed == null) {
                    return "Work arrangement row " + (i + 1) + ": specific time must use weekday and 24-hour time, e.g. Wednesday 14:00.";
                }
                it.setSpecificTime(parsed.day + " " + parsed.time);
            }
        }
        return null;
    }

    /**
     * Sets {@link Job#setTaSlots}, {@link Job#setWorkArrangements}, workingHours, workload, taAllocationPlan from items.
     *
     * @param job   job to update in place
     * @param items validated work arrangement rows
     */
    public static void applyDerivedFields(Job job, List<WorkArrangementItem> items) {
        int sum = items.stream().mapToInt(WorkArrangementItem::getTaCount).sum();
        job.setTaSlots(Math.max(1, sum));
        job.setWorkArrangements(new ArrayList<>(items));
        StringBuilder wh = new StringBuilder();
        StringBuilder wl = new StringBuilder();
        StringBuilder plan = new StringBuilder();
        for (WorkArrangementItem it : items) {
            String timeLine = trim(it.getSpecificTime());
            if (timeLine.isEmpty()) {
                timeLine = "TBD - to be scheduled based on operational needs";
            }
            if (wh.length() > 0) {
                wh.append('\n');
            }
            wh.append(it.getWorkName()).append(": ").append(timeLine);
            if (wl.length() > 0) {
                wl.append("; ");
            }
            wl.append(it.getWorkName()).append(" ")
                    .append(it.getResolvedSessionDuration())
                    .append(" x ")
                    .append(it.getResolvedOccurrenceCount())
                    .append(" occurrence(s)");
            if (plan.length() > 0) {
                plan.append('\n');
            }
            plan.append("- ").append(it.getWorkName())
                    .append(" | per session: ").append(it.getResolvedSessionDuration())
                    .append(" | occurrences: ").append(it.getResolvedOccurrenceCount())
                    .append(" | TAs: ").append(it.getTaCount());
            if (!trim(it.getSpecificTime()).isEmpty()) {
                plan.append(" | time: ").append(it.getSpecificTime());
            } else {
                plan.append(" | time: TBD (to be arranged as needed)");
            }
        }
        job.setWorkingHours(wh.toString());
        job.setWorkload(wl.toString());
        job.setTaAllocationPlan(plan.toString());
    }

    /**
     * @param item work arrangement row
     * @return hours field for form repopulation
     */
    public static String durationHoursInputValue(WorkArrangementItem item) {
        if (item == null) {
            return "";
        }
        return durationHoursInputValue(item.getResolvedSessionDuration());
    }

    /**
     * @param raw stored duration text
     * @return decimal hours for the form, or empty when unparseable
     */
    public static String durationHoursInputValue(String raw) {
        Double hours = parseDurationHours(raw);
        return hours == null ? "" : formatDecimal(hours);
    }

    /**
     * @param raw combined specific time text (e.g. {@code Wednesday 14:00})
     * @return weekday portion for the form, or empty
     */
    public static String specificDayInputValue(String raw) {
        ParsedSpecificTime parsed = parseSpecificTime(raw);
        return parsed == null ? "" : parsed.day;
    }

    /**
     * @param raw combined specific time text
     * @return time portion (HH:mm) for the form, or empty
     */
    public static String specificTimeInputValue(String raw) {
        ParsedSpecificTime parsed = parseSpecificTime(raw);
        return parsed == null ? "" : parsed.time;
    }

    /**
     * @return weekday labels for the MO work-arrangement form
     */
    public static String[] weekdays() {
        return WEEKDAYS.clone();
    }

    static String normalizeDurationHoursForStorage(String raw) {
        Double hours = parseDurationHours(raw);
        if (hours == null) {
            return "";
        }
        String formatted = formatDecimal(hours);
        return formatted + ("1".equals(formatted) ? " hour" : " hours");
    }

    private static Double parseDurationHours(String raw) {
        String text = trim(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return null;
        }

        Matcher minutes = MINUTES_PATTERN.matcher(text);
        if (minutes.matches()) {
            Double minutesValue = parsePositiveDouble(minutes.group(1));
            return minutesValue == null ? null : minutesValue / 60.0;
        }

        Matcher hours = HOURS_PATTERN.matcher(text);
        if (!hours.matches()) {
            return null;
        }
        return parsePositiveDouble(hours.group(1));
    }

    private static Double parsePositiveDouble(String raw) {
        try {
            double value = Double.parseDouble(raw);
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String canonicalSpecificTime(String dayRaw, String timeRaw) {
        String day = canonicalDay(dayRaw);
        String time = canonicalTime(timeRaw);
        if (trim(dayRaw).isEmpty() && trim(timeRaw).isEmpty()) {
            return "";
        }
        if (day == null || time == null) {
            return (trim(dayRaw) + " " + trim(timeRaw)).trim();
        }
        return day + " " + time;
    }

    private static ParsedSpecificTime parseSpecificTime(String raw) {
        String text = trim(raw);
        if (text.isEmpty()) {
            return null;
        }
        Matcher matcher = SPECIFIC_TIME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String day = canonicalDay(matcher.group(1));
        String time = canonicalTime(matcher.group(2));
        if (day == null || time == null) {
            return null;
        }
        return new ParsedSpecificTime(day, time);
    }

    private static String canonicalDay(String raw) {
        String value = trim(raw).toLowerCase(Locale.ROOT).replace(".", "");
        switch (value) {
            case "mon":
            case "monday":
                return "Monday";
            case "tue":
            case "tues":
            case "tuesday":
                return "Tuesday";
            case "wed":
            case "wednesday":
                return "Wednesday";
            case "thu":
            case "thur":
            case "thurs":
            case "thursday":
                return "Thursday";
            case "fri":
            case "friday":
                return "Friday";
            case "sat":
            case "saturday":
                return "Saturday";
            case "sun":
            case "sunday":
                return "Sunday";
            default:
                return null;
        }
    }

    private static String canonicalTime(String raw) {
        Matcher matcher = TIME_PATTERN.matcher(trim(raw));
        if (!matcher.matches()) {
            return null;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private static final class ParsedSpecificTime {
        private final String day;
        private final String time;

        private ParsedSpecificTime(String day, String time) {
            this.day = day;
            this.time = time;
        }
    }
}
