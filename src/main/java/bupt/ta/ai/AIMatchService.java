package bupt.ta.ai;

import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered matching service - rule-based, explainable results.
 * Combines structured logic for skill matching, missing skills, and workload balancing.
 * No external AI API - deterministic and auditable.
 */
public class AIMatchService {

    /**
     * Result of skill matching between applicant and job.
     */
    public static class MatchResult {
        /** Match score from 0 to 100. */
        public final double score;
        /** Required skills the applicant appears to have. */
        public final List<String> matched;
        /** Required skills not found on the applicant profile. */
        public final List<String> missing;
        /** Human-readable summary of the score. */
        public final String explanation;

        public MatchResult(double score, List<String> matched, List<String> missing) {
            this.score = Math.min(100, Math.max(0, score));
            this.matched = matched != null ? matched : Collections.emptyList();
            this.missing = missing != null ? missing : Collections.emptyList();
            this.explanation = buildExplanation(score, matched, missing);
        }

        private static String buildExplanation(double score, List<String> matched, List<String> missing) {
            StringBuilder sb = new StringBuilder();
            if (matched != null && !matched.isEmpty()) {
                sb.append("Matched skills: ").append(String.join(", ", matched)).append(". ");
            }
            if (missing != null && !missing.isEmpty()) {
                sb.append("Missing: ").append(String.join(", ", missing)).append(". ");
            }
            sb.append(String.format("Match score: %.0f%%", score));
            if (missing != null && !missing.isEmpty()) {
                sb.append(" (consider improving missing skills)");
            }
            return sb.toString();
        }
    }

    /**
     * Normalize skill for comparison (case-insensitive, trim).
     */
    private static Set<String> normalizeSkills(Collection<String> skills) {
        if (skills == null || skills.isEmpty()) return Collections.emptySet();
        return skills.stream()
                .map(s -> s.trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Calculate skill match score between applicant profile and job.
     * Uses Jaccard-like similarity: matched / required.
     * Returns 0-100. Explainable: matched skills and missing skills listed.
     */
    /**
     * @param profile applicant profile (may be null)
     * @param job     job posting with required skills
     * @return scored match with matched and missing skill lists
     */
    public MatchResult matchSkills(TAProfile profile, Job job) {
        Set<String> jobSkills = normalizeSkills(job.getRequiredSkills());
        Set<String> applicantSkills = normalizeSkills(profile != null ? profile.getSkills() : null);

        if (jobSkills.isEmpty()) {
            return new MatchResult(100, new ArrayList<>(applicantSkills), Collections.emptyList());
        }

        List<String> matched = new ArrayList<>();
        for (String js : jobSkills) {
            boolean found = applicantSkills.stream().anyMatch(as -> as.contains(js) || js.contains(as));
            if (found) {
                matched.add(js);
            }
        }

        List<String> missing = new ArrayList<>();
        for (String js : jobSkills) {
            boolean found = applicantSkills.stream().anyMatch(as -> as.contains(js) || js.contains(as));
            if (!found) {
                missing.add(js);
            }
        }

        double score = jobSkills.isEmpty() ? 100 : (matched.size() * 100.0 / jobSkills.size());
        return new MatchResult(score, matched, missing);
    }

    /**
     * Identify missing skills for an applicant regarding a job.
     */
    public List<String> getMissingSkills(TAProfile profile, Job job) {
        return matchSkills(profile, job).missing;
    }

    /**
     * Workload info for a TA (number of selected jobs).
     */
    public static class WorkloadInfo {
        /** TA user id. */
        public final String taId;
        /** Display name. */
        public final String taName;
        /** Number of SELECTED applications. */
        public final int selectedCount;
        /** {@code true} when at or below the cohort average selected count. */
        public final boolean isBalanced;

        public WorkloadInfo(String taId, String taName, int selectedCount, boolean isBalanced) {
            this.taId = taId;
            this.taName = taName;
            this.selectedCount = selectedCount;
            this.isBalanced = isBalanced;
        }
    }

    /**
     * Calculate workload for each TA and determine balance recommendation.
     * Recommends TAs with lower workload for fairness.
     */
    /**
     * @param selectedApps applications to count (SELECTED status only)
     * @param taIdToName   map of applicant id to display name
     * @return workload rows sorted by ascending selected count
     */
    public List<WorkloadInfo> getWorkloadBalancedOrder(List<Application> selectedApps,
                                                       Map<String, String> taIdToName) {
        Map<String, Integer> workload = new HashMap<>();
        for (Application a : selectedApps) {
            if ("SELECTED".equals(a.getStatus())) {
                String id = a.getApplicantId();
                workload.merge(id, 1, Integer::sum);
            }
        }

        double avg = workload.isEmpty() ? 0 : workload.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        List<WorkloadInfo> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : workload.entrySet()) {
            result.add(new WorkloadInfo(e.getKey(), taIdToName.getOrDefault(e.getKey(), e.getKey()),
                    e.getValue(), e.getValue() <= avg));
        }
        result.sort(Comparator.comparingInt(w -> w.selectedCount)); // lower workload first
        return result;
    }

    /**
     * For MO: enrich applicants with match score, missing skills, and workload recommendation.
     */
    public static class ApplicantRecommendation {
        /** Application under review. */
        public final Application application;
        /** Applicant profile (may be null). */
        public final TAProfile profile;
        /** Rule-based skill match. */
        public final MatchResult matchResult;
        /** Count of jobs the applicant is already selected for. */
        public final int currentWorkload;
        /** {@code true} when workload is at or below average (prefer for balance). */
        public final boolean workloadBalanced;

        public ApplicantRecommendation(Application app, TAProfile profile, MatchResult match, int workload, boolean balanced) {
            this.application = app;
            this.profile = profile;
            this.matchResult = match;
            this.currentWorkload = workload;
            this.workloadBalanced = balanced;
        }
    }
}
