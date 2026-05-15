package bupt.ta.util;

import bupt.ta.model.Application;
import bupt.ta.model.Job;

import java.util.List;
import java.util.Objects;

/**
 * Shared helpers for enforcing selected-TA capacity on a posting.
 */
public final class JobSelectionCapacity {

    private JobSelectionCapacity() {
    }

    /**
     * @param job job posting (may be null)
     * @return planned TA slots, at least 1
     */
    public static int selectionSlots(Job job) {
        if (job == null) {
            return 1;
        }
        return job.getTaSlots() > 0 ? job.getTaSlots() : 1;
    }

    /**
     * @param apps                  all applications (may be null)
     * @param jobId                 job to count for
     * @param excludeApplicationId  optional application id to omit (e.g. current selection)
     * @return number of SELECTED applications for the job
     */
    public static long selectedCount(List<Application> apps, String jobId, String excludeApplicationId) {
        if (apps == null || jobId == null || jobId.trim().isEmpty()) {
            return 0;
        }
        return apps.stream()
                .filter(app -> Objects.equals(jobId, app.getJobId()))
                .filter(app -> !Objects.equals(excludeApplicationId, app.getId()))
                .filter(app -> "SELECTED".equals(app.getStatus()))
                .count();
    }

    /**
     * @return {@code true} if fewer than {@link #selectionSlots(Job)} applicants are SELECTED
     */
    public static boolean hasVacancy(Job job, List<Application> apps, String excludeApplicationId) {
        return selectedCount(apps, job != null ? job.getId() : null, excludeApplicationId) < selectionSlots(job);
    }
}
