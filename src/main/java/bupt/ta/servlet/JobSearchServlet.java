package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TA job search with optional filters and rule-based skill match scores.
 */
public class JobSearchServlet extends HttpServlet {

    private final AIMatchService aiService = new AIMatchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        String moduleCode = req.getParameter("moduleCode");
        String skill = req.getParameter("skill");
        String jobType = req.getParameter("jobType");
        String applicantId = (String) req.getSession().getAttribute("userId");

        DataStorage storage = new DataStorage(getServletContext());
        storage.syncJobStatusesWithDeadlines();
        List<Job> jobs = storage.loadJobs().stream()
                .filter(j -> "OPEN".equals(j.getStatus()))
                .collect(Collectors.toList());

        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim().toLowerCase();
            jobs = jobs.stream()
                    .filter(j -> (j.getTitle() != null && j.getTitle().toLowerCase().contains(k))
                            || (j.getModuleName() != null && j.getModuleName().toLowerCase().contains(k))
                            || (j.getDescription() != null && j.getDescription().toLowerCase().contains(k))
                            || (j.getResponsibilities() != null && j.getResponsibilities().toLowerCase().contains(k)))
                    .collect(Collectors.toList());
        }
        if (moduleCode != null && !moduleCode.trim().isEmpty()) {
            String m = moduleCode.trim().toUpperCase();
            jobs = jobs.stream().filter(j -> m.equals(j.getModuleCode())).collect(Collectors.toList());
        }
        if (skill != null && !skill.trim().isEmpty()) {
            String s = skill.trim().toLowerCase();
            jobs = jobs.stream()
                    .filter(j -> j.getRequiredSkills() != null
                            && j.getRequiredSkills().stream().anyMatch(sk -> sk.toLowerCase().contains(s)))
                    .collect(Collectors.toList());
        }
        if (jobType != null && !jobType.trim().isEmpty()) {
            String jt = jobType.trim();
            jobs = jobs.stream().filter(j -> jt.equals(j.getJobType())).collect(Collectors.toList());
        }

        TAProfile profile = applicantId != null ? storage.getOrCreateProfile(applicantId) : null;
        List<Object[]> jobsWithMatch = new ArrayList<>();
        List<String> savedJobIds = profile != null ? profile.getSavedJobIds() : java.util.Collections.emptyList();
        for (Job j : jobs) {
            AIMatchService.MatchResult match = aiService.matchSkills(profile, j);
            jobsWithMatch.add(new Object[]{j, match, savedJobIds.contains(j.getId())});
        }
        jobsWithMatch.sort((a, b) -> Double.compare(((AIMatchService.MatchResult) b[1]).score, ((AIMatchService.MatchResult) a[1]).score));

        req.setAttribute("jobsWithMatch", jobsWithMatch);
        req.getRequestDispatcher("/ta/jobs.jsp").forward(req, resp);
    }
}
