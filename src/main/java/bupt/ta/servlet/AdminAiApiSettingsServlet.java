package bupt.ta.servlet;

import bupt.ta.llm.DeepSeekClient;
import bupt.ta.model.AiApiSettings;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Admin page for configuring the DeepSeek-compatible LLM API.
 * <p>
 * GET renders the form prefilled with values from {@code data/ai-api-settings.json}; POST
 * validates input and persists changes via {@link DataStorage#saveAiApiSettings(AiApiSettings)}.
 * An empty {@code apiKey} form field keeps the previously saved key (so reloading the page
 * never leaks the key into HTML).
 */
public class AdminAiApiSettingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AiApiSettings settings = storage.loadAiApiSettings();
        renderForm(req, resp, settings, null);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AiApiSettings settings = storage.loadAiApiSettings();

        String error = applySettingsFromRequest(settings, req);
        if (error != null) {
            renderForm(req, resp, settings, error);
            return;
        }
        storage.saveAiApiSettings(settings);
        resp.sendRedirect(req.getContextPath() + "/admin/ai-api?saved=1");
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp,
                            AiApiSettings settings, String error)
            throws ServletException, IOException {
        DeepSeekClient probe = DeepSeekClient.fromAdminSettings(settings);
        req.setAttribute("aiApiSettings", settings);
        req.setAttribute("deepSeekEffectiveConfigured", probe.isConfigured());
        if (error != null) {
            req.setAttribute("error", error);
        }
        req.getRequestDispatcher("/admin/ai-api-settings.jsp").forward(req, resp);
    }

    private static String applySettingsFromRequest(AiApiSettings settings, HttpServletRequest req) {
        if (settings == null) {
            return "AI API settings are not available.";
        }
        String provider = trim(req.getParameter("provider"));
        String baseUrl = trim(req.getParameter("baseUrl"));
        String model = trim(req.getParameter("model"));
        String submittedKey = req.getParameter("apiKey");

        if (baseUrl != null && !baseUrl.isEmpty()) {
            String lower = baseUrl.toLowerCase();
            if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
                return "Base URL must start with http:// or https://.";
            }
        }

        settings.setApiEnabled(req.getParameter("apiEnabled") != null);
        settings.setStreamingEnabled(req.getParameter("streamingEnabled") != null);
        settings.setProvider(provider);
        settings.setBaseUrl(baseUrl);
        settings.setModel(model);
        if (submittedKey != null && !submittedKey.trim().isEmpty()) {
            settings.setApiKey(submittedKey.trim());
        }
        return null;
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
