package bupt.ta.llm;

import bupt.ta.ai.AIMatchService;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds short MO-facing screening summaries for one application.
 */
public final class LlmApplicantSummaryService {

    private final DeepSeekClient client;

    /** Uses environment-driven {@link DeepSeekClient} configuration. */
    public LlmApplicantSummaryService() {
        this(new DeepSeekClient());
    }

    /**
     * @param client LLM client (typically from admin settings in the web app)
     */
    public LlmApplicantSummaryService(DeepSeekClient client) {
        this.client = client;
    }

    /**
     * Returns 5–8 concise bullet lines for MO review cards.
     * Falls back to deterministic rule-based lines if the LLM is unavailable.
     *
     * @param profile           applicant profile (may be null)
     * @param job               job posting
     * @param match             rule-based match result
     * @param currentWorkload   number of jobs the applicant is already selected for
     * @param workloadBalanced  whether workload is at or below cohort average
     * @return summary lines (empty when inputs are invalid)
     */
    public List<String> buildSummaryLines(TAProfile profile,
                                          Job job,
                                          AIMatchService.MatchResult match,
                                          int currentWorkload,
                                          boolean workloadBalanced) {
        if (job == null || match == null) {
            return Collections.emptyList();
        }
        if (!client.isConfigured()) {
            return buildFallbackSummary(profile, job, match, currentWorkload, workloadBalanced);
        }

        String[] prompt = buildPromptPair(profile, job, match, currentWorkload, workloadBalanced);
        try {
            String raw = client.chat(prompt[0], prompt[1]);
            List<String> lines = normalizeLines(raw);
            if (lines.size() >= 5) {
                return lines;
            }
        } catch (IOException | IllegalStateException ignored) {
            // Fall through to deterministic summary.
        }
        return buildFallbackSummary(profile, job, match, currentWorkload, workloadBalanced);
    }

    /**
     * Streaming counterpart of {@link #buildSummaryLines}. Pushes each token chunk to
     * {@code onChunk} and returns the full raw text the LLM produced (caller can normalise
     * into lines if needed). The deterministic fallback used in the non-streaming path is
     * skipped here because we have already started emitting an SSE response when the LLM
     * call fails; surface the error to the caller instead.
     *
     * @param profile           applicant profile (may be null)
     * @param job               job posting (required)
     * @param match             rule-based match result (required)
     * @param currentWorkload   selected job count for workload context
     * @param workloadBalanced  workload balance flag
     * @param onChunk           receives each streamed token
     * @return full raw LLM text (caller may normalise into lines)
     * @throws IOException when job/match are missing or the stream fails
     */
    public String buildSummaryStream(TAProfile profile, Job job, AIMatchService.MatchResult match,
                                     int currentWorkload, boolean workloadBalanced,
                                     Consumer<String> onChunk) throws IOException {
        if (job == null || match == null) {
            throw new IOException("Job and match result are required for AI summary.");
        }
        String[] prompt = buildPromptPair(profile, job, match, currentWorkload, workloadBalanced);
        String full = client.chatStream(prompt[0], prompt[1], onChunk);
        return full != null ? full : "";
    }

    private static String[] buildPromptPair(TAProfile profile, Job job, AIMatchService.MatchResult match,
                                            int currentWorkload, boolean workloadBalanced) {
        String systemPrompt = "You assist a university MO in screening TA applications. "
                + "Return exactly 5 to 8 short lines, each line plain text without markdown bullets or numbering. "
                + "Cover: core skills, strongest fit points, key risks/gaps, workload fairness signal, and interview recommendation. "
                + "Use factual language and do not invent data.";
        String userMessage = buildPromptData(profile, job, match, currentWorkload, workloadBalanced);
        return new String[]{systemPrompt, userMessage};
    }

    private static String buildPromptData(TAProfile profile,
                                          Job job,
                                          AIMatchService.MatchResult match,
                                          int currentWorkload,
                                          boolean workloadBalanced) {
        StringBuilder sb = new StringBuilder();
        sb.append("Job title: ").append(nullToEmpty(job.getTitle())).append('\n');
        sb.append("Module: ").append(nullToEmpty(job.getModuleCode())).append(' ').append(nullToEmpty(job.getModuleName())).append('\n');
        sb.append("Required skills: ")
                .append(job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "")
                .append('\n');
        sb.append("Applicant skills: ")
                .append(profile != null && profile.getSkills() != null ? String.join(", ", profile.getSkills()) : "not provided")
                .append('\n');
        sb.append("Rule-based match score: ").append(Math.round(match.score)).append("%\n");
        sb.append("Matched skills: ").append(String.join(", ", match.matched)).append('\n');
        sb.append("Missing skills: ").append(String.join(", ", match.missing)).append('\n');
        sb.append("Current selected workload: ").append(currentWorkload).append(" jobs\n");
        sb.append("Balanced workload signal: ").append(workloadBalanced ? "yes" : "no").append('\n');
        return sb.toString();
    }

    private static List<String> normalizeLines(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        String[] split = raw.split("\\r?\\n");
        for (String s : split) {
            String line = stripPrefix(s);
            if (!line.isEmpty()) {
                lines.add(line);
            }
            if (lines.size() >= 8) {
                break;
            }
        }
        if (lines.size() > 5) {
            return lines;
        }
        return Collections.emptyList();
    }

    private static List<String> buildFallbackSummary(TAProfile profile,
                                                     Job job,
                                                     AIMatchService.MatchResult match,
                                                     int currentWorkload,
                                                     boolean workloadBalanced) {
        List<String> lines = new ArrayList<>();
        lines.add("Match score is " + Math.round(match.score) + "% for " + nullToEmpty(job.getTitle()) + ".");
        lines.add(match.matched.isEmpty()
                ? "No explicit required skill match was detected."
                : "Strongest matched skills: " + joinTop(match.matched, 3) + ".");
        lines.add(match.missing.isEmpty()
                ? "No major required-skill gaps were detected."
                : "Main missing skills: " + joinTop(match.missing, 3) + ".");
        lines.add(profile == null
                ? "Candidate profile is missing; verify details before final decision."
                : "Profile is available with " + (profile.getSkills() != null ? profile.getSkills().size() : 0) + " listed skills.");
        lines.add("Current selected workload is " + currentWorkload + " jobs (" + (workloadBalanced ? "balanced" : "above average") + ").");
        lines.add(match.score >= 70 && workloadBalanced
                ? "Interview recommendation: strong yes."
                : match.score >= 50 ? "Interview recommendation: consider interview with targeted questions."
                : "Interview recommendation: low priority unless applicant pool is limited.");
        return lines;
    }

    private static String joinTop(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> top = values.size() <= max ? values : values.subList(0, max);
        return String.join(", ", top);
    }

    private static String stripPrefix(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceFirst("^[\\-\\*\\d\\.\\)\\s]+", "").trim();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
