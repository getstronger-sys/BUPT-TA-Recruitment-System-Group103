package bupt.ta.util;

import bupt.ta.model.Job;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Whether a job is still "active" for MO day-to-day management (My Jobs).
 * Inactive = manually closed or application deadline has passed (date only, yyyy-MM-dd).
 */
public final class JobActivity {

    public static final String PATH_ACTIVE = "/mo/jobs";
    public static final String PATH_INACTIVE = "/mo/past-jobs";

    private JobActivity() {}

    /**
     * @param job job posting
     * @return {@code true} when closed or past the application deadline
     */
    public static boolean isInactive(Job job) {
        if (job == null) return true;
        if ("CLOSED".equalsIgnoreCase(job.getStatus())) return true;
        return isApplicationDeadlinePast(job.getDeadline());
    }

    /**
     * @param job job posting
     * @return {@code true} when the job is still open and before deadline
     */
    public static boolean isActive(Job job) {
        return !isInactive(job);
    }

    /**
     * @param job job posting
     * @return {@link #PATH_ACTIVE} or {@link #PATH_INACTIVE} for MO navigation
     */
    public static String listPathFor(Job job) {
        return isInactive(job) ? PATH_INACTIVE : PATH_ACTIVE;
    }

    /**
     * True if the application deadline date (yyyy-MM-dd prefix) is strictly before today.
     * Missing or unparseable deadlines are not treated as past.
     */
    public static boolean isApplicationDeadlinePast(String deadline) {
        if (deadline == null || deadline.trim().isEmpty()) return false;
        String d = deadline.trim();
        if (d.length() >= 10) {
            d = d.substring(0, 10);
        }
        try {
            LocalDate end = LocalDate.parse(d);
            return end.isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * If the job is still OPEN but the deadline has passed, set status to CLOSED (call before persisting).
     *
     * @return true if the in-memory job was modified
     */
    public static boolean closeOpenJobIfDeadlinePassed(Job job) {
        if (job == null) return false;
        if (!"OPEN".equalsIgnoreCase(job.getStatus())) return false;
        if (!isApplicationDeadlinePast(job.getDeadline())) return false;
        job.setStatus("CLOSED");
        return true;
    }
}
