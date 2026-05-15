package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.service.ApplicationTimelineService;
import bupt.ta.service.StudentNotificationService;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.JobSelectionCapacity;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lets a TA withdraw an application; may promote waitlisted candidates when auto-fill is enabled.
 */
public class WithdrawApplicationServlet extends HttpServlet {

    private final AIMatchService aiService = new AIMatchService();
    private final ApplicationTimelineService timelineService = new ApplicationTimelineService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String appId = req.getParameter("applicationId");
        String applicantId = (String) req.getSession().getAttribute("userId");

        if (appId == null || appId.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/ta/applications?error=invalid");
            return;
        }

        DataStorage storage = new DataStorage(getServletContext());
        List<Application> apps = storage.loadApplications();
        Application target = apps.stream()
                .filter(a -> a.getId().equals(appId) && applicantId.equals(a.getApplicantId()))
                .findFirst().orElse(null);

        if (target == null) {
            resp.sendRedirect(req.getContextPath() + "/ta/applications?error=not_found");
            return;
        }
        if (!"PENDING".equals(target.getStatus())
                && !"INTERVIEW".equals(target.getStatus())
                && !"WAITLIST".equals(target.getStatus())
                && !"SELECTED".equals(target.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/ta/applications?error=already_processed");
            return;
        }

        boolean wasSelected = "SELECTED".equals(target.getStatus());
        Job job = storage.getJobById(target.getJobId());
        String fromStatus = target.getStatus();
        target.setStatus("WITHDRAWN");
        storage.saveApplication(target);
        timelineService.record(storage, target, job, applicantId, target.getApplicantName(), "TA",
                ApplicationTimelineService.TYPE_WITHDRAWN,
                "Application withdrawn",
                "Applicant withdrew this application.",
                fromStatus, target.getStatus());
        StudentNotificationService.notifyWithdrawn(storage, target, job);
        if (wasSelected && job != null && job.isAutoFillFromWaitlist()) {
            autoPromoteFromWaitlist(storage, job);
        }
        resp.sendRedirect(req.getContextPath() + "/ta/applications?withdrawn=1");
    }

    private void autoPromoteFromWaitlist(DataStorage storage, Job job) throws IOException {
        List<Application> apps = storage.getApplicationsByJobId(job.getId());
        if (!JobSelectionCapacity.hasVacancy(job, apps, null)) {
            return;
        }
        List<Application> waitlist = apps.stream()
                .filter(a -> "WAITLIST".equals(a.getStatus()))
                .collect(Collectors.toList());
        if (waitlist.isEmpty()) {
            return;
        }

        Map<String, TAProfile> profileByUser = storage.loadProfiles().stream()
                .collect(Collectors.toMap(TAProfile::getUserId, p -> p, (a, b) -> a));

        waitlist.sort(Comparator
                .comparingDouble((Application a) -> aiService.matchSkills(profileByUser.get(a.getApplicantId()), job).score)
                .reversed()
                .thenComparing(a -> a.getAppliedAt() != null ? a.getAppliedAt() : ""));

        Application next = waitlist.get(0);
        next.setStatus("SELECTED");
        String note = next.getNotes() != null && !next.getNotes().trim().isEmpty() ? next.getNotes().trim() + " " : "";
        next.setNotes((note + "Auto-promoted from waitlist after a selected TA withdrew.").trim());
        storage.saveApplication(next);
        timelineService.record(storage, next, job, "SYSTEM", "System", "SYSTEM",
                ApplicationTimelineService.TYPE_AUTO_PROMOTED,
                "Auto-promoted from waitlist",
                "Selected automatically after a vacancy opened.",
                "WAITLIST", next.getStatus());
        StudentNotificationService.notifyAutoPromotedFromWaitlist(storage, next, job);
    }
}
