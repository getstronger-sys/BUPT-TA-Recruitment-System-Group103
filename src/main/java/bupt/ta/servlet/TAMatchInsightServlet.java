package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.llm.DeepSeekClient;
import bupt.ta.llm.LlmMatchInsightService;
import bupt.ta.llm.SseEmitter;
import bupt.ta.model.AiApiSettings;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * On-demand AI match insight for a TA viewing a job. Invoked via fetch() from
 * {@code /ta/job-detail.jsp} when the TA clicks the "Generate AI insight" button.
 * When admin has enabled streaming the response is delivered as {@code text/event-stream}
 * SSE; otherwise a single JSON payload is returned, preserving the original behaviour.
 */
public class TAMatchInsightServlet extends HttpServlet {

    private final AIMatchService aiService = new AIMatchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = (String) req.getSession().getAttribute("userId");
        String jobId = req.getParameter("jobId");
        if (userId == null || userId.isEmpty() || jobId == null || jobId.trim().isEmpty()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters.");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        Job job = storage.getJobById(jobId.trim());
        if (job == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Job not found.");
            return;
        }

        TAProfile profile = storage.getOrCreateProfile(userId);
        AIMatchService.MatchResult match = aiService.matchSkills(profile, job);

        AiApiSettings settings = storage.loadAiApiSettings();
        DeepSeekClient client = DeepSeekClient.fromRuntimeSettings(settings);
        if (!client.isConfigured()) {
            writeJsonError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AI API is not configured. Ask an admin to enable it.");
            return;
        }

        LlmMatchInsightService service = new LlmMatchInsightService(client);
        if (settings.isStreamingEnabled()) {
            streamInsight(resp, service, profile, job, match);
        } else {
            replyInsightJson(resp, service, profile, job, match);
        }
    }

    private static void replyInsightJson(HttpServletResponse resp, LlmMatchInsightService service,
                                         TAProfile profile, Job job, AIMatchService.MatchResult match) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String insight = service.buildInsight(profile, job, match);
        if (insight == null || insight.trim().isEmpty()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_GATEWAY,
                    "AI did not return a response. Please try again later.");
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("ok", true);
        data.addProperty("insight", insight);
        resp.getWriter().write(data.toString());
    }

    private static void streamInsight(HttpServletResponse resp, LlmMatchInsightService service,
                                      TAProfile profile, Job job, AIMatchService.MatchResult match) throws IOException {
        SseEmitter emitter = new SseEmitter(resp);
        try {
            service.buildInsightStream(profile, job, match, emitter::sendChunk);
            emitter.sendDone();
        } catch (IOException | IllegalStateException ex) {
            emitter.sendError(ex.getMessage() != null ? ex.getMessage() : "AI streaming failed.");
        }
    }

    private static void writeJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        JsonObject error = new JsonObject();
        error.addProperty("ok", false);
        error.addProperty("error", message);
        resp.getWriter().write(error.toString());
    }
}
