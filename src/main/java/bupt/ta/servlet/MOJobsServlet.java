package bupt.ta.servlet;

import bupt.ta.ai.AIMatchService;
import bupt.ta.llm.DeepSeekClient;
import bupt.ta.model.AssignedModule;
import bupt.ta.model.Application;
import bupt.ta.model.InterviewEvaluation;
import bupt.ta.model.Job;
import bupt.ta.model.TAProfile;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.JobActivity;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MO dashboard listing jobs and applicants grouped by recruitment stage.
 */
public class MOJobsServlet extends HttpServlet {

    private static final Set<String> JOB_VIEWS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("pending", "interview", "waitlist", "withdrawn", "outcome")));

    private final AIMatchService aiService = new AIMatchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Logged-in user id (MO)
        String moId = (String) req.getSession().getAttribute("userId");
        // Persistence layer for jobs, applications, profiles
        DataStorage storage = new DataStorage(getServletContext());
        storage.syncJobStatusesWithDeadlines();

        // Jobs posted by this MO that are still active (open) for management
        List<Job> jobs = storage.loadJobs().stream()
                .filter(j -> moId.equals(j.getPostedBy()))
                .filter(JobActivity::isActive)
                .collect(Collectors.toList());

        List<Application> allApps = storage.loadApplications();
        Map<String, InterviewEvaluation> evaluationByApplicationId = storage.loadInterviewEvaluations().stream()
                .filter(e -> e != null && e.getApplicationId() != null)
                .collect(Collectors.toMap(InterviewEvaluation::getApplicationId, e -> e, (a, b) -> a));
        // jobId -> applications for that job
        Map<String, List<Application>> appsByJob = allApps.stream().collect(Collectors.groupingBy(Application::getJobId));

        // SELECTED applications: used to compute per-TA workload
        List<Application> selectedApps = allApps.stream().filter(a -> "SELECTED".equals(a.getStatus())).collect(Collectors.toList());
        // applicantId -> number of SELECTED roles (workload)
        Map<String, Integer> workloadByTa = new HashMap<>();
        for (Application a : selectedApps) {
            // one increment per SELECTED application
            workloadByTa.merge(a.getApplicantId(), 1, Integer::sum);
        }
        // average workload across TAs with at least one SELECTED; 0 if none
        double avgWorkload = workloadByTa.isEmpty() ? 0 : workloadByTa.values().stream().mapToInt(Integer::intValue).average().orElse(0);

        List<TAProfile> profiles = storage.loadProfiles();
        // userId -> profile for skill matching; first wins on duplicate keys
        Map<String, TAProfile> profileByUser = profiles.stream().collect(Collectors.toMap(TAProfile::getUserId, p -> p, (a, b) -> a));

        // Per active job: job + five status buckets + AI match / workload hints
        List<Object[]> enrichedAll = buildEnriched(jobs, appsByJob, profileByUser, workloadByTa, avgWorkload);

        String paramJobId = req.getParameter("jobId");
        // null -> empty; else trim
        final String selectedJobId = paramJobId != null ? paramJobId.trim() : "";
        // no jobId: job list mode; with jobId: single-job management mode
        boolean jobListMode = selectedJobId.isEmpty();

        String view = req.getParameter("view");
        if (view == null || !JOB_VIEWS.contains(view)) {
            // invalid or missing -> default tab
            view = "pending";
        }

        if (!jobListMode) {
            Job sel = storage.getJobById(selectedJobId);
            if (sel == null || !moId.equals(sel.getPostedBy())) {
                resp.sendRedirect(req.getContextPath() + "/mo/jobs?error=invalid_job");
                return;
            }
            if (JobActivity.isInactive(sel)) {
                // inactive jobs are managed under past postings
                resp.sendRedirect(req.getContextPath() + JobActivity.PATH_INACTIVE + "?jobId="
                        + URLEncoder.encode(selectedJobId, StandardCharsets.UTF_8) + "&view=" + view);
                return;
            }
        }

        req.setAttribute("moPastJobsPage", Boolean.FALSE);
        req.setAttribute("moJobsBase", req.getContextPath() + JobActivity.PATH_ACTIVE);
        req.setAttribute("assignedModules", storage.loadAssignedModulesForMo(moId));
        req.setAttribute("evaluationByApplicationId", evaluationByApplicationId);
        req.setAttribute("moJobPickList", enrichedAll);
        req.setAttribute("moJobListMode", jobListMode);
        req.setAttribute("moSelectedJobId", jobListMode ? "" : selectedJobId);
        req.setAttribute("moJobsView", view);

        if (jobListMode) {
            req.setAttribute("moJobsCountPending", 0);
            req.setAttribute("moJobsCountInterview", 0);
            req.setAttribute("moJobsCountWaitlist", 0);
            req.setAttribute("moJobsCountWithdrawn", 0);
            req.setAttribute("moJobsCountOutcome", 0);
            req.setAttribute("jobsWithApps", Collections.emptyList());
        } else {
            List<Object[]> oneJob = enrichedAll.stream()
                    .filter(row -> selectedJobId.equals(((Job) row[0]).getId()))
                    .collect(Collectors.toList());
            req.setAttribute("jobsWithApps", oneJob);
            // tab badge counts for the selected job row
            int countPending = 0;
            int countInterview = 0;
            int countWaitlist = 0;
            int countWithdrawn = 0;
            int countOutcome = 0;
            for (Object[] row : oneJob) {
                // row[1..5]: pending, interview, waitlist, withdrawn, outcome lists
                countPending += listSize(row, 1);
                countInterview += listSize(row, 2);
                countWaitlist += listSize(row, 3);
                countWithdrawn += listSize(row, 4);
                countOutcome += listSize(row, 5);
            }
            req.setAttribute("moJobsCountPending", countPending);
            req.setAttribute("moJobsCountInterview", countInterview);
            req.setAttribute("moJobsCountWaitlist", countWaitlist);
            req.setAttribute("moJobsCountWithdrawn", countWithdrawn);
            req.setAttribute("moJobsCountOutcome", countOutcome);
        }

        req.setAttribute("llmEnabled", DeepSeekClient.isRuntimeConfigured(storage.loadAiApiSettings()));
        req.getRequestDispatcher("/mo/jobs.jsp").forward(req, resp);
    }

    private List<Object[]> buildEnriched(List<Job> jobs,
                                         Map<String, List<Application>> appsByJob,
                                         Map<String, TAProfile> profileByUser,
                                         Map<String, Integer> workloadByTa,
                                         double avgWorkload) {
        List<Object[]> enriched = new ArrayList<>();
        for (Job j : jobs) {
            List<Application> apps = appsByJob.getOrDefault(j.getId(), new ArrayList<>());
            List<AIMatchService.ApplicantRecommendation> all = new ArrayList<>();
            for (Application a : apps) {
                TAProfile profile = profileByUser.get(a.getApplicantId());
                AIMatchService.MatchResult match = aiService.matchSkills(profile, j);
                int workload = workloadByTa.getOrDefault(a.getApplicantId(), 0);
                boolean balanced = workload <= avgWorkload;
                all.add(new AIMatchService.ApplicantRecommendation(a, profile, match, workload, balanced));
            }
            List<AIMatchService.ApplicantRecommendation> pending = new ArrayList<>();
            List<AIMatchService.ApplicantRecommendation> interview = new ArrayList<>();
            List<AIMatchService.ApplicantRecommendation> waitlist = new ArrayList<>();
            List<AIMatchService.ApplicantRecommendation> withdrawn = new ArrayList<>();
            List<AIMatchService.ApplicantRecommendation> outcome = new ArrayList<>();
            for (AIMatchService.ApplicantRecommendation r : all) {
                String s = r.application.getStatus();
                if ("WITHDRAWN".equals(s)) {
                    withdrawn.add(r);
                } else if ("PENDING".equals(s)) {
                    pending.add(r);
                } else if ("INTERVIEW".equals(s)) {
                    interview.add(r);
                } else if ("WAITLIST".equals(s)) {
                    waitlist.add(r);
                } else {
                    outcome.add(r);
                }
            }
            sortByMatch(pending);
            sortByMatch(interview);
            sortByMatch(waitlist);
            sortByMatch(withdrawn);
            sortByMatch(outcome);
            enriched.add(new Object[]{j, pending, interview, waitlist, withdrawn, outcome});
        }
        return enriched;
    }

    @SuppressWarnings("unchecked")
    private static int listSize(Object[] row, int idx) {
        List<AIMatchService.ApplicantRecommendation> list = (List<AIMatchService.ApplicantRecommendation>) row[idx];
        return list != null ? list.size() : 0;
    }

    private static void sortByMatch(List<AIMatchService.ApplicantRecommendation> recs) {
        recs.sort((r1, r2) -> {
            int cmp = Double.compare(r2.matchResult.score, r1.matchResult.score);
            if (cmp != 0) return cmp;
            return Integer.compare(r1.currentWorkload, r2.currentWorkload);
        });
    }
}
