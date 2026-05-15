package bupt.ta.llm;

import bupt.ta.ai.AIMatchService;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Optional DeepSeek-generated narrative on top of rule-based {@link AIMatchService.MatchResult}.
 */
public final class LlmMatchInsightService {

    private final DeepSeekClient client;

    /** Uses environment-driven {@link DeepSeekClient} configuration. */
    public LlmMatchInsightService() {
        this(new DeepSeekClient());
    }

    /**
     * @param client LLM client (typically from admin settings in the web app)
     */
    public LlmMatchInsightService(DeepSeekClient client) {
        this.client = client;
    }

    /**
     * @param profile applicant profile (may be null)
     * @param job     job posting
     * @param match   rule-based match result (may be null)
     * @return short English paragraph, or {@code null} if the API is unavailable or fails
     */
    public String buildInsight(TAProfile profile, Job job, AIMatchService.MatchResult match) {
        if (!client.isConfigured() || job == null) {
            return null;
        }
        String[] prompt = buildPrompt(profile, job, match);
        try {
            return client.chat(prompt[0], prompt[1]).trim();
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }

    /**
     * Streaming counterpart of {@link #buildInsight}. Each token chunk is delivered to
     * {@code onChunk} as it arrives; the full text is returned once the stream finishes.
     *
     * @param profile applicant profile (may be null)
     * @param job     job posting (required)
     * @param match   rule-based match result (may be null)
     * @param onChunk receives each streamed token
     * @return full generated text after the stream completes
     * @throws IOException           on transport or API errors
     * @throws IllegalStateException if the client is not configured
     */
    public String buildInsightStream(TAProfile profile, Job job, AIMatchService.MatchResult match,
                                     Consumer<String> onChunk) throws IOException {
        if (job == null) {
            throw new IOException("Job is required for AI insight.");
        }
        String[] prompt = buildPrompt(profile, job, match);
        String full = client.chatStream(prompt[0], prompt[1], onChunk);
        return full != null ? full.trim() : "";
    }

    private static String[] buildPrompt(TAProfile profile, Job job, AIMatchService.MatchResult match) {
        String sys = "You help teaching staff assess TA applicants. "
                + "Write 2-4 short sentences in English: strengths, gaps, and practical fit. "
                + "Be factual; do not invent credentials not implied by the data. "
                + "No bullet points; plain paragraph only.";

        StringBuilder data = new StringBuilder();
        data.append("Job title: ").append(nullToEmpty(job.getTitle())).append('\n');
        data.append("Module: ").append(nullToEmpty(job.getModuleCode())).append(' ')
                .append(nullToEmpty(job.getModuleName())).append('\n');
        data.append("Required skills: ").append(job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "").append('\n');
        data.append("Responsibilities (excerpt): ")
                .append(trim(job.getResponsibilities(), 800)).append('\n');

        if (profile != null) {
            data.append("Applicant skills: ")
                    .append(profile.getSkills() != null ? String.join(", ", profile.getSkills()) : "unknown").append('\n');
            data.append("Programme: ").append(nullToEmpty(profile.getProgramme())).append('\n');
            data.append("TA experience (excerpt): ").append(trim(profile.getTaExperience(), 400)).append('\n');
        } else {
            data.append("Applicant profile: not available.\n");
        }

        if (match != null) {
            data.append("Rule-based match score: ").append(Math.round(match.score)).append("%\n");
            data.append("Matched skills: ").append(match.matched != null ? String.join(", ", match.matched) : "").append('\n');
            data.append("Missing skills: ").append(match.missing != null ? String.join(", ", match.missing) : "").append('\n');
        }
        return new String[]{sys, data.toString()};
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
