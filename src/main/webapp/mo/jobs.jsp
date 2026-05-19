<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="bupt.ta.model.AssignedModule" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.Application" %>
<%@ page import="bupt.ta.model.InterviewEvaluation" %>
<%@ page import="bupt.ta.ai.AIMatchService" %>
<%@ page import="bupt.ta.util.WorkQuotaPlanner" %>
<%@ page import="bupt.ta.util.JobActivity" %>
<% List<Object[]> jobsWithApps = (List<Object[]>) request.getAttribute("jobsWithApps"); if (jobsWithApps == null) jobsWithApps = java.util.Collections.emptyList();
   List<Object[]> moJobPickList = (List<Object[]>) request.getAttribute("moJobPickList");
   if (moJobPickList == null) moJobPickList = java.util.Collections.emptyList();
   boolean moJobListMode = Boolean.TRUE.equals(request.getAttribute("moJobListMode"));
   String moSelectedJobId = (String) request.getAttribute("moSelectedJobId");
   if (moSelectedJobId == null) moSelectedJobId = "";
   String moJobIdQ = moSelectedJobId.isEmpty() ? "" : "&jobId=" + java.net.URLEncoder.encode(moSelectedJobId, "UTF-8");
   String moView = (String) request.getAttribute("moJobsView");
   if (moView == null) moView = "pending";
   int moCntP = request.getAttribute("moJobsCountPending") != null ? (Integer) request.getAttribute("moJobsCountPending") : 0;
   int moCntI = request.getAttribute("moJobsCountInterview") != null ? (Integer) request.getAttribute("moJobsCountInterview") : 0;
   int moCntWl = request.getAttribute("moJobsCountWaitlist") != null ? (Integer) request.getAttribute("moJobsCountWaitlist") : 0;
   int moCntW = request.getAttribute("moJobsCountWithdrawn") != null ? (Integer) request.getAttribute("moJobsCountWithdrawn") : 0;
   int moCntO = request.getAttribute("moJobsCountOutcome") != null ? (Integer) request.getAttribute("moJobsCountOutcome") : 0;
   String moJobsBaseAttr = (String) request.getAttribute("moJobsBase");
   String moBase = moJobsBaseAttr != null ? moJobsBaseAttr : request.getContextPath() + "/mo/jobs";
   boolean moPastJobsPage = Boolean.TRUE.equals(request.getAttribute("moPastJobsPage"));
   boolean moReadOnly = moPastJobsPage;
   String moCtx = request.getContextPath();
   List<AssignedModule> assignedModules = (List<AssignedModule>) request.getAttribute("assignedModules");
   if (assignedModules == null) assignedModules = java.util.Collections.emptyList();
   java.util.Map<String, InterviewEvaluation> evaluationByApplicationId = (java.util.Map<String, InterviewEvaluation>) request.getAttribute("evaluationByApplicationId");
   if (evaluationByApplicationId == null) evaluationByApplicationId = java.util.Collections.emptyMap();
   request.setAttribute("moNavActive", moPastJobsPage ? "past" : "jobs");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= moPastJobsPage ? "Past postings - MO" : "My Jobs - MO" %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="nav top-nav">
        <span class="brand">BUPT Teaching Assistant Recruitment System</span>
        <div class="user user-inline-actions"><span><%= escHtml(session.getAttribute("realName")) %> |</span><form action="${pageContext.request.contextPath}/logout" method="post" class="inline-form logout-form"><%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %><button type="submit" class="logout-button">Logout</button></form></div>
    </div>
    <div class="page-layout">
        <div class="left-nav-wrap">
            <div class="icon-rail">
                <div class="icon-dot active">J</div>
                <div class="icon-dot">P</div>
                <div class="icon-dot">D</div>
            </div>
            <%@ include file="/WEB-INF/jspf/mo-side-nav.jspf" %>
        </div>
        <main class="main-panel mo-main mo-page mo-page--mo-jobs">
            <% String err = request.getParameter("error");
               boolean moJobsFlash = "1".equals(request.getParameter("success")) || "1".equals(request.getParameter("updated")) || "1".equals(request.getParameter("notice")) || err != null;
               if (moJobsFlash) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("success"))) { %><p class="success">Job posted successfully!</p><% } %>
            <% if ("1".equals(request.getParameter("updated"))) { %><p class="success">Applicant status updated.</p><% } %>
            <% if ("1".equals(request.getParameter("notice"))) { %><p class="success">Interview notice saved (in-app message).</p><% } %>
            <% if (err != null) {
                   String errMsg = err;
                   if ("not_pending".equals(err)) errMsg = "Only pending applications can be moved to interview.";
                   else if ("not_interview".equals(err)) errMsg = "Only interview-stage applications can be selected.";
                   else if ("capacity_reached".equals(err)) errMsg = "Planned recruit slots are already full for this posting.";
                   else if ("ta_workload_cap".equals(err)) errMsg = "This applicant has reached the admin workload cap and cannot be selected for another post.";
                   else if ("evaluation_required".equals(err)) errMsg = "Save an interview evaluation before selecting this applicant.";
                   else if ("decision_reason_required".equals(err)) errMsg = "Selection requires a decision reason.";
                   else if ("not_applicant".equals(err)) errMsg = "This action is not allowed for the current status.";
                   else if ("batch_empty".equals(err)) errMsg = "Select at least one row.";
                   else if ("invalid_action".equals(err)) errMsg = "Invalid action.";
                   else if ("invalid_job".equals(err)) errMsg = "Invalid posting or access denied.";
                   else if ("job_inactive".equals(err)) errMsg = "This posting is closed or past deadline. Management actions are disabled.";
                   else if ("invalid_notice_time".equals(err)) errMsg = "Interview notice requires a valid date and time.";
                   else if ("reopen_deadline_required".equals(err)) errMsg = "The application deadline has passed. Enter a new deadline (today or later) to reopen this posting; it will return to My Jobs.";
            %><p class="error">Error: <%= errMsg %></p><% } %>
            </div>
            <% } %>

            <% if (moJobListMode) { %>
            <header class="ta-page-header">
                <p class="ta-page-kicker"><%= moPastJobsPage ? "History" : "Postings" %></p>
                <h1><%= moPastJobsPage ? "Past postings" : "Your postings" %></h1>
                <p class="ta-page-lead">
                    <%= moPastJobsPage
                            ? "Review closed or expired postings for history and records."
                            : "Choose one posting to manage applicants, interviews, and outcomes in a focused workflow." %>
                </p>
            </header>
            <div class="ta-panel ta-panel--tip">
                <strong class="ta-panel__title"><%= moPastJobsPage ? "Closed or past deadline" : "One posting at a time" %></strong>
                <% if (moPastJobsPage) { %>
                <p class="ta-panel__body">These jobs were closed manually or are past the application deadline. Use this list for history only. Active recruitment is under <strong>My Jobs</strong> in the sidebar.</p>
                <% } else { %>
                <p class="ta-panel__body">Choose a job below to manage it. Applicants, interviewees, and progress are kept separate per posting and are not mixed in one list.</p>
                <% } %>
            </div>
            <div class="ta-panel">
                <strong class="ta-panel__title">Assigned modules this term</strong>
                <% if (assignedModules.isEmpty()) { %>
                <p class="ta-panel__lede muted-inline">No modules assigned by admin yet. Contact admin before posting new jobs.</p>
                <% } else { %>
                <p class="ta-panel__lede muted-inline">
                    <% for (int i = 0; i < assignedModules.size(); i++) {
                           AssignedModule am = assignedModules.get(i);
                           if (am == null || am.getModuleCode() == null || am.getModuleCode().trim().isEmpty()) continue;
                    %>
                    <code><%= escHtml(am.getModuleCode().trim().toUpperCase()) %></code><%= (am.getModuleName() != null && !am.getModuleName().trim().isEmpty()) ? " - " + escHtml(am.getModuleName().trim()) : "" %><%= i < assignedModules.size() - 1 ? " | " : "" %>
                    <% } %>
                </p>
                <% } %>
            </div>
            <div class="mo-page-toolbar">
            <% if (moPastJobsPage) { %>
            <a href="<%= moCtx %>/mo/jobs" class="btn btn-primary">Back to active postings</a>
            <% } else { %>
            <a href="<%= moCtx %>/mo/post-job" class="btn btn-primary">Post New Job</a>
            <% } %>
            </div>
            <% if (moJobPickList.isEmpty()) { %>
            <p><%= moPastJobsPage ? "No closed or expired postings." : "You have not posted any jobs yet." %><% if (!moPastJobsPage) { %> <a href="<%= moCtx %>/mo/post-job">Post your first job</a>.<% } %></p>
            <% } else { %>
            <div class="mo-job-pick-grid">
            <% for (Object[] pick : moJobPickList) {
                Job pj = (Job) pick[0];
                List<AIMatchService.ApplicantRecommendation> pr = (List<AIMatchService.ApplicantRecommendation>) pick[1];
                List<AIMatchService.ApplicantRecommendation> ir = (List<AIMatchService.ApplicantRecommendation>) pick[2];
                List<AIMatchService.ApplicantRecommendation> wlr = (List<AIMatchService.ApplicantRecommendation>) pick[3];
                List<AIMatchService.ApplicantRecommendation> wdr = (List<AIMatchService.ApplicantRecommendation>) pick[4];
                List<AIMatchService.ApplicantRecommendation> ou = (List<AIMatchService.ApplicantRecommendation>) pick[5];
                if (pr == null) pr = java.util.Collections.emptyList();
                if (ir == null) ir = java.util.Collections.emptyList();
                if (wlr == null) wlr = java.util.Collections.emptyList();
                if (wdr == null) wdr = java.util.Collections.emptyList();
                if (ou == null) ou = java.util.Collections.emptyList();
                String pickHref = moBase + "?jobId=" + java.net.URLEncoder.encode(pj.getId(), "UTF-8") + "&view=pending";
                String detailHref = moCtx + "/mo/job?jobId=" + java.net.URLEncoder.encode(pj.getId(), "UTF-8");
            %>
                <div class="mo-job-pick-card context-card">
                    <h3><%= escHtml(pj.getTitle()) %></h3>
                    <p class="pick-meta"><%= escHtml(pj.getModuleCode()) %> · <%= escHtml(pj.getModuleName() != null ? pj.getModuleName() : "") %></p>
                    <p class="pick-meta"><span class="job-status-text">(<%= escHtml(pj.getStatus()) %>)</span>
                        <% if (moPastJobsPage && pj.getDeadline() != null && !pj.getDeadline().isEmpty()) { %>
                        <span class="muted-inline"> · Deadline <%= escHtml(pj.getDeadline()) %></span>
                        <% } %>
                    </p>
                    <p class="pick-stats">App <span class="tab-count"><%= pr.size() %></span> · Int <span class="tab-count"><%= ir.size() %></span> · Wl <span class="tab-count"><%= wlr.size() %></span> · Wdn <span class="tab-count"><%= wdr.size() %></span> · Out <span class="tab-count"><%= ou.size() %></span></p>
                    <p class="mo-job-pick-actions">
                        <a class="btn btn-primary" href="<%= pickHref %>">Manage applicants</a>
                        <a class="btn btn-secondary" href="<%= detailHref %>">Full posting</a>
                    </p>
                </div>
            <% } %>
            </div>
            <% } %>
            <% } else { %>
            <p class="breadcrumb-row"><a href="<%= moBase %>" class="mini-link">&larr; Back to posting list</a></p>
            <% Job hdr = jobsWithApps.isEmpty() ? null : (Job) ((Object[]) jobsWithApps.get(0))[0]; %>
            <header class="ta-page-header">
                <p class="ta-page-kicker">Manage posting</p>
                <h1><%= hdr != null ? escHtml(hdr.getTitle()) : "Job management" %></h1>
                <% if (hdr != null) { %><p class="pick-meta"><%= escHtml(hdr.getModuleCode()) %> · <%= escHtml(hdr.getModuleName() != null ? hdr.getModuleName() : "") %></p><% } %>
                <p class="ta-page-lead">Manage one posting end-to-end: screen applicants, send interview notices, and record final outcomes.</p>
            </header>
            <% if (hdr != null) { %>
            <div class="context-card mo-posting-detail-card">
                <strong>Full posting (all fields from the publish form)</strong>
                <p class="muted-inline">Description, responsibilities, pay, deadline, skills, hours, timeline, interview info, planned recruits, and waitlist settings are listed on one page.</p>
                <p class="mo-posting-detail-actions">
                    <a class="btn btn-primary" href="<%= moCtx %>/mo/job?jobId=<%= java.net.URLEncoder.encode(hdr.getId(), "UTF-8") %>">View full posting</a>
                </p>
            </div>
            <% } %>
            <% if (hdr != null) {
                   int plannedRecruits = hdr.getTaSlots() > 0 ? hdr.getTaSlots() : 1;
                   WorkQuotaPlanner.Recommendation quotaRec = WorkQuotaPlanner.recommend(hdr.getWorkArrangements(), plannedRecruits);
                   List<String[]> weekMilestones = new ArrayList<>();
                   String timelineRaw = hdr.getExamTimeline() != null ? hdr.getExamTimeline() : "";
                   Matcher weekMatcher = Pattern.compile("(?:Week|W)\\s*(\\d{1,2})\\s*[:\\-]?\\s*([^;\\n]+)?", Pattern.CASE_INSENSITIVE).matcher(timelineRaw);
                   while (weekMatcher.find()) {
                       String weekNo = weekMatcher.group(1);
                       String detail = weekMatcher.group(2) != null ? weekMatcher.group(2).trim() : "";
                       weekMilestones.add(new String[]{weekNo, detail});
                   }
                   if (weekMilestones.isEmpty() && !timelineRaw.trim().isEmpty()) {
                       String[] fallback = timelineRaw.split("[;\\n]+");
                       int wk = 1;
                       for (String f : fallback) {
                           String t = f != null ? f.trim() : "";
                           if (t.isEmpty()) continue;
                           weekMilestones.add(new String[]{String.valueOf(wk), t});
                           wk += 3;
                       }
                   }
            %>
            <div class="context-card">
                <strong>Recruitment arrangement</strong>
                <table class="arrangement-table">
                    <tbody>
                    <tr>
                        <th><span class="arr-icon arr-icon-slots" aria-hidden="true">TA</span>Planned recruits</th>
                        <td><%= plannedRecruits %></td>
                    </tr>
                    <tr>
                        <th><span class="arr-icon arr-icon-timeline" aria-hidden="true">TL</span>Course timeline</th>
                        <td>
                            <% if (weekMilestones.isEmpty()) { %>
                            <p class="pre-wrap"><%= escHtml(hdr.getExamTimeline() != null ? hdr.getExamTimeline() : "Not set.") %></p>
                            <% } else { %>
                            <div class="week-timeline-list">
                                <% for (String[] item : weekMilestones) {
                                       int weekNum = 1;
                                       try { weekNum = Integer.parseInt(item[0]); } catch (Exception ignored) {}
                                       int progress = Math.max(0, Math.min(100, (int) Math.round((weekNum / 14.0) * 100)));
                                %>
                                <div class="week-timeline-row">
                                    <div class="week-line">
                                        <span class="week-label">W<%= weekNum %></span>
                                        <span class="week-progress"><span class="week-progress-fill" style="--week-p: <%= progress %>;"></span></span>
                                    </div>
                                    <div class="week-desc"><%= escHtml(item[1] != null && !item[1].isEmpty() ? item[1] : "Milestone") %></div>
                                </div>
                                <% } %>
                            </div>
                            <% } %>
                        </td>
                    </tr>
                    <tr>
                        <th><span class="arr-icon arr-icon-interview" aria-hidden="true">IV</span>Estimated interview time</th>
                        <td class="pre-wrap"><%= escHtml(hdr.getInterviewSchedule() != null ? hdr.getInterviewSchedule() : "Not set.") %></td>
                    </tr>
                    <tr>
                        <th><span class="arr-icon arr-icon-location" aria-hidden="true">LOC</span>Estimated interview location</th>
                        <td class="pre-wrap"><%= escHtml(hdr.getInterviewLocation() != null ? hdr.getInterviewLocation() : "Not set.") %></td>
                    </tr>
                    </tbody>
                </table>
                <p class="muted-inline job-wa-edit-hint">
                    Balanced by planned recruits: total estimated workload <strong><%= String.format(Locale.US, "%.2f", quotaRec.getTotalHours()) %> h</strong>,
                    average per selected TA <strong><%= String.format(Locale.US, "%.2f", quotaRec.getAverageHours()) %> h</strong>.
                    Selected TAs share one common role for this posting.
                </p>
            </div>
            <% } %>
            <div class="context-card">
                <strong>Workflow</strong>
                <p>Use the tabs: Applicants &rarr; Interview &rarr; Waitlist &rarr; Withdrawn &rarr; Outcomes (selected, rejected, and other final states). This page shows only <strong>this posting</strong>.
                <% if (moPastJobsPage) { %><span class="muted-inline"> (Closed or past deadline; this page is read-only.)</span><% } %>
                </p>
            </div>
            <nav class="mo-jobs-tabs" aria-label="Application views">
                <a href="<%= moBase %>?view=pending<%= moJobIdQ %>" class="mo-jobs-tab <%= "pending".equals(moView) ? "active" : "" %>">Applicants<span class="tab-count"><%= moCntP %></span></a>
                <a href="<%= moBase %>?view=interview<%= moJobIdQ %>" class="mo-jobs-tab <%= "interview".equals(moView) ? "active" : "" %>">Interview<span class="tab-count"><%= moCntI %></span></a>
                <a href="<%= moBase %>?view=waitlist<%= moJobIdQ %>" class="mo-jobs-tab <%= "waitlist".equals(moView) ? "active" : "" %>">Waitlist<span class="tab-count"><%= moCntWl %></span></a>
                <a href="<%= moBase %>?view=withdrawn<%= moJobIdQ %>" class="mo-jobs-tab <%= "withdrawn".equals(moView) ? "active" : "" %>">Withdrawn<span class="tab-count"><%= moCntW %></span></a>
                <a href="<%= moBase %>?view=outcome<%= moJobIdQ %>" class="mo-jobs-tab <%= "outcome".equals(moView) ? "active" : "" %>">Outcomes<span class="tab-count"><%= moCntO %></span></a>
            </nav>
            <p class="ai-hint mo-ai-hint"><strong>AI hint</strong>: Within each group, applicants are sorted by match score and workload.</p>
            <p><a href="${pageContext.request.contextPath}/mo/post-job" class="btn btn-primary">Post New Job</a></p>

            <% for (Object[] row : jobsWithApps) {
                Job j = (Job) row[0];
                List<AIMatchService.ApplicantRecommendation> pendingRecs = (List<AIMatchService.ApplicantRecommendation>) row[1];
                List<AIMatchService.ApplicantRecommendation> interviewRecs = (List<AIMatchService.ApplicantRecommendation>) row[2];
                List<AIMatchService.ApplicantRecommendation> waitlistRecs = (List<AIMatchService.ApplicantRecommendation>) row[3];
                List<AIMatchService.ApplicantRecommendation> withdrawnRecs = (List<AIMatchService.ApplicantRecommendation>) row[4];
                List<AIMatchService.ApplicantRecommendation> outcomeRecs = (List<AIMatchService.ApplicantRecommendation>) row[5];
                if (pendingRecs == null) pendingRecs = java.util.Collections.emptyList();
                if (interviewRecs == null) interviewRecs = java.util.Collections.emptyList();
                if (waitlistRecs == null) waitlistRecs = java.util.Collections.emptyList();
                if (withdrawnRecs == null) withdrawnRecs = java.util.Collections.emptyList();
                if (outcomeRecs == null) outcomeRecs = java.util.Collections.emptyList();
                int totalApps = pendingRecs.size() + interviewRecs.size() + waitlistRecs.size() + withdrawnRecs.size() + outcomeRecs.size();
                String batchPendingFormId = "batch_pending_" + j.getId().replaceAll("[^A-Za-z0-9]", "_");
                String batchNoticeFormId = "batch_notice_" + j.getId().replaceAll("[^A-Za-z0-9]", "_");
            %>
            <div class="job-card mo-job-manage-card">
                <div class="job-card-head">
                    <div class="job-card-title">
                        <h3><%= j.getTitle() %> - <%= j.getModuleCode() %> <span class="job-status-text">(<%= j.getStatus() %>)</span></h3>
                        <% if (j.getJobType() != null && !j.getJobType().isEmpty()) { %>
                        <p class="skills">Type: <%= "MODULE_TA".equals(j.getJobType()) ? "Module TA" : "INVIGILATION".equals(j.getJobType()) ? "Invigilation" : "Other" %></p>
                        <% } %>
                    </div>
                    <div class="job-card-actions">
                        <a href="<%= moCtx %>/mo/job?jobId=<%= java.net.URLEncoder.encode(j.getId(), "UTF-8") %>" class="btn btn-primary">Full detail</a>
                        <% if ("OPEN".equals(j.getStatus())) { %>
                        <form action="${pageContext.request.contextPath}/mo/close-job" method="post">
                            <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                            <input type="hidden" name="jobId" value="<%= j.getId() %>">
                            <input type="hidden" name="action" value="close">
                            <button type="submit" class="btn btn-danger">Close Job</button>
                        </form>
                        <% } else {
                               boolean reopenNeedsDeadline = JobActivity.isApplicationDeadlinePast(j.getDeadline());
                        %>
                        <form action="${pageContext.request.contextPath}/mo/close-job" method="post" class="mo-reopen-form">
                            <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                            <input type="hidden" name="jobId" value="<%= j.getId() %>">
                            <input type="hidden" name="action" value="reopen">
                            <% if (reopenNeedsDeadline) { %>
                            <label class="mo-reopen-deadline-label">New application deadline
                                <input type="date" name="newDeadline" class="note-input" required aria-required="true">
                            </label>
                            <% } %>
                            <button type="submit" class="btn btn-primary"><%= reopenNeedsDeadline ? "Reopen with new deadline" : "Reopen" %></button>
                        </form>
                        <% } %>
                    </div>
                </div>

                <p class="job-description"><%= j.getDescription() != null ? j.getDescription() : "" %></p>
                <% if (j.getRequiredSkills() != null && !j.getRequiredSkills().isEmpty()) { %>
                <p class="skills">Required: <%= String.join(", ", j.getRequiredSkills()) %></p>
                <% } %>

                <div class="applicants-panel">
                    <div class="applicants-head">
                        <h4>
                            <% if ("pending".equals(moView)) { %>Applicants<% } else if ("interview".equals(moView)) { %>Interview<% } else if ("waitlist".equals(moView)) { %>Waitlist<% } else if ("withdrawn".equals(moView)) { %>Withdrawn<% } else { %>Outcomes<% } %>
                            <span class="job-apps-count">(this posting: <%= "pending".equals(moView) ? pendingRecs.size() : "interview".equals(moView) ? interviewRecs.size() : "waitlist".equals(moView) ? waitlistRecs.size() : "withdrawn".equals(moView) ? withdrawnRecs.size() : outcomeRecs.size() %>)</span>
                        </h4>
                    </div>

                    <% if (totalApps == 0) { %>
                    <div class="empty-applicants-card">No applications yet.</div>
                    <% } else if ("pending".equals(moView)) { %>
                    <% if (!moReadOnly) { %><form id="<%= batchPendingFormId %>" action="${pageContext.request.contextPath}/mo/batch-applicants" method="post" class="batch-form-hidden">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="action" value="toInterview">
                        <input type="hidden" name="returnJobId" value="<%= j.getId() %>">
                    </form><% } %>
                    <% if (!pendingRecs.isEmpty()) { %>
                    <% if (moReadOnly) { %>
                    <p class="muted-inline section-empty">Read-only: this posting is inactive, so applicant actions are disabled.</p>
                    <% } else { %><p class="batch-toolbar">
                        <button type="submit" class="btn btn-primary" form="<%= batchPendingFormId %>">Set selected to interview (batch)</button>
                    </p><% } %>
                    <% for (AIMatchService.ApplicantRecommendation rec : pendingRecs) {
                        Application a = rec.application;
                        InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
                        String appliedText = a.getAppliedAt() != null ? a.getAppliedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-";
                        String applicantName = a.getApplicantName() != null ? a.getApplicantName() : a.getApplicantId();
                        boolean hasProfile = rec.profile != null;
                        String emailDisp = hasProfile && rec.profile.getEmail() != null && !rec.profile.getEmail().isEmpty() ? escHtml(rec.profile.getEmail()) : "";
                        String skillsText = hasProfile && rec.profile.getSkills() != null && !rec.profile.getSkills().isEmpty() ? String.join(", ", rec.profile.getSkills()) : "Not provided";
                        String missingText = rec.matchResult.missing != null && !rec.matchResult.missing.isEmpty() ? String.join(", ", rec.matchResult.missing) : "No major gaps";
                        boolean hasCv = hasProfile && rec.profile.getCvFilePath() != null && !rec.profile.getCvFilePath().isEmpty();
                        String degreeText = hasProfile && rec.profile.getDegree() != null && !rec.profile.getDegree().isEmpty() ? escHtml(rec.profile.getDegree()) : "-";
                        String programmeText = hasProfile && rec.profile.getProgramme() != null && !rec.profile.getProgramme().isEmpty() ? escHtml(rec.profile.getProgramme()) : "-";
                        String taExpText = hasProfile && rec.profile.getTaExperience() != null && !rec.profile.getTaExperience().isEmpty() ? escHtml(rec.profile.getTaExperience()) : "Not provided.";
                        String templateId = "applicant-tpl-" + j.getId() + "-" + a.getId();
                        String noteText = a.getNotes() != null && !a.getNotes().isEmpty() ? a.getNotes() : "No notes saved for this application.";
                        String profileStateText;
                        if (!hasProfile) {
                            profileStateText = "No profile submitted yet.";
                        } else if (hasCv) {
                            profileStateText = "Profile available, CV uploaded.";
                        } else {
                            profileStateText = "Profile available, CV missing.";
                        }
                    %>
                    <article class="applicant-card">
                        <% if (!moReadOnly) { %><label class="batch-check-label">
                            <input type="checkbox" name="applicationId" value="<%= a.getId() %>" class="batch-checkbox" form="<%= batchPendingFormId %>">
                            <span class="batch-check-hint">Include in batch</span>
                        </label><% } %>
                        <div class="applicant-topline">
                            <div class="applicant-title-group">
                                <div class="applicant-name-row">
                                    <h5><%= applicantName %></h5>
                                    <% if (rec.workloadBalanced) { %>
                                    <span class="load-badge" title="Lower workload">Low load</span>
                                    <% } %>
                                </div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="muted-inline applicant-email-line"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <div class="applicant-links">
                                    <button type="button" class="btn btn-primary applicant-quick-btn" data-template="<%= templateId %>">Quick view</button>
                                    <a href="${pageContext.request.contextPath}/mo/applicant-detail?applicantId=<%= a.getApplicantId() %>" class="mini-link">Full profile</a>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener" class="mini-link">View CV</a>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1" class="mini-link">Download CV</a>
                                    <% } else { %>
                                    <span class="muted-inline">CV not uploaded</span>
                                    <% } %>
                                </div>
                            </div>
                            <div class="applicant-score-area">
                                <span class="match-badge" title="<%= rec.matchResult.explanation %>"><%= (int)rec.matchResult.score %>% match</span>
                                <span class="status-pill status-pill-pending">PENDING</span>
                            </div>
                        </div>
                        <div class="applicant-sections applicant-sections-compact">
                            <section class="applicant-section">
                                <div class="section-label">Profile</div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="section-copy"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p class="section-copy"><strong>Skills:</strong> <%= skillsText %></p>
                                <p class="section-copy muted-inline"><%= profileStateText %></p>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">AI Review</div>
                                <p class="section-copy"><strong>Missing skills:</strong> <%= missingText %></p>
                                <p class="section-copy"><strong>Workload:</strong> <%= rec.currentWorkload %> jobs</p>
                                <% if (Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
                                <div class="ai-summary-actions">
                                    <button type="button" class="btn btn-secondary btn-sm ai-summary-generate-btn" data-application-id="<%= escHtml(a.getId()) %>">Generate AI summary</button>
                                </div>
                                <div class="ai-summary-result" data-application-id="<%= escHtml(a.getId()) %>"></div>
                                <% } %>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">Application</div>
                                <p class="section-copy"><strong>Applied:</strong> <%= appliedText %></p>
                            </section>
                        </div>
                        <div class="applicant-actions">
                            <% if (moReadOnly) { %>
                            <div class="decision-bar decision-bar-recorded"><p class="muted-inline">Read-only: no actions available for inactive postings.</p></div>
                            <% } else { %><div class="decision-bar">
                                <form action="${pageContext.request.contextPath}/mo/select-applicant" method="post" class="decision-form decision-form-inline">
                                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                                    <input type="hidden" name="applicationId" value="<%= a.getId() %>">
                                    <input type="text" name="notes" placeholder="Optional notes" class="note-input">
                                    <div class="decision-buttons decision-buttons-inline">
                                        <button type="submit" name="action" value="interview" class="btn btn-primary decision-btn">Move to interview</button>
                                        <button type="submit" name="action" value="reject" class="btn btn-danger decision-btn">Reject</button>
                                    </div>
                                </form>
                            </div><% } %>
                        </div>
                        <template id="<%= templateId %>">
                            <div class="quick-detail-sheet">
                                <p class="quick-detail-name"><%= escHtml(applicantName) %></p>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p><strong>Degree:</strong> <%= degreeText %></p>
                                <p><strong>Programme:</strong> <%= programmeText %></p>
                                <p><strong>Skills:</strong> <%= escHtml(skillsText) %></p>
                                <div class="detail-block-text">
                                    <strong>TA experience</strong>
                                    <p class="pre-wrap"><%= taExpText %></p>
                                </div>
                                <p><strong>CV:</strong>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener">View</a>
                                    <span class="muted-inline"> | </span>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1">Download</a>
                                    <% } else { %>
                                    <span class="muted-inline">Not uploaded</span>
                                    <% } %>
                                </p>
                            </div>
                        </template>
                    </article>
                    <% } %>
                    <% if (!moReadOnly) { %><p class="batch-toolbar">
                        <button type="submit" class="btn btn-primary" form="<%= batchPendingFormId %>">Set selected to interview (batch)</button>
                    </p><% } %>
                    <% } else { %><p class="muted-inline section-empty">No applicants for this posting.</p><% } %>
                    <% } else if ("interview".equals(moView)) { %>
                    <% if (!interviewRecs.isEmpty()) { %>
                    <% if (!moReadOnly) { %><form id="<%= batchNoticeFormId %>" action="${pageContext.request.contextPath}/mo/batch-applicants" method="post" class="notice-form-fields">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="action" value="sendNotice">
                        <input type="hidden" name="returnJobId" value="<%= j.getId() %>">
                        <div class="notice-fields notice-fields--grid">
                            <label class="notice-field">Interview date
                                <input type="date" name="interviewDate" required class="note-input notice-input-wide">
                            </label>
                            <label class="notice-field">Interview time
                                <input type="time" name="interviewClock" required class="note-input notice-input-wide">
                            </label>
                            <label class="notice-field notice-field--full">Location <input type="text" name="interviewLocation" placeholder="Room or online link" class="note-input notice-input-wide"></label>
                            <label class="notice-field notice-field--full">Assessment <textarea name="interviewAssessment" rows="2" placeholder="Scope, format, etc." class="note-input notice-textarea"></textarea></label>
                        </div>
                    </form>
                    <p class="batch-toolbar">
                        <button type="submit" class="btn btn-success" form="<%= batchNoticeFormId %>">Send/update in-app interview notice</button>
                    </p>
                    <% } else { %>
                    <p class="muted-inline section-empty">Read-only: interview notices and decisions are disabled for inactive postings.</p>
                    <% } %>
                    <% for (AIMatchService.ApplicantRecommendation rec : interviewRecs) {
                        Application a = rec.application;
                        InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
                        String appliedText = a.getAppliedAt() != null ? a.getAppliedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-";
                        String applicantName = a.getApplicantName() != null ? a.getApplicantName() : a.getApplicantId();
                        boolean hasProfile = rec.profile != null;
                        String emailDisp = hasProfile && rec.profile.getEmail() != null && !rec.profile.getEmail().isEmpty() ? escHtml(rec.profile.getEmail()) : "";
                        String skillsText = hasProfile && rec.profile.getSkills() != null && !rec.profile.getSkills().isEmpty() ? String.join(", ", rec.profile.getSkills()) : "Not provided";
                        String missingText = rec.matchResult.missing != null && !rec.matchResult.missing.isEmpty() ? String.join(", ", rec.matchResult.missing) : "No major gaps";
                        boolean hasCv = hasProfile && rec.profile.getCvFilePath() != null && !rec.profile.getCvFilePath().isEmpty();
                        String profileStateText = !hasProfile ? "No profile submitted yet." : (hasCv ? "Profile available, CV uploaded." : "Profile available, CV missing.");
                        boolean hasNotice = (a.getInterviewTime() != null && !a.getInterviewTime().isEmpty())
                                || (a.getInterviewLocation() != null && !a.getInterviewLocation().isEmpty())
                                || (a.getInterviewAssessment() != null && !a.getInterviewAssessment().isEmpty());
                    %>
                    <article class="applicant-card">
                        <% if (!moReadOnly) { %><label class="batch-check-label">
                            <input type="checkbox" name="applicationId" value="<%= a.getId() %>" class="batch-checkbox" form="<%= batchNoticeFormId %>">
                            <span class="batch-check-hint">Select for notice</span>
                        </label><% } %>
                        <div class="applicant-topline">
                            <div class="applicant-title-group">
                                <div class="applicant-name-row">
                                    <h5><%= applicantName %></h5>
                                </div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="muted-inline applicant-email-line"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <div class="applicant-links">
                                    <a href="${pageContext.request.contextPath}/mo/applicant-detail?applicantId=<%= a.getApplicantId() %>" class="mini-link">Full profile</a>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener" class="mini-link">View CV</a>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1" class="mini-link">Download CV</a>
                                    <% } else { %><span class="muted-inline">CV not uploaded</span><% } %>
                                </div>
                            </div>
                            <div class="applicant-score-area">
                                <span class="match-badge" title="<%= rec.matchResult.explanation %>"><%= (int)rec.matchResult.score %>% match</span>
                                <% if (ev != null) { %><span class="match-badge evaluation-badge"><%= ev.getTotalScore() %>/100 eval</span><% } %>
                                <span class="status-pill status-pill-interview">INTERVIEW</span>
                            </div>
                        </div>
                        <% if (hasNotice) { %>
                        <div class="notice-preview">
                            <p><strong>Notice:</strong> Time <%= a.getInterviewTime() != null ? a.getInterviewTime() : "&mdash;" %> |
                                Location <%= a.getInterviewLocation() != null ? a.getInterviewLocation() : "&mdash;" %></p>
                            <% if (a.getInterviewAssessment() != null && !a.getInterviewAssessment().isEmpty()) { %>
                            <p class="muted-inline">Assessment: <%= a.getInterviewAssessment() %></p>
                            <% } %>
                        </div>
                        <% } %>
                        <div class="applicant-sections applicant-sections-compact">
                            <section class="applicant-section">
                                <div class="section-label">Profile</div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="section-copy"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p class="section-copy"><strong>Skills:</strong> <%= skillsText %></p>
                                <p class="section-copy muted-inline"><%= profileStateText %></p>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">AI Review</div>
                                <p class="section-copy"><strong>Missing skills:</strong> <%= missingText %></p>
                                <% if (Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
                                <div class="ai-summary-actions">
                                    <button type="button" class="btn btn-secondary btn-sm ai-summary-generate-btn" data-application-id="<%= escHtml(a.getId()) %>">Generate AI summary</button>
                                </div>
                                <div class="ai-summary-result" data-application-id="<%= escHtml(a.getId()) %>"></div>
                                <% } %>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">Application</div>
                                <p class="section-copy"><strong>Applied:</strong> <%= appliedText %></p>
                            </section>
                        </div>
                        <div class="applicant-actions">
                            <% if (moReadOnly) { %>
                            <div class="decision-bar decision-bar-recorded"><p class="muted-inline">Read-only: no actions available for inactive postings.</p></div>
                            <% } else { %><div class="decision-bar">
                                <form action="${pageContext.request.contextPath}/mo/select-applicant" method="post" class="decision-form decision-form-inline">
                                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                                    <input type="hidden" name="applicationId" value="<%= a.getId() %>">
                                    <input type="text" name="notes" placeholder="Optional notes" class="note-input">
                                    <input type="text" name="decisionReason" placeholder="Selection reason (required if selected)" class="note-input">
                                    <input type="text" name="applicantFeedback" placeholder="TA-visible feedback (optional)" class="note-input">
                                    <div class="decision-buttons decision-buttons-inline">
                                        <button type="submit" name="action" value="select" class="btn btn-success decision-btn">Select</button>
                                        <button type="submit" name="action" value="reject" class="btn btn-danger decision-btn">Reject</button>
                                    </div>
                                </form>
                            </div><% } %>
                        </div>
                    </article>
                    <% } %>
                    <% } else { %><p class="muted-inline section-empty">No interviewees for this posting.</p><% } %>
                    <% } else if ("waitlist".equals(moView)) { %>
                    <% if (!waitlistRecs.isEmpty()) { %>
                    <% for (AIMatchService.ApplicantRecommendation rec : waitlistRecs) {
                        Application a = rec.application;
                        InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
                        String appliedText = a.getAppliedAt() != null ? a.getAppliedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-";
                        String applicantName = a.getApplicantName() != null ? a.getApplicantName() : a.getApplicantId();
                        boolean hasProfile = rec.profile != null;
                        String emailDisp = hasProfile && rec.profile.getEmail() != null && !rec.profile.getEmail().isEmpty() ? escHtml(rec.profile.getEmail()) : "";
                        String skillsText = hasProfile && rec.profile.getSkills() != null && !rec.profile.getSkills().isEmpty() ? String.join(", ", rec.profile.getSkills()) : "Not provided";
                        String missingText = rec.matchResult.missing != null && !rec.matchResult.missing.isEmpty() ? String.join(", ", rec.matchResult.missing) : "No major gaps";
                        boolean hasCv = hasProfile && rec.profile.getCvFilePath() != null && !rec.profile.getCvFilePath().isEmpty();
                        String profileStateText = !hasProfile ? "No profile submitted yet." : (hasCv ? "Profile available, CV uploaded." : "Profile available, CV missing.");
                    %>
                    <article class="applicant-card">
                        <div class="applicant-topline">
                            <div class="applicant-title-group">
                                <div class="applicant-name-row">
                                    <h5><%= applicantName %></h5>
                                </div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="muted-inline applicant-email-line"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <div class="applicant-links">
                                    <a href="${pageContext.request.contextPath}/mo/applicant-detail?applicantId=<%= a.getApplicantId() %>" class="mini-link">Full profile</a>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener" class="mini-link">View CV</a>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1" class="mini-link">Download CV</a>
                                    <% } else { %><span class="muted-inline">CV not uploaded</span><% } %>
                                </div>
                            </div>
                            <div class="applicant-score-area">
                                <span class="match-badge" title="<%= rec.matchResult.explanation %>"><%= (int)rec.matchResult.score %>% match</span>
                                <% if (ev != null) { %><span class="match-badge evaluation-badge"><%= ev.getTotalScore() %>/100 eval</span><% } %>
                                <span class="status-pill status-pill-pending">WAITLIST</span>
                            </div>
                        </div>
                        <div class="applicant-sections applicant-sections-compact">
                            <section class="applicant-section">
                                <div class="section-label">Profile</div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="section-copy"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p class="section-copy"><strong>Skills:</strong> <%= skillsText %></p>
                                <p class="section-copy muted-inline"><%= profileStateText %></p>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">AI Review</div>
                                <p class="section-copy"><strong>Missing skills:</strong> <%= missingText %></p>
                                <% if (Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
                                <div class="ai-summary-actions">
                                    <button type="button" class="btn btn-secondary btn-sm ai-summary-generate-btn" data-application-id="<%= escHtml(a.getId()) %>">Generate AI summary</button>
                                </div>
                                <div class="ai-summary-result" data-application-id="<%= escHtml(a.getId()) %>"></div>
                                <% } %>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">Application</div>
                                <p class="section-copy"><strong>Applied:</strong> <%= appliedText %></p>
                            </section>
                        </div>
                        <div class="applicant-actions">
                            <% if (moReadOnly) { %>
                            <div class="decision-bar decision-bar-recorded"><p class="muted-inline">Read-only: no actions available for inactive postings.</p></div>
                            <% } else { %><div class="decision-bar">
                                <form action="${pageContext.request.contextPath}/mo/select-applicant" method="post" class="decision-form decision-form-inline">
                                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                                    <input type="hidden" name="applicationId" value="<%= a.getId() %>">
                                    <input type="text" name="notes" placeholder="Optional notes" class="note-input">
                                    <input type="text" name="decisionReason" placeholder="Selection reason (required if selected)" class="note-input">
                                    <input type="text" name="applicantFeedback" placeholder="TA-visible feedback (optional)" class="note-input">
                                    <div class="decision-buttons decision-buttons-inline">
                                        <button type="submit" name="action" value="select" class="btn btn-success decision-btn">Select</button>
                                        <button type="submit" name="action" value="reject" class="btn btn-danger decision-btn">Reject</button>
                                    </div>
                                </form>
                            </div><% } %>
                        </div>
                    </article>
                    <% } %>
                    <% } else { %><p class="muted-inline section-empty">No waitlisted applicants for this posting.</p><% } %>
                    <% } else if ("withdrawn".equals(moView)) { %>
                    <% for (AIMatchService.ApplicantRecommendation rec : withdrawnRecs) {
                        Application a = rec.application;
                        InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
                        String appliedText = a.getAppliedAt() != null ? a.getAppliedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-";
                        String applicantName = a.getApplicantName() != null ? a.getApplicantName() : a.getApplicantId();
                    %>
                    <article class="applicant-card applicant-card-withdrawn">
                        <div class="applicant-topline">
                            <div class="applicant-title-group">
                                <h5><%= applicantName %></h5>
                                <p class="muted-inline">Application withdrawn</p>
                            </div>
                            <span class="status-pill status-pill-rejected">WITHDRAWN</span>
                        </div>
                        <p class="section-copy"><strong>Applied:</strong> <%= appliedText %></p>
                    </article>
                    <% } %>
                    <% if (withdrawnRecs.isEmpty()) { %><p class="muted-inline section-empty">No withdrawn applications for this posting.</p><% } %>
                    <% } else { %>
                    <% for (AIMatchService.ApplicantRecommendation rec : outcomeRecs) {
                        Application a = rec.application;
                        InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
                        String appliedText = a.getAppliedAt() != null ? a.getAppliedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-";
                        String applicantName = a.getApplicantName() != null ? a.getApplicantName() : a.getApplicantId();
                        boolean hasProfile = rec.profile != null;
                        String emailDisp = hasProfile && rec.profile.getEmail() != null && !rec.profile.getEmail().isEmpty() ? escHtml(rec.profile.getEmail()) : "";
                        String skillsText = hasProfile && rec.profile.getSkills() != null && !rec.profile.getSkills().isEmpty() ? String.join(", ", rec.profile.getSkills()) : "Not provided";
                        String missingText = rec.matchResult.missing != null && !rec.matchResult.missing.isEmpty() ? String.join(", ", rec.matchResult.missing) : "No major gaps";
                        boolean hasCv = hasProfile && rec.profile.getCvFilePath() != null && !rec.profile.getCvFilePath().isEmpty();
                        String profileStateText = !hasProfile ? "No profile submitted yet." : (hasCv ? "Profile available, CV uploaded." : "Profile available, CV missing.");
                        String noteText = a.getNotes() != null && !a.getNotes().isEmpty() ? a.getNotes() : "No notes saved for this application.";
                        String statusPillClass = "SELECTED".equals(a.getStatus()) ? "status-pill status-pill-selected" : "status-pill status-pill-rejected";
                        String degreeText = hasProfile && rec.profile.getDegree() != null && !rec.profile.getDegree().isEmpty() ? escHtml(rec.profile.getDegree()) : "-";
                        String programmeText = hasProfile && rec.profile.getProgramme() != null && !rec.profile.getProgramme().isEmpty() ? escHtml(rec.profile.getProgramme()) : "-";
                        String taExpText = hasProfile && rec.profile.getTaExperience() != null && !rec.profile.getTaExperience().isEmpty() ? escHtml(rec.profile.getTaExperience()) : "Not provided.";
                        String templateId = "applicant-outcome-" + j.getId() + "-" + a.getId();
                    %>
                    <article class="applicant-card">
                        <div class="applicant-topline">
                            <div class="applicant-title-group">
                                <div class="applicant-name-row">
                                    <h5><%= applicantName %></h5>
                                </div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="muted-inline applicant-email-line"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <div class="applicant-links">
                                    <button type="button" class="btn btn-primary applicant-quick-btn" data-template="<%= templateId %>">Quick view</button>
                                    <a href="${pageContext.request.contextPath}/mo/applicant-detail?applicantId=<%= a.getApplicantId() %>" class="mini-link">Full profile</a>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener" class="mini-link">View CV</a>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1" class="mini-link">Download CV</a>
                                    <% } else { %>
                                    <span class="muted-inline">CV not uploaded</span>
                                    <% } %>
                                </div>
                            </div>
                            <div class="applicant-score-area">
                                <span class="match-badge" title="<%= rec.matchResult.explanation %>"><%= (int)rec.matchResult.score %>% match</span>
                                <% if (ev != null) { %><span class="match-badge evaluation-badge"><%= ev.getTotalScore() %>/100 eval</span><% } %>
                                <span class="<%= statusPillClass %>"><%= a.getStatus() %></span>
                            </div>
                        </div>
                        <div class="applicant-sections applicant-sections-compact">
                            <section class="applicant-section">
                                <div class="section-label">Profile</div>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p class="section-copy"><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p class="section-copy"><strong>Skills:</strong> <%= skillsText %></p>
                                <p class="section-copy muted-inline"><%= profileStateText %></p>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">AI Review</div>
                                <p class="section-copy"><strong>Missing skills:</strong> <%= missingText %></p>
                                <% if (Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
                                <div class="ai-summary-actions">
                                    <button type="button" class="btn btn-secondary btn-sm ai-summary-generate-btn" data-application-id="<%= escHtml(a.getId()) %>">Generate AI summary</button>
                                </div>
                                <div class="ai-summary-result" data-application-id="<%= escHtml(a.getId()) %>"></div>
                                <% } %>
                            </section>
                            <section class="applicant-section">
                                <div class="section-label">Application</div>
                                <p class="section-copy"><strong>Applied:</strong> <%= appliedText %></p>
                            </section>
                        </div>
                        <div class="applicant-actions">
                            <div class="decision-bar decision-bar-recorded">
                                <div class="decision-bar-copy">
                                    <div class="section-label">Decision recorded</div>
                                    <p>Status: <strong><%= a.getStatus() %></strong></p>
                                </div>
                                <div class="decision-summary">
                                    <p><strong>Notes:</strong> <%= noteText %></p>
                                    <% if (a.getDecisionReason() != null && !a.getDecisionReason().isEmpty()) { %><p><strong>Decision reason:</strong> <%= escHtml(a.getDecisionReason()) %></p><% } %>
                                    <% if (a.getApplicantFeedback() != null && !a.getApplicantFeedback().isEmpty()) { %><p><strong>TA feedback:</strong> <%= escHtml(a.getApplicantFeedback()) %></p><% } %>
                                </div>
                            </div>
                        </div>
                        <template id="<%= templateId %>">
                            <div class="quick-detail-sheet">
                                <p class="quick-detail-name"><%= escHtml(applicantName) %></p>
                                <% if (!emailDisp.isEmpty()) { %>
                                <p><strong>Email:</strong> <%= emailDisp %></p>
                                <% } %>
                                <p><strong>Degree:</strong> <%= degreeText %></p>
                                <p><strong>Programme:</strong> <%= programmeText %></p>
                                <p><strong>Skills:</strong> <%= escHtml(skillsText) %></p>
                                <div class="detail-block-text">
                                    <strong>TA experience</strong>
                                    <p class="pre-wrap"><%= taExpText %></p>
                                </div>
                                <p><strong>CV:</strong>
                                    <% if (hasCv) { %>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>" target="_blank" rel="noopener">View</a>
                                    <span class="muted-inline"> | </span>
                                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= a.getApplicantId() %>&amp;download=1">Download</a>
                                    <% } else { %>
                                    <span class="muted-inline">Not uploaded</span>
                                    <% } %>
                                </p>
                            </div>
                        </template>
                    </article>
                    <% } %>
                    <% if (outcomeRecs.isEmpty()) { %><p class="muted-inline section-empty">No outcomes recorded for this posting yet.</p><% } %>
                    <% } %>
                </div>
            </div>
            <% }
               if (jobsWithApps.isEmpty()) { %>
            <p class="error">Could not load this posting. <a href="<%= moBase %>">Back to list</a></p>
            <% } %>
            <% } %>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card">
                <div class="widget-title">MO Dashboard</div>
                <p class="widget-line">Applicants &rarr; Interview &rarr; In-app notice &rarr; Select / reject</p>
            </div>
        </aside>
    </div>
