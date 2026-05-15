package bupt.ta.servlet;

import bupt.ta.model.Application;
import bupt.ta.model.ApplicationEvent;
import bupt.ta.model.InterviewSlot;
import bupt.ta.model.Job;
import bupt.ta.service.AdminService;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lists the logged-in TA's applications with status summaries and timeline events.
 */
public class ApplicationStatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String applicantId = (String) req.getSession().getAttribute("userId");
        DataStorage storage = new DataStorage(getServletContext());
        storage.syncJobStatusesWithDeadlines();

        List<Application> applications = storage.getApplicationsByApplicantId(applicantId);
        List<Job> allJobs = storage.loadJobs();
        List<InterviewSlot> allSlots = storage.loadInterviewSlots();
        Map<String, Job> jobMap = allJobs.stream().collect(Collectors.toMap(Job::getId, j -> j));
        Map<String, Integer> slotCountByJobId = new HashMap<>();
        for (InterviewSlot slot : allSlots) {
            slotCountByJobId.merge(slot.getJobId(), 1, Integer::sum);
        }

        List<Object[]> enriched = new ArrayList<>();
        List<String> applicationIds = new ArrayList<>();
        int selectedCount = 0;
        int pendingCount = 0;
        int interviewCount = 0;
        int waitlistCount = 0;
        int rejectedCount = 0;
        int autoClosedCount = 0;
        for (Application a : applications) {
            Job j = jobMap.get(a.getJobId());
            enriched.add(new Object[]{a, j});
            applicationIds.add(a.getId());
            if ("SELECTED".equals(a.getStatus())) selectedCount++;
            else if ("PENDING".equals(a.getStatus())) pendingCount++;
            else if ("INTERVIEW".equals(a.getStatus())) interviewCount++;
            else if ("WAITLIST".equals(a.getStatus())) waitlistCount++;
            else if ("REJECTED".equals(a.getStatus()) || "WITHDRAWN".equals(a.getStatus())) rejectedCount++;
            else if (AdminService.STATUS_AUTO_CLOSED.equals(a.getStatus())) autoClosedCount++;
        }

        int points = selectedCount * 100 + pendingCount * 20 + interviewCount * 40 + waitlistCount * 30;
        req.setAttribute("applications", enriched);
        req.setAttribute("eventsByApplicationId", storage.getApplicationEventsByApplicationIds(applicationIds));
        req.setAttribute("selectedCount", selectedCount);
        req.setAttribute("pendingCount", pendingCount);
        req.setAttribute("interviewCount", interviewCount);
        req.setAttribute("waitlistCount", waitlistCount);
        req.setAttribute("rejectedCount", rejectedCount);
        req.setAttribute("autoClosedCount", autoClosedCount);
        req.setAttribute("slotCountByJobId", slotCountByJobId);
        req.setAttribute("points", points);
        req.getRequestDispatcher("/ta/applications.jsp").forward(req, resp);
    }
}
