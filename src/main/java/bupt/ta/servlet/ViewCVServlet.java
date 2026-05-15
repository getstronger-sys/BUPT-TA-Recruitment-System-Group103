package bupt.ta.servlet;

import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Streams an uploaded CV file for authorized TA, MO, or admin viewers.
 */
public class ViewCVServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String role = (String) req.getSession().getAttribute("role");
        String currentUserId = (String) req.getSession().getAttribute("userId");
        String targetUserId = req.getParameter("userId");

        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            targetUserId = currentUserId;
        }

        boolean allowed = "ADMIN".equals(role) || "MO".equals(role) || ("TA".equals(role) && targetUserId.equals(currentUserId));
        if (!allowed) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "No permission to view this CV");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        TAProfile profile = storage.getProfileByUserId(targetUserId);
        if (profile == null || profile.getCvFilePath() == null || profile.getCvFilePath().trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CV not uploaded");
            return;
        }

        // Same base dir as DataStorage / CVUploadServlet (not getRealPath webapp root).
        String raw = profile.getCvFilePath().replace("\\", "/").trim();
        Path dataRoot = storage.getBasePath().normalize();
        Path file;
        if (Path.of(raw).isAbsolute()) {
            file = Path.of(raw).normalize();
        } else {
            String rel = raw;
            if (rel.startsWith("data/")) {
                rel = rel.substring(5);
            }
            file = dataRoot.resolve(rel).normalize();
        }
        if (!file.startsWith(dataRoot)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CV path");
            return;
          }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "CV file not found");
            return;
        }

        String filename = file.getFileName().toString();
        String contentType = getServletContext().getMimeType(filename);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        resp.setContentType(contentType);
        boolean asDownload = "1".equals(req.getParameter("download")) || "true".equalsIgnoreCase(req.getParameter("download"));
        String disp = asDownload ? "attachment" : "inline";
        resp.setHeader("Content-Disposition", disp + "; filename=\"" + filename.replace("\"", "") + "\"");
        resp.setContentLengthLong(Files.size(file));
        Files.copy(file, resp.getOutputStream());
    }
}
