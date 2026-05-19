package bupt.ta.servlet;

import bupt.ta.cv.ResumeTextExtractor;
import bupt.ta.llm.DeepSeekClient;
import bupt.ta.llm.LlmProfileExtractionService;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uploads a TA CV (PDF/DOCX), stores the file, and optionally pre-fills profile fields via LLM.
 */
@MultipartConfig(maxFileSize = 5242880, maxRequestSize = 5242880) // 5MB
public class CVUploadServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(CVUploadServlet.class.getName());
    private static final String AI_STATUS_FILLED = "filled";
    private static final String AI_STATUS_NO_TEXT = "no_text";
    private static final String AI_STATUS_DISABLED = "disabled";
    private static final String AI_STATUS_NO_CHANGES = "no_changes";
    private static final String AI_STATUS_FAILED = "failed";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = (String) req.getSession().getAttribute("userId");
        Part filePart = req.getPart("cvFile");

        if (filePart == null || filePart.getSize() == 0) {
            resp.sendRedirect(req.getContextPath() + "/ta/profile?error=no_file");
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || fileName.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/ta/profile?error=no_file");
            return;
        }
        String ext = extractExtension(fileName);
        if (!ResumeTextExtractor.supportsExtension(ext)) {
            resp.sendRedirect(req.getContextPath() + "/ta/profile?error=invalid_type");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        Path uploadDir = storage.getUploadPath();
        Files.createDirectories(uploadDir);

        String safeName = "cv_" + userId + "_" + System.currentTimeMillis() + ext;
        Path targetFile = uploadDir.resolve(safeName);

        try (InputStream is = filePart.getInputStream()) {
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = "uploads/" + safeName;
        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile == null) {
            profile = new TAProfile(userId);
        }
        profile.setCvFilePath(relativePath);

        boolean aiFilled = false;
        String aiStatus = AI_STATUS_FAILED;
        try {
            String plain = ResumeTextExtractor.extract(targetFile, ext);
            if (plain == null || plain.trim().isEmpty()) {
                aiStatus = AI_STATUS_NO_TEXT;
            } else {
                DeepSeekClient ds = DeepSeekClient.fromRuntimeSettings(storage.loadAiApiSettings());
                if (!ds.isConfigured()) {
                    aiStatus = AI_STATUS_DISABLED;
                } else {
                    LlmProfileExtractionService extractor = new LlmProfileExtractionService(ds);
                    aiFilled = extractor.extractAndMergeProfile(profile, plain);
                    aiStatus = aiFilled ? AI_STATUS_FILLED : AI_STATUS_NO_CHANGES;
                }
            }
        } catch (Exception ex) {
            aiStatus = AI_STATUS_FAILED;
            LOG.log(Level.WARNING, "CV text extraction or DeepSeek profile merge failed", ex);
        }

        storage.saveProfile(profile);

        String redirect = req.getContextPath() + "/ta/profile?cv_success=1"
                + "&ai_status=" + URLEncoder.encode(aiStatus, StandardCharsets.UTF_8.name());
        if (aiFilled) {
            redirect += "&ai_fill=1";
        }
        resp.sendRedirect(redirect);
    }

    private static String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot).toLowerCase(Locale.ROOT) : "";
    }
}
