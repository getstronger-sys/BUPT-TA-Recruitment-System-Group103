package bupt.ta.util;

import bupt.ta.model.Job;

/**
 * Estimates expected hours per selected TA for a job from structured work arrangements
 * (same basis as MO work-quota planning).
 */
public final class JobWorkloadEstimator {

    private JobWorkloadEstimator() {
    }

    /**
     * Average hours per recruited TA for this posting, from {@link WorkQuotaPlanner}.
     * Returns 0 when there are no valid arrangement rows.
     */
    /**
     * @param job job posting with work arrangements
     * @return average hours per recruited TA, or 0 when arrangements are empty
     */
    public static double estimatedHoursPerSelectedTa(Job job) {
        if (job == null) {
            return 0.0;
        }
        int slots = job.getTaSlots() > 0 ? job.getTaSlots() : 1;
        return WorkQuotaPlanner.recommend(job.getWorkArrangements(), slots).getAverageHours();
    }
}
