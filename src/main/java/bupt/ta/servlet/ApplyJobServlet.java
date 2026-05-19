package bupt.ta.servlet;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.User;
import bupt.ta.service.AdminService;
import bupt.ta.service.ApplicationTimelineService;
import bupt.ta.service.StudentNotificationService;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.JobWorkloadEstimator;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Submits a TA job application and records the initial timeline event.
 */
public class ApplyJobServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();
    private final ApplicationTimelineService timelineService = new ApplicationTimelineService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String jobId = req.getParameter("jobId");
        String applicantId = (String) req.getSession().getAttribute("userId");
        String applicantName = (String) req.getSession().getAttribute("realName");
        if (applicantName == null) {
            User u = (User) req.getSession().getAttribute("user");
            applicantName = u != null ? u.getUsername() : "Unknown";
        }

        if (jobId == null || jobId.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=invalid_job");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        storage.syncJobStatusesWithDeadlines();
        Job job = storage.getJobById(jobId);
        if (job == null) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=job_not_found");
            return;
        }
        if (!"OPEN".equals(job.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=job_closed");
            return;
        }
        if (storage.hasApplied(jobId, applicantId)) {
            resp.sendRedirect(req.getContextPath() + "/ta/jobs?error=already_applied");
            return;
        }

        AdminSettings adminSettings = storage.loadAdminSettings();
        if (adminSettings.usesHourWorkloadLimit()) {
            double cap = adminSettings.getMaxWorkloadHoursPerTa();
            double already = adminService.sumSelectedWorkloadHours(storage, applicantId);
            double add = JobWorkloadEstimator.estimatedHoursPerSelectedTa(job);
            if (already + add > cap + 1e-9) {
                String q = URLEncoder.encode(jobId, StandardCharsets.UTF_8);
                resp.sendRedirect(req.getContextPath() + "/ta/apply-confirm?jobId=" + q + "&error=workload_hours_cap");
                return;
            }
        }

        Application app = new Application();
        app.setJobId(jobId);
        app.setApplicantId(applicantId);
        app.setApplicantName(applicantName);
        storage.addApplication(app);
        timelineService.record(storage, app, job, applicantId, applicantName, "TA",
                ApplicationTimelineService.TYPE_SUBMITTED,
                "Application submitted",
                "Application submitted for this posting.",
                "", app.getStatus());
        StudentNotificationService.notifyApplicationSubmitted(storage, app, job);

        resp.sendRedirect(req.getContextPath() + "/ta/applications?success=1");
    }
}
