package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.llm.DeepSeekClient;
import bupt.ta.llm.LlmApplicantSummaryService;
import bupt.ta.llm.SseEmitter;
import bupt.ta.model.AiApiSettings;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * MO-only endpoint: generate AI summary for one application on demand.
 * Streams via SSE when admin enables streaming; otherwise replies with the original JSON
 * {@code {ok, lines:[...]}} body. The deterministic fallback only applies to the JSON path
 * because once SSE has started we can no longer rewind the response.
 */
public class MOApplicantSummaryServlet extends HttpServlet {

    private final AIMatchService aiService = new AIMatchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moId = (String) req.getSession().getAttribute("userId");
        String applicationId = trim(req.getParameter("applicationId"));
        if (moId == null || moId.isEmpty() || applicationId.isEmpty()) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters.");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        List<Application> allApps = storage.loadApplications();
        Application target = null;
        for (Application app : allApps) {
            if (applicationId.equals(app.getId())) {
                target = app;
                break;
            }
        }
        if (target == null) {
            writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND, "Application not found.");
            return;
        }

        Job job = storage.getJobById(target.getJobId());
        if (job == null || !moId.equals(job.getPostedBy())) {
            writeJsonError(resp, HttpServletResponse.SC_FORBIDDEN, "You do not have access to this application.");
            return;
        }

        TAProfile profile = storage.getProfileByUserId(target.getApplicantId());
        AIMatchService.MatchResult match = aiService.matchSkills(profile, job);

        int currentWorkload = 0;
        int selectedTotal = 0;
        for (Application app : allApps) {
            if ("SELECTED".equals(app.getStatus())) {
                selectedTotal += 1;
                if (target.getApplicantId().equals(app.getApplicantId())) {
                    currentWorkload += 1;
                }
            }
        }
        long selectedApplicants = allApps.stream()
                .filter(a -> "SELECTED".equals(a.getStatus()))
                .map(Application::getApplicantId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .count();
        double avgWorkload = selectedApplicants == 0 ? 0 : (selectedTotal * 1.0 / selectedApplicants);
        boolean balanced = currentWorkload <= avgWorkload;

        AiApiSettings settings = storage.loadAiApiSettings();
        DeepSeekClient client = DeepSeekClient.fromRuntimeSettings(settings);
        LlmApplicantSummaryService summaryService = new LlmApplicantSummaryService(client);

        if (settings.isStreamingEnabled() && client.isConfigured()) {
            streamSummary(resp, summaryService, profile, job, match, currentWorkload, balanced);
        } else {
            replySummaryJson(resp, summaryService, profile, job, match, currentWorkload, balanced);
        }
    }

    private static void replySummaryJson(HttpServletResponse resp, LlmApplicantSummaryService service,
                                         TAProfile profile, Job job, AIMatchService.MatchResult match,
                                         int currentWorkload, boolean balanced) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        List<String> lines = service.buildSummaryLines(profile, job, match, currentWorkload, balanced);
        JsonObject data = new JsonObject();
        data.addProperty("ok", true);
        JsonArray arr = new JsonArray();
        for (String line : lines) {
            arr.add(line);
        }
        data.add("lines", arr);
        resp.getWriter().write(data.toString());
    }

    private static void streamSummary(HttpServletResponse resp, LlmApplicantSummaryService service,
                                      TAProfile profile, Job job, AIMatchService.MatchResult match,
                                      int currentWorkload, boolean balanced) throws IOException {
        SseEmitter emitter = new SseEmitter(resp);
        try {
            service.buildSummaryStream(profile, job, match, currentWorkload, balanced, emitter::sendChunk);
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
