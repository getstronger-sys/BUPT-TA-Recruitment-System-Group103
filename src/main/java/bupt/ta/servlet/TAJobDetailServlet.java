package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TA-facing full job detail; apply is submitted from this page.
 */
public class TAJobDetailServlet extends HttpServlet {

    private final AIMatchService aiService = new AIMatchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jobId = req.getParameter("jobId");
        if (jobId == null || jobId.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=invalid_job");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        storage.syncJobStatusesWithDeadlines();
        Job job = storage.getJobById(jobId.trim());
        if (job == null) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=job_not_found");
            return;
        }

        String applicantId = (String) req.getSession().getAttribute("userId");
        TAProfile profile = applicantId != null ? storage.getOrCreateProfile(applicantId) : null;
        AIMatchService.MatchResult match = aiService.matchSkills(profile, job);
        boolean saved = profile != null && profile.getSavedJobIds().contains(job.getId());

        req.setAttribute("job", job);
        req.setAttribute("match", match);
        req.setAttribute("saved", saved);
        req.setAttribute("llmEnabled", storage.loadAiApiSettings().isEffectivelyConfigured());
        req.getRequestDispatcher("/ta/job-detail.jsp").forward(req, resp);
    }
}