</div>

<dialog id="applicantQuickDialog" class="applicant-quick-dialog">
    <div class="applicant-quick-dialog-inner">
        <div class="applicant-quick-dialog-head">
            <h3>Applicant detail</h3>
            <button type="button" class="dialog-close-btn" aria-label="Close">&times;</button>
        </div>
        <div class="applicant-quick-dialog-body"></div>
    </div>
</dialog>
<script>
(function () {
    var dialog = document.getElementById('applicantQuickDialog');
    if (!dialog) return;
    var body = dialog.querySelector('.applicant-quick-dialog-body');
    var closeBtn = dialog.querySelector('.dialog-close-btn');
    if (closeBtn) closeBtn.addEventListener('click', function () { dialog.close(); });
    dialog.addEventListener('click', function (e) { if (e.target === dialog) dialog.close(); });
    document.querySelectorAll('.applicant-quick-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var id = btn.getAttribute('data-template');
            var tpl = id ? document.getElementById(id) : null;
            if (body) body.innerHTML = '';
            if (tpl && tpl.content && body) body.appendChild(tpl.content.cloneNode(true));
            dialog.showModal();
        });
    });
})();
</script>
<script src="<%= moCtx %>/js/llm-stream.js"></script>
<script>
(function () {
    function escapeHtml(str) {
        return (str || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function renderLines(resultBox, lines) {
        if (!resultBox) return;
        if (!lines || !lines.length) {
            resultBox.innerHTML = "<p class='section-copy muted-inline'>No summary generated.</p>";
            return;
        }
        var html = "<p class='section-copy'><strong>AI summary card:</strong></p><ul class='applicant-ai-summary-list'>";
        lines.forEach(function (line) {
            html += "<li>" + escapeHtml(line) + "</li>";
        });
        html += "</ul>";
        resultBox.innerHTML = html;
    }

    function normalizeRawText(text) {
        if (!text) return [];
        return text.split(/\r?\n/)
            .map(function (s) {
                return s.replace(/^\s*[\-\*\u2022]\s*/, "")
                        .replace(/^\s*\d+[\.\)]\s*/, "")
                        .trim();
            })
            .filter(function (s) { return s.length > 0; });
    }

    document.querySelectorAll(".ai-summary-generate-btn").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var appId = btn.getAttribute("data-application-id");
            if (!appId) return;
            var resultBox = document.querySelector(".ai-summary-result[data-application-id='" + appId + "']");
            if (resultBox) {
                resultBox.innerHTML = "<p class='section-copy muted-inline'>Generating summary...</p>";
            }
            btn.disabled = true;
            var oldText = btn.textContent;
            btn.textContent = "Generating...";

            var url = "<%= moCtx %>/mo/applicant-summary?applicationId=" + encodeURIComponent(appId);
            LlmStream.call(url, {
                onChunk: function (accumulated) {
                    renderLines(resultBox, normalizeRawText(accumulated));
                },
                onDone: function (accumulated) {
                    renderLines(resultBox, normalizeRawText(accumulated));
                    btn.textContent = "Regenerate AI summary";
                    btn.disabled = false;
                },
                onJson: function (data) {
                    renderLines(resultBox, data.lines || []);
                    btn.textContent = "Regenerate AI summary";
                    btn.disabled = false;
                },
                onError: function (msg) {
                    if (resultBox) {
                        resultBox.innerHTML = "<p class='section-copy error'>AI summary failed: " + escapeHtml(msg || "Unknown error") + "</p>";
                    }
                    btn.textContent = oldText;
                    btn.disabled = false;
                }
            });
        });
    });
})();
</script>
<%@ include file="/WEB-INF/jspf/login-celebration.jspf" %>
</body>
</html>
