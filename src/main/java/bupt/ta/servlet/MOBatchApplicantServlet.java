package bupt.ta.servlet;

import bupt.ta.model.Application;
import bupt.ta.model.Job;
import bupt.ta.service.ApplicationTimelineService;
import bupt.ta.service.StudentNotificationService;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.InterviewNoticeTimeSupport;
import bupt.ta.util.JobActivity;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Batch operations for MO: mark PENDING as INTERVIEW, or send in-app interview notice to INTERVIEW applicants.
 */
public class MOBatchApplicantServlet extends HttpServlet {
    private final ApplicationTimelineService timelineService = new ApplicationTimelineService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String moId = (String) req.getSession().getAttribute("userId");
        String moName = (String) req.getSession().getAttribute("realName");
        String action = req.getParameter("action");
        String returnJobId = trim(req.getParameter("returnJobId"));
        String[] ids = req.getParameterValues("applicationId");
        String ctx = req.getContextPath();
        DataStorage storage = new DataStorage(getServletContext());
        if (ids == null || ids.length == 0) {
            resp.sendRedirect(moJobsUrl(ctx, moListPath(storage, returnJobId, moId), "pending", returnJobId, "error=batch_empty"));
            return;
        }
        if (!returnJobId.isEmpty()) {
            Job fixedJob = storage.getJobById(returnJobId);
            if (fixedJob == null || !moId.equals(fixedJob.getPostedBy())) {
                resp.sendRedirect(moJobsUrl(ctx, JobActivity.PATH_ACTIVE, "pending", returnJobId, "error=invalid_job"));
                return;
            }
            if (JobActivity.isInactive(fixedJob)) {
                resp.sendRedirect(moJobsUrl(ctx, JobActivity.PATH_INACTIVE, "pending", returnJobId, "error=job_inactive"));
                return;
            }
        }

        Set<String> idSet = new HashSet<>(Arrays.asList(ids));

        if ("toInterview".equals(action)) {
            for (String appId : idSet) {
                Application target = findApp(storage, appId);
                if (target == null) continue;
                Job job = storage.getJobById(target.getJobId());
                if (job == null || !moId.equals(job.getPostedBy())) continue;
                if (JobActivity.isInactive(job)) continue;
                if (!returnJobId.isEmpty() && !returnJobId.equals(target.getJobId())) continue;
                if (!"PENDING".equals(target.getStatus())) continue;
                String fromStatus = target.getStatus();
                target.setStatus("INTERVIEW");
                storage.saveApplication(target);
                timelineService.recordStatusChange(storage, target, job, moId, moName, "MO",
                        fromStatus, target.getStatus(), "Batch moved to interview stage.");
                StudentNotificationService.notifyInterviewInvite(storage, target, job);
            }
            String jid = resolveReturnJobId(storage, idSet, returnJobId, moId);
            Job jref = jid.isEmpty() ? null : storage.getJobById(jid);
            String listPath = jref != null ? JobActivity.listPathFor(jref) : JobActivity.PATH_ACTIVE;
            resp.sendRedirect(moJobsUrl(ctx, listPath, "interview", jid, "updated=1"));
            return;
        }

        if ("sendNotice".equals(action)) {
            String time;
            try {
                time = InterviewNoticeTimeSupport.normalizeFromForm(
                        req.getParameter("interviewDate"),
                        req.getParameter("interviewClock"));
            } catch (IllegalArgumentException ex) {
                String jid = resolveReturnJobId(storage, idSet, returnJobId, moId);
                Job jref = jid.isEmpty() ? null : storage.getJobById(jid);
                String listPath = jref != null ? JobActivity.listPathFor(jref) : moListPath(storage, returnJobId, moId);
                resp.sendRedirect(moJobsUrl(ctx, listPath, "interview", jid, "error=invalid_notice_time"));
                return;
            }
            String location = trim(req.getParameter("interviewLocation"));
            String assessment = trim(req.getParameter("interviewAssessment"));
            for (String appId : idSet) {
                Application target = findApp(storage, appId);
                if (target == null) continue;
                Job job = storage.getJobById(target.getJobId());
                if (job == null || !moId.equals(job.getPostedBy())) continue;
                if (JobActivity.isInactive(job)) continue;
                if (!returnJobId.isEmpty() && !returnJobId.equals(target.getJobId())) continue;
                if (!"INTERVIEW".equals(target.getStatus())) continue;
                target.setInterviewTime(time);
                target.setInterviewLocation(location);
                target.setInterviewAssessment(assessment);
                storage.saveApplication(target);
                timelineService.record(storage, target, job, moId, moName, "MO",
                        ApplicationTimelineService.TYPE_INTERVIEW_NOTICE,
                        "Interview notice sent",
                        "Time: " + (time.isEmpty() ? "TBD" : time) + "; Location: " + (location.isEmpty() ? "TBD" : location),
                        target.getStatus(), target.getStatus());
                StudentNotificationService.notifyInterviewDetails(storage, target, job);
            }
            String jid = resolveReturnJobId(storage, idSet, returnJobId, moId);
            Job jref = jid.isEmpty() ? null : storage.getJobById(jid);
            String listPath = jref != null ? JobActivity.listPathFor(jref) : JobActivity.PATH_ACTIVE;
            resp.sendRedirect(moJobsUrl(ctx, listPath, "interview", jid, "notice=1"));
            return;
        }

        resp.sendRedirect(moJobsUrl(ctx, moListPath(storage, returnJobId, moId), "pending", returnJobId, "error=invalid_action"));
    }

    private static Application findApp(DataStorage storage, String appId) throws IOException {
        return storage.loadApplications().stream()
                .filter(a -> a.getId().equals(appId))
                .findFirst()
                .orElse(null);
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String moJobsUrl(String ctx, String listPath, String view, String jobId, String extraQuery) {
        StringBuilder b = new StringBuilder(ctx).append(listPath).append("?");
        if (jobId != null && !jobId.isEmpty()) {
            b.append("jobId=").append(URLEncoder.encode(jobId, StandardCharsets.UTF_8)).append("&");
        }
        b.append("view=").append(view);
        if (extraQuery != null && !extraQuery.isEmpty()) {
            b.append("&").append(extraQuery);
        }
        return b.toString();
    }

    private static String moListPath(DataStorage storage, String jobId, String moId) throws IOException {
        if (jobId == null || jobId.isEmpty()) return JobActivity.PATH_ACTIVE;
        Job j = storage.getJobById(jobId);
        if (j != null && moId.equals(j.getPostedBy())) return JobActivity.listPathFor(j);
        return JobActivity.PATH_ACTIVE;
    }

    /** Prefer explicit returnJobId when it matches processed apps; else first app's job owned by MO. */
    private static String resolveReturnJobId(DataStorage storage, Set<String> idSet, String returnJobId, String moId) throws IOException {
        if (returnJobId != null && !returnJobId.isEmpty()) {
            Job j = storage.getJobById(returnJobId);
            if (j != null && moId.equals(j.getPostedBy())) {
                return returnJobId;
            }
        }
        for (String appId : idSet) {
            Application a = findApp(storage, appId);
            if (a == null) continue;
            Job job = storage.getJobById(a.getJobId());
            if (job != null && moId.equals(job.getPostedBy())) {
                return a.getJobId();
            }
        }
        return "";
    }
}
