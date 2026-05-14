<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.Application" %>
<%@ page import="bupt.ta.model.InterviewEvaluation" %>
<%@ page import="bupt.ta.model.WorkArrangementItem" %>
<%@ page import="bupt.ta.service.InterviewBookingService.SlotSummary" %>
<%@ page import="bupt.ta.util.WorkQuotaPlanner" %>
<%@ page import="bupt.ta.util.WorkArrangementSupport" %>
<%@ page import="bupt.ta.util.JobActivity" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%
    Job job = (Job) request.getAttribute("job");
    if (job == null) {
        response.sendRedirect(request.getContextPath() + "/mo/jobs?error=invalid_job");
        return;
    }
    String moListPath = (String) request.getAttribute("moListPath");
    if (moListPath == null) moListPath = "/mo/jobs";
    boolean moPastJobsPage = Boolean.TRUE.equals(request.getAttribute("moPastJobsPage"));
    String moCtx = request.getContextPath();
    String manageHref = moCtx + moListPath + "?jobId=" + java.net.URLEncoder.encode(job.getId(), "UTF-8") + "&view=pending";

    boolean isOpen = "OPEN".equals(job.getStatus());
    String moduleName = job.getModuleName() != null && !job.getModuleName().isEmpty() ? escHtml(job.getModuleName()) : "&mdash;";
    String wh = job.getWorkingHours() != null && !job.getWorkingHours().isEmpty() ? escHtml(job.getWorkingHours()) : "&mdash;";
    String wl = job.getWorkload() != null && !job.getWorkload().isEmpty() ? escHtml(job.getWorkload()) : "&mdash;";
    String pay = job.getPayment() != null && !job.getPayment().isEmpty() ? escHtml(job.getPayment()) : "&mdash;";
    String deadline = job.getDeadline() != null && !job.getDeadline().isEmpty() ? escHtml(job.getDeadline()) : "&mdash;";
    String examTimeline = job.getExamTimeline() != null && !job.getExamTimeline().isEmpty() ? escHtml(job.getExamTimeline()) : "&mdash;";
    String interviewSchedule = job.getInterviewSchedule() != null && !job.getInterviewSchedule().isEmpty() ? escHtml(job.getInterviewSchedule()) : "&mdash;";
    String interviewLocation = job.getInterviewLocation() != null && !job.getInterviewLocation().isEmpty() ? escHtml(job.getInterviewLocation()) : "&mdash;";
    int plannedRecruits = job.getTaSlots() > 0 ? job.getTaSlots() : 1;
    WorkQuotaPlanner.Recommendation quotaRec = WorkQuotaPlanner.recommend(job.getWorkArrangements(), plannedRecruits);
    List<SlotSummary> slotSummaries = (List<SlotSummary>) request.getAttribute("slotSummaries");
    if (slotSummaries == null) slotSummaries = new ArrayList<>();
    List<Application> candidateRanking = (List<Application>) request.getAttribute("candidateRanking");
    if (candidateRanking == null) candidateRanking = new ArrayList<>();
    Map<String, InterviewEvaluation> evaluationByApplicationId = (Map<String, InterviewEvaluation>) request.getAttribute("evaluationByApplicationId");
    if (evaluationByApplicationId == null) evaluationByApplicationId = java.util.Collections.emptyMap();
    List<String[]> weekMilestones = new ArrayList<>();
    String timelineRaw = job.getExamTimeline() != null ? job.getExamTimeline() : "";
    Matcher weekMatcher = Pattern.compile("(?:Week|W)\\s*(\\d{1,3})\\s*[:\\-–.]?\\s*([^;\\n]+)?", Pattern.CASE_INSENSITIVE).matcher(timelineRaw);
    while (weekMatcher.find()) {
        String weekNo = weekMatcher.group(1);
        String detail = weekMatcher.group(2) != null ? weekMatcher.group(2).trim() : "";
        weekMilestones.add(new String[]{weekNo, escHtml(detail)});
    }
    if (weekMilestones.isEmpty() && !timelineRaw.trim().isEmpty()) {
        String[] fallback = timelineRaw.split("[;\\n]+");
        int wk = 1;
        for (String f : fallback) {
            String t = f != null ? f.trim() : "";
            if (t.isEmpty()) continue;
            weekMilestones.add(new String[]{String.valueOf(wk), escHtml(t)});
            wk += 3;
        }
    }
%>
<%@ include file="/WEB-INF/jspf/job-ta-plan-chunks.jspf" %>
<%
    String respText = job.getResponsibilities() != null && !job.getResponsibilities().isEmpty() ? escHtml(job.getResponsibilities()) : "&mdash;";
    String desc = job.getDescription() != null && !job.getDescription().isEmpty() ? escHtml(job.getDescription()) : "&mdash;";
    String safeTitle = escHtml(job.getTitle() != null ? job.getTitle() : "");
    String createdAt = job.getCreatedAt() != null && !job.getCreatedAt().isEmpty() ? escHtml(job.getCreatedAt()) : "&mdash;";
    String maxAppText = job.getMaxApplicants() <= 0 ? "No limit" : String.valueOf(job.getMaxApplicants());
    request.setAttribute("moNavActive", moPastJobsPage ? "past" : "jobs");
    boolean canEditWorkArrangements = !moPastJobsPage && JobActivity.isActive(job);
    List<WorkArrangementItem> waRowsForEdit = new ArrayList<>();
    if (job.getWorkArrangements() != null) {
        waRowsForEdit.addAll(job.getWorkArrangements());
    }
    if (canEditWorkArrangements && waRowsForEdit.isEmpty()) {
        waRowsForEdit.add(new WorkArrangementItem());
    }
    boolean taPlanSingleUnstructured = !taPlanChunks.isEmpty() && taPlanChunks.size() == 1 && taPlanChunks.get(0)[0] == null;
    boolean hasPersistedWorkRows = job.getWorkArrangements() != null && !job.getWorkArrangements().isEmpty();
    boolean hideMoTaPlanDuplicate = hasPersistedWorkRows && taPlanSingleUnstructured;
    int timelineMaxWeekMo = 14;
    for (String[] item : weekMilestones) {
        try {
            int w = Integer.parseInt(item[0]);
            if (w > timelineMaxWeekMo) {
                timelineMaxWeekMo = w;
            }
        } catch (Exception ignored) { /* keep */ }
    }
    if (timelineMaxWeekMo < 1) {
        timelineMaxWeekMo = 14;
    }
    String[] weekdays = WorkArrangementSupport.weekdays();
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= safeTitle %> - Posting detail</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="nav top-nav">
        <span class="brand">BUPT Teaching Assistant Recruitment System</span>
        <div class="user user-inline-actions"><span><%= session.getAttribute("realName") %> |</span><form action="${pageContext.request.contextPath}/logout" method="post" class="inline-form logout-form"><%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %><button type="submit" class="logout-button">Logout</button></form></div>
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
        <main class="main-panel mo-main mo-page mo-page--mo-job-detail">
            <p class="breadcrumb-row">
                <a href="<%= moCtx %><%= moListPath %>">&larr; Back to posting list</a>
                <span class="muted-inline"> &nbsp;|&nbsp; </span>
                <a href="<%= manageHref %>">Manage applicants</a>
            </p>
            <% String waErr = request.getParameter("error");
               boolean moJobDetailFlash = "1".equals(request.getParameter("posted"))
                       || "1".equals(request.getParameter("workArrangementsUpdated"))
                       || "1".equals(request.getParameter("slotSaved"))
                       || request.getParameter("slotError") != null
                       || "wa_count_mismatch".equals(waErr)
                       || "wa_ta_invalid".equals(waErr)
                       || "planned_ta_invalid".equals(waErr)
                       || "wa_confirm_required".equals(waErr)
                       || "wa_validation".equals(waErr)
                       || "wa_job_inactive".equals(waErr);
               if (moJobDetailFlash) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("posted"))) { %>
            <p class="success">Posting saved. Everything below is the full text you entered (same fields TAs see, plus MO-only limits). When you are ready, open <a href="<%= manageHref %>">Manage applicants</a>.</p>
            <% } %>
            <% if ("1".equals(request.getParameter("workArrangementsUpdated"))) { %>
            <p class="success">Work arrangements and planned recruits were saved. Summary text and quota hints were refreshed.</p>
            <% } %>
            <% if ("1".equals(request.getParameter("slotSaved"))) { %>
            <p class="success">Interview slot updated successfully.</p>
            <% } %>
            <% if ("wa_count_mismatch".equals(waErr)) { %><p class="error">Could not update work arrangements (form mismatch). Please try again.</p><% }
               else if ("wa_ta_invalid".equals(waErr)) { %><p class="error">Each row needs at least 1 TA.</p><% }
               else if ("planned_ta_invalid".equals(waErr)) { %><p class="error">Planned recruits must be at least 1.</p><% }
               else if ("wa_confirm_required".equals(waErr)) { %><p class="error">Please tick both confirmation boxes before saving changes to work arrangements.</p><% }
               else if ("wa_validation".equals(waErr)) { %><p class="error">Fix work arrangement rows: each needs a name, per-session duration, occurrences (≥1), and TA count (≥1).</p><% }
               else if ("wa_job_inactive".equals(waErr)) { %><p class="error">This posting is closed or past deadline; work arrangements cannot be edited here.</p><% } %>
            <% if (request.getParameter("slotError") != null) { %><p class="error"><%= escHtml(request.getParameter("slotError")) %></p><% } %>
            </div>
            <% } %>
            <header class="ta-page-header">
                <p class="ta-page-kicker">Posting detail</p>
                <h1><%= safeTitle %></h1>
                <p class="job-detail-meta">
                    <span class="status-pill <%= isOpen ? "status-pill-pending" : "status-pill-rejected" %>"><%= escHtml(job.getStatus()) %></span>
                    <% if (job.getJobType() != null && !job.getJobType().isEmpty()) { %>
                    <span class="muted-inline">Type: <%= "MODULE_TA".equals(job.getJobType()) ? "Module TA" : "INVIGILATION".equals(job.getJobType()) ? "Invigilation" : "Other" %></span>
                    <% } %>
                </p>
                <p class="ta-page-lead">Full text of this vacancy (what TAs see), plus MO tools below for work arrangements, interview slots, and candidate ranking when you manage the posting.</p>
            </header>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-decision-title">
                <h2 id="mo-job-decision-title" class="ta-job-detail__heading">Candidate decision board</h2>
                <p class="ta-job-detail__lede muted-inline">Interviewed candidates with saved evaluations rise to the top; use this board before final selection.</p>
                <% if (candidateRanking.isEmpty()) { %>
                <p class="section-empty">No applications for this posting yet.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>Rank</th>
                        <th>Applicant</th>
                        <th>Status</th>
                        <th>Evaluation</th>
                        <th>Recommendation</th>
                        <th>Decision reason</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (int ci = 0; ci < candidateRanking.size(); ci++) {
                        Application app = candidateRanking.get(ci);
                        InterviewEvaluation ev = evaluationByApplicationId.get(app.getId());
                    %>
                    <tr>
                        <td><%= ci + 1 %></td>
                        <td><a href="<%= moCtx %>/mo/applicant-detail?applicantId=<%= java.net.URLEncoder.encode(app.getApplicantId(), "UTF-8") %>"><%= escHtml(app.getApplicantName() != null ? app.getApplicantName() : app.getApplicantId()) %></a></td>
                        <td><%= escHtml(app.getStatus()) %></td>
                        <td><% if (ev != null) { %><strong><%= ev.getTotalScore() %>/100</strong><% } else { %><span class="muted-inline">Not evaluated</span><% } %></td>
                        <td><%= ev != null ? escHtml(ev.getRecommendationLabel()) : "-" %></td>
                        <td class="pre-wrap"><%= app.getDecisionReason() != null && !app.getDecisionReason().isEmpty() ? escHtml(app.getDecisionReason()) : "-" %></td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
                </div>
                <% } %>
            </section>

            <div class="ta-job-detail">

            <% if ((job.getWorkArrangements() != null && !job.getWorkArrangements().isEmpty()) || canEditWorkArrangements) { %>
            <section class="ta-job-detail__section" aria-labelledby="mo-job-wa-title">
            <h2 id="mo-job-wa-title" class="ta-job-detail__heading">Work arrangements</h2>
            <% if (canEditWorkArrangements) { %>
            <p class="ta-job-detail__lede muted-inline">While this posting is <strong>open and before the deadline</strong>, edit the table below. Changes refresh workload summaries and smart-quota hints; avoid unnecessary edits.</p>
            <form id="job-detail-wa-form" action="<%= moCtx %>/mo/update-work-ta-counts" method="post" class="form form--mo-post job-detail-wa-form">
                <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                <input type="hidden" name="jobId" value="<%= escHtml(job.getId()) %>">
                <div class="wa-section mo-wa-card">
                    <div class="mo-wa-table-scroll">
                    <table class="wa-edit-table" role="grid" aria-label="Work arrangement rows">
                        <caption class="sr-only">Work arrangement rows, required fields per column header</caption>
                        <thead>
                        <tr>
                            <th scope="col" class="wa-edit-table__col-name">Work name</th>
                            <th scope="col" class="wa-edit-table__col-duration">Per-session duration (hours)</th>
                            <th scope="col" class="wa-edit-table__col-num">Sessions</th>
                            <th scope="col" class="wa-edit-table__col-num">TAs</th>
                            <th scope="col" class="wa-edit-table__col-time">Specific time <span class="muted-inline">(optional)</span></th>
                            <th scope="col" class="wa-edit-table__col-actions"><span class="sr-only">Remove row</span></th>
                        </tr>
                        </thead>
                        <tbody id="job-detail-wa-rows" class="wa-rows wa-rows--edit">
                        <% for (int waIdx = 0; waIdx < waRowsForEdit.size(); waIdx++) {
                               WorkArrangementItem w = waRowsForEdit.get(waIdx);
                               String wid = "jd-wa-" + waIdx;
                               String wn = w.getWorkName() != null ? w.getWorkName() : "";
                               String sd = WorkArrangementSupport.durationHoursInputValue(w);
                               int oc = w.getOccurrenceCount() > 0 ? w.getOccurrenceCount() : (w.getResolvedOccurrenceCount() > 0 ? w.getResolvedOccurrenceCount() : 1);
                               int wc = w.getTaCount() > 0 ? w.getTaCount() : 1;
                               String wt = w.getSpecificTime() != null ? w.getSpecificTime() : "";
                               String wtDay = WorkArrangementSupport.specificDayInputValue(wt);
                               String wtClock = WorkArrangementSupport.specificTimeInputValue(wt);
                        %>
                        <tr class="wa-row" data-wa-row>
                            <td class="wa-edit-table__cell">
                                <label class="sr-only" for="<%= wid %>-wn">Work name</label>
                                <input id="<%= wid %>-wn" type="text" name="waWorkName" required value="<%= escHtml(wn) %>" placeholder="e.g. Lab" autocomplete="off" class="wa-edit-table__input">
                            </td>
                            <td class="wa-edit-table__cell">
                                <label class="sr-only" for="<%= wid %>-sd">Per-session duration</label>
                                <input id="<%= wid %>-sd" type="number" name="waSessionDuration" min="0.25" step="0.25" required value="<%= escHtml(sd) %>" placeholder="2" inputmode="decimal" autocomplete="off" class="wa-edit-table__input">
                            </td>
                            <td class="wa-edit-table__cell wa-edit-table__cell--num">
                                <label class="sr-only" for="<%= wid %>-oc">Sessions</label>
                                <input id="<%= wid %>-oc" type="number" name="waOccurrenceCount" min="1" required value="<%= oc %>" class="wa-input-num wa-edit-table__input">
                            </td>
                            <td class="wa-edit-table__cell wa-edit-table__cell--num">
                                <label class="sr-only" for="<%= wid %>-tc">TAs</label>
                                <input id="<%= wid %>-tc" type="number" name="waTaCount" min="1" required value="<%= wc %>" class="wa-input-num wa-edit-table__input">
                            </td>
                            <td class="wa-edit-table__cell">
                                <label class="sr-only" for="<%= wid %>-st">Specific time</label>
                                <div class="wa-time-controls">
                                    <select id="<%= wid %>-day" name="waSpecificDay" class="wa-edit-table__input">
                                        <option value="">TBD</option>
                                        <% for (String day : weekdays) { %>
                                        <option value="<%= escHtml(day) %>" <%= day.equals(wtDay) ? "selected" : "" %>><%= escHtml(day) %></option>
                                        <% } %>
                                    </select>
                                    <input id="<%= wid %>-st" type="time" name="waSpecificStartTime" value="<%= escHtml(wtClock) %>" autocomplete="off" class="wa-edit-table__input">
                                </div>
                            </td>
                            <td class="wa-edit-table__cell wa-edit-table__cell--actions">
                                <button type="button" class="btn btn-secondary wa-remove-compact" title="Remove row" aria-label="Remove this row">&minus;</button>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    </div>
                    <div class="wa-toolbar">
                        <button type="button" id="job-detail-wa-add-row" class="btn btn-secondary" title="Add work arrangement row">+ Add row</button>
                        <button type="button" id="job-detail-wa-suggest-quota" class="btn btn-secondary" title="Suggest balanced TA quotas">Smart quota recommendation</button>
                    </div>
                    <div id="job-detail-wa-suggestion-panel" class="wa-suggestion-panel" hidden>
                        <p id="job-detail-wa-suggestion-meta" class="muted-inline wa-suggestion-meta"></p>
                        <div id="job-detail-wa-suggestion-list" class="wa-suggestion-list"></div>
                    </div>
                    <div class="mo-wa-form-footer">
                        <div class="mo-wa-planned-row">
                            <label for="job-detail-planned-ta">Planned recruits <abbr title="required">*</abbr></label>
                            <input type="number" name="plannedTaCount" id="job-detail-planned-ta" class="mo-wa-planned-input" min="1" required value="<%= plannedRecruits %>">
                        </div>
                        <div class="mo-wa-confirm-stack">
                            <label class="checkbox-line mo-wa-checkbox-line"><input type="checkbox" name="waConfirmUnderstand" value="1" required> I understand this updates the published workload summary and may affect TA-facing text.</label>
                            <label class="checkbox-line mo-wa-checkbox-line"><input type="checkbox" name="waConfirmAccurate" value="1" required> I confirm the rows above are correct before saving.</label>
                        </div>
                        <div class="mo-wa-save-row">
                            <button type="submit" class="btn btn-primary" id="job-detail-wa-submit">Save work arrangements</button>
                        </div>
                    </div>
                </div>
            </form>
            <% } else if (job.getWorkArrangements() != null && !job.getWorkArrangements().isEmpty()) { %>
            <p class="ta-job-detail__lede muted-inline">Read-only snapshot from when this posting became inactive.</p>
            <table class="job-wa-table">
                <thead>
                <tr>
                    <th scope="col">Work name</th>
                    <th scope="col">Per-session duration</th>
                    <th scope="col">Occurrences</th>
                    <th scope="col">TA count</th>
                    <th scope="col">Specific time</th>
                </tr>
                </thead>
                <tbody>
                <% for (WorkArrangementItem wa : job.getWorkArrangements()) {
                       String wn = wa.getWorkName() != null && !wa.getWorkName().isEmpty() ? escHtml(wa.getWorkName()) : "&mdash;";
                       String sdRaw = wa.getResolvedSessionDuration();
                       String sd = sdRaw != null && !sdRaw.isEmpty() ? escHtml(sdRaw) : "&mdash;";
                       int occ = wa.getResolvedOccurrenceCount();
                       int wc = wa.getTaCount() > 0 ? wa.getTaCount() : 1;
                %>
                <tr>
                    <td><%= wn %></td>
                    <td class="pre-wrap"><%= sd %></td>
                    <td><%= occ %></td>
                    <td><%= wc %></td>
                    <td class="pre-wrap"><% if (wa.getSpecificTime() != null && !wa.getSpecificTime().isEmpty()) { %><%= escHtml(wa.getSpecificTime()) %><% } else { %><span class="muted-inline">TBD &mdash; to be arranged as needed</span><% } %></td>
                </tr>
                <% } %>
                </tbody>
            </table>
            <% } %>
            </section>
            <% } %>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-key-title">
            <h2 id="mo-job-key-title" class="ta-job-detail__heading">Key information</h2>
            <p class="ta-job-detail__lede muted-inline">Posting identifiers, limits, compensation, deadline, and open status (mirrors what TAs see in &ldquo;Key information&rdquo;).</p>
            <dl class="job-detail-dl job-detail-dl--ta-summary">
                <dt>Posting ID</dt><dd><code><%= escHtml(job.getId()) %></code></dd>
                <dt>Created</dt><dd><%= createdAt %></dd>
                <dt>Module</dt>
                <dd><%
                    String mcodeRawMo = job.getModuleCode() != null ? job.getModuleCode().trim() : "";
                    String mnameRawMo = job.getModuleName() != null ? job.getModuleName().trim() : "";
                    if (mcodeRawMo.isEmpty() && mnameRawMo.isEmpty()) { %>&mdash;<% }
                    else if (!mcodeRawMo.isEmpty() && !mnameRawMo.isEmpty()) { %><%= escHtml(mcodeRawMo) %> <span class="muted-inline">&middot;</span> <%= escHtml(mnameRawMo) %><% }
                    else if (!mcodeRawMo.isEmpty()) { %><%= escHtml(mcodeRawMo) %><% }
                    else { %><%= escHtml(mnameRawMo) %><% }
                %></dd>
                <% if (!(hasPersistedWorkRows || canEditWorkArrangements)) { %>
                <dt>Hours / schedule</dt><dd class="pre-wrap"><%= wh %></dd>
                <% } %>
                <dt>Payment</dt><dd class="pre-wrap"><%= pay %></dd>
                <dt>Required skills</dt><dd><%= job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty() ? escHtml(String.join(", ", job.getRequiredSkills())) : "&mdash;" %></dd>
                <% if (!(hasPersistedWorkRows || canEditWorkArrangements)) { %>
                <dt>Workload</dt><dd class="pre-wrap"><%= wl %></dd>
                <% } %>
                <dt>Max applicants</dt><dd><%= maxAppText %></dd>
                <% if (!canEditWorkArrangements) { %>
                <dt>Planned recruits</dt><dd><%= plannedRecruits %></dd>
                <% } %>
                <dt>Auto-fill from waitlist</dt><dd><%= job.isAutoFillFromWaitlist() ? "Yes" : "No" %></dd>
                <dt>Application deadline</dt><dd><%= deadline %></dd>
                <dt>Open status</dt><dd><%= isOpen ? "Open for applications" : "Closed" %></dd>
            </dl>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-role-title">
            <h2 id="mo-job-role-title" class="ta-job-detail__heading">Role &amp; scope</h2>
            <p class="ta-job-detail__lede muted-inline">Published overview and responsibilities (same block as the TA &ldquo;Role &amp; scope&rdquo; view).</p>
            <div class="ta-job-role-unified">
                <% if (job.getDescription() != null && !job.getDescription().trim().isEmpty()) { %>
                <div class="ta-job-role-unified__block">
                    <h3 class="ta-job-role-unified__h">Summary</h3>
                    <p class="ta-job-role-unified__body pre-wrap"><%= desc %></p>
                </div>
                <% } %>
                <div class="ta-job-role-unified__block">
                    <h3 class="ta-job-role-unified__h">Responsibilities</h3>
                    <p class="ta-job-role-unified__body pre-wrap"><%= respText %></p>
                </div>
            </div>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-schedule-title">
            <h2 id="mo-job-schedule-title" class="ta-job-detail__heading">Schedule &amp; interview</h2>
            <p class="ta-job-detail__lede muted-inline">Teaching-week milestones (W) and the interview preview TAs see on the job page.</p>
            <div class="ta-job-panel">
                <div class="ta-job-panel__chunk">
                    <h3 class="ta-job-panel__title">Course milestones</h3>
                    <% if (weekMilestones.isEmpty()) { %>
                    <p class="ta-job-panel__body pre-wrap"><%= examTimeline %></p>
                    <% } else { %>
                    <p class="ta-job-panel__caption muted-inline">Bar length is relative to the latest week listed (week <%= timelineMaxWeekMo %>).</p>
                    <div class="week-timeline-list week-timeline-list--panel">
                        <% for (int ix = 0; ix < weekMilestones.size(); ix++) {
                               String[] item = weekMilestones.get(ix);
                               int weekNum = 1;
                               try { weekNum = Integer.parseInt(item[0]); } catch (Exception ignored) {}
                               int progress = Math.max(0, Math.min(100, (int) Math.round((weekNum / (double) timelineMaxWeekMo) * 100)));
                               String rawDesc = item[1] != null ? item[1].trim() : "";
                               boolean numericOnly = !rawDesc.isEmpty() && rawDesc.matches("\\d{1,4}");
                               String displayDesc = numericOnly ? "" : rawDesc;
                        %>
                        <div class="week-timeline-row">
                            <div class="week-timeline-head">
                                <span class="week-badge" title="Teaching week in the module calendar">Week <%= weekNum %></span>
                                <% if (weekMilestones.size() > 1) { %>
                                <span class="muted-inline week-milestone-index">Milestone <%= ix + 1 %> of <%= weekMilestones.size() %></span>
                                <% } %>
                            </div>
                            <div class="week-progress week-progress--full" role="img" aria-label="Relative position in module weeks: week <%= weekNum %> of <%= timelineMaxWeekMo %>">
                                <span class="week-progress-fill" style="width:<%= progress %>%"></span>
                            </div>
                            <% if (!displayDesc.isEmpty()) { %>
                            <p class="week-desc"><%= displayDesc %></p>
                            <% } else if (numericOnly) { %>
                            <p class="week-desc week-desc--muted muted-inline">No readable milestone text for this entry (module referenced week <%= weekNum %> only).</p>
                            <% } else { %>
                            <p class="week-desc week-desc--muted muted-inline">No extra detail for this week.</p>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                    <% } %>
                </div>
                <% if (!hideMoTaPlanDuplicate) { %>
                <div class="ta-job-panel__chunk ta-job-panel__chunk--sep">
                    <h3 class="ta-job-panel__title">Multi-TA allocation plan</h3>
                    <% if (taPlanChunks.isEmpty()) { %>
                    <p class="job-detail-empty">&mdash;</p>
                    <% } else if (taPlanChunks.size() == 1 && taPlanChunks.get(0)[0] == null) { %>
                    <div class="job-rich-text pre-wrap ta-job-panel__body"><%= taPlanChunks.get(0)[1] %></div>
                    <% } else { %>
                    <div class="ta-plan-grid">
                        <% for (String[] row : taPlanChunks) { %>
                        <article class="ta-plan-card">
                            <% if (row[0] != null) { %><span class="ta-plan-badge"><%= row[0] %></span><% } %>
                            <p class="ta-plan-text"><%= row[1] %></p>
                        </article>
                        <% } %>
                    </div>
                    <% } %>
                </div>
                <% } %>
                <div class="ta-job-panel__chunk ta-job-panel__chunk--sep ta-job-panel__chunk--interview">
                    <h3 class="ta-job-panel__title">Interview <span class="ta-job-panel__title-tag">published</span></h3>
                    <div class="job-interview-inline job-interview-inline--panel" role="group" aria-label="Published interview preview for applicants">
                        <div class="job-interview-inline-col job-interview-inline-col--time">
                            <span class="interview-info-label">Time</span>
                            <p class="interview-info-value pre-wrap"><% if (job.getInterviewSchedule() == null || job.getInterviewSchedule().trim().isEmpty()) { %><span class="job-detail-empty">&mdash;</span><% } else { %><%= interviewSchedule %><% } %></p>
                        </div>
                        <div class="job-interview-inline-col job-interview-inline-col--loc">
                            <span class="interview-info-label">Location</span>
                            <p class="interview-info-value pre-wrap"><% if (job.getInterviewLocation() == null || job.getInterviewLocation().trim().isEmpty()) { %><span class="job-detail-empty">&mdash;</span><% } else { %><%= interviewLocation %><% } %></p>
                        </div>
                    </div>
                    <p class="muted-inline mo-job-detail-interview-foot">Applicants see this preview on the TA job page; remind them to arrive early and bring ID if required.</p>
                </div>
            </div>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-workload-title">
            <h2 id="mo-job-workload-title" class="ta-job-detail__heading">Workload per TA <span class="ta-job-detail__heading-note">(estimate)</span></h2>
            <p class="muted-inline job-wa-edit-hint">
                If every planned TA slot is filled, the same duties split about like this (modelled on <strong><%= plannedRecruits %></strong> recruit(s)):
                total <strong><%= String.format(Locale.US, "%.2f", quotaRec.getTotalHours()) %> h</strong>,
                average <strong><%= String.format(Locale.US, "%.2f", quotaRec.getAverageHours()) %> h</strong> per TA,
                imbalance (max &minus; min) <strong><%= String.format(Locale.US, "%.2f", quotaRec.getImbalanceHours()) %> h</strong>.
                <% if (quotaRec.getUnknownDurationRows() > 0) { %>
                <span> <%= quotaRec.getUnknownDurationRows() %> row(s) used default 1h because duration text could not be parsed.</span>
                <% } %>
            </p>
            <div class="ta-duty-board">
                <% for (WorkQuotaPlanner.TAQuota q : quotaRec.getQuotas()) {
                       StringBuilder duty = new StringBuilder();
                       for (Map.Entry<String, Integer> e : q.getWorkCounts().entrySet()) {
                           if (duty.length() > 0) duty.append("; ");
                           duty.append(escHtml(e.getKey())).append(" x ").append(e.getValue());
                       }
                       if (duty.length() == 0) {
                           duty.append("No assigned work units.");
                       }
                %>
                <article class="ta-duty-card">
                    <div class="ta-duty-head"><span class="arr-icon arr-icon-slots" aria-hidden="true">TA</span><%= escHtml(q.getName()) %></div>
                    <p><strong>Estimated load:</strong> <%= String.format(Locale.US, "%.2f", q.getTotalHours()) %> h</p>
                    <p class="pre-wrap"><%= duty.toString() %></p>
                </article>
                <% } %>
            </div>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="mo-job-slots-title">
                <h2 id="mo-job-slots-title" class="ta-job-detail__heading">Bookable interview slots</h2>
                <p class="ta-job-detail__lede muted-inline">Reusable slots applicants can self-book when they are in Interview or Waitlist.</p>
                <div class="ta-job-panel mo-job-slots-panel">
                <div class="ta-job-panel__chunk">
                <% if (!moPastJobsPage) { %>
                <form action="<%= moCtx %>/mo/interview-slots" method="post" class="form">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <input type="hidden" name="jobId" value="<%= escHtml(job.getId()) %>">
                    <input type="hidden" name="action" value="create">
                    <label>Start time</label>
                    <input type="datetime-local" name="startsAt" required>
                    <label>Duration (minutes)</label>
                    <input type="number" name="durationMinutes" min="15" value="45" required>
                    <label>Capacity</label>
                    <input type="number" name="capacity" min="1" value="1" required>
                    <label>Location</label>
                    <input type="text" name="location" required placeholder="Room or Teams link">
                    <label>Notes</label>
                    <textarea name="notes" placeholder="Optional instructions for applicants"></textarea>
                    <button type="submit" class="btn btn-primary">Add interview slot</button>
                </form>
                <% } %>

                <% if (slotSummaries.isEmpty()) { %>
                <p class="section-empty">No bookable interview slots yet.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                    <table class="admin-table mo-cal-table">
                        <thead>
                        <tr>
                            <th>Time</th>
                            <th>Location</th>
                            <th>Capacity</th>
                            <th>Booked applicants</th>
                            <th>Notes</th>
                            <% if (!moPastJobsPage) { %><th></th><% } %>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (SlotSummary summary : slotSummaries) { %>
                        <tr>
                            <td class="pre-wrap"><%= escHtml(summary.getSlot().getStartsAt()) %><br><span class="muted-inline">to <%= escHtml(summary.getSlot().getEndsAt()) %></span></td>
                            <td class="pre-wrap"><%= escHtml(summary.getSlot().getLocation()) %></td>
                            <td><%= summary.getBookedCount() %> / <%= summary.getCapacity() %></td>
                            <td>
                                <% if (summary.getBookedApplications().isEmpty()) { %>
                                <span class="muted-inline">No bookings yet</span>
                                <% } else { %>
                                <% for (bupt.ta.model.Application booked : summary.getBookedApplications()) { %>
                                <div><%= escHtml(booked.getApplicantName() != null ? booked.getApplicantName() : booked.getApplicantId()) %> (<%= escHtml(booked.getStatus()) %>)</div>
                                <% } %>
                                <% } %>
                            </td>
                            <td class="pre-wrap"><%= escHtml(summary.getSlot().getNotes() != null && !summary.getSlot().getNotes().isEmpty() ? summary.getSlot().getNotes() : "-") %></td>
                            <% if (!moPastJobsPage) { %>
                            <td>
                                <form action="<%= moCtx %>/mo/interview-slots" method="post" class="inline-form">
                                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                                    <input type="hidden" name="jobId" value="<%= escHtml(job.getId()) %>">
                                    <input type="hidden" name="slotId" value="<%= escHtml(summary.getSlot().getId()) %>">
                                    <input type="hidden" name="action" value="delete">
                                    <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Delete this interview slot?');">Delete</button>
                                </form>
                            </td>
                            <% } %>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
                </div>
            </div>
            </section>

            </div>

            <p class="ta-job-detail__posted"><em>Posted as <%= escHtml(job.getPostedByName() != null ? job.getPostedByName() : "MO") %></em></p>

            <p>
                <a href="<%= manageHref %>" class="btn btn-primary">Go to applicant management</a>
                <a href="<%= moCtx %><%= moListPath %>" class="btn btn-primary">Back to list</a>
            </p>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card">
                <div class="widget-title">Posting detail</div>
                <p class="widget-line">This is what applicants see on the TA job page, plus MO-only fields (limits, waitlist).</p>
                <p class="widget-line"><a href="<%= manageHref %>">Review applications</a> for this posting.</p>
            </div>
        </aside>
    </div>
</div>
<% if (canEditWorkArrangements) { %>
<script>
(function () {
    var rowsBox = document.getElementById("job-detail-wa-rows");
    var addBtn = document.getElementById("job-detail-wa-add-row");
    var suggestBtn = document.getElementById("job-detail-wa-suggest-quota");
    var suggestionPanel = document.getElementById("job-detail-wa-suggestion-panel");
    var suggestionMeta = document.getElementById("job-detail-wa-suggestion-meta");
    var suggestionList = document.getElementById("job-detail-wa-suggestion-list");
    var plannedTaInput = document.getElementById("job-detail-planned-ta");
    var waForm = document.getElementById("job-detail-wa-form");
    if (!rowsBox || !addBtn || !waForm) return;

    function clearRowInputs(row) {
        row.querySelectorAll("input").forEach(function (inp) {
            if (inp.name === "waTaCount" || inp.name === "waOccurrenceCount") {
                inp.value = "1";
            } else {
                inp.value = "";
            }
        });
        row.querySelectorAll("select").forEach(function (sel) {
            sel.selectedIndex = 0;
        });
    }

    function bindRemove(row) {
        var btn = row.querySelector(".wa-remove-compact");
        if (!btn) return;
        btn.addEventListener("click", function () {
            if (rowsBox.querySelectorAll(".wa-row").length <= 1) return;
            row.remove();
        });
    }

    rowsBox.querySelectorAll(".wa-row").forEach(bindRemove);

    function stripIdsForClone(row) {
        row.querySelectorAll("[id]").forEach(function (el) { el.removeAttribute("id"); });
        row.querySelectorAll("label[for]").forEach(function (lb) { lb.removeAttribute("for"); });
    }

    addBtn.addEventListener("click", function () {
        var first = rowsBox.querySelector(".wa-row");
        if (!first) return;
        var clone = first.cloneNode(true);
        clearRowInputs(clone);
        stripIdsForClone(clone);
        rowsBox.appendChild(clone);
        bindRemove(clone);
    });

    function parseDurationHours(raw) {
        var text = (raw || "").trim();
        if (!text) return null;
        if (!/^\d+(\.\d+)?$/.test(text) && !/^\.\d+$/.test(text)) return null;
        var value = parseFloat(text);
        if (!isFinite(value) || value <= 0) return null;
        return value;
    }

    function collectRows() {
        var rows = [];
        rowsBox.querySelectorAll(".wa-row").forEach(function (row) {
            var name = (row.querySelector("input[name='waWorkName']") || {}).value || "";
            var duration = (row.querySelector("input[name='waSessionDuration']") || {}).value || "";
            var occRaw = parseInt(((row.querySelector("input[name='waOccurrenceCount']") || {}).value || "0"), 10);
            var taRaw = parseInt(((row.querySelector("input[name='waTaCount']") || {}).value || "0"), 10);
            var occ = isFinite(occRaw) ? occRaw : 0;
            var ta = isFinite(taRaw) ? taRaw : 0;
            rows.push({
                workName: name.trim(),
                durationText: duration.trim(),
                occurrenceCount: occ,
                taCount: ta
            });
        });
        return rows;
    }

    function renderSuggestion(result) {
        if (!suggestionPanel || !suggestionMeta || !suggestionList) return;
        suggestionPanel.hidden = false;
        suggestionMeta.textContent = result.meta;
        suggestionList.innerHTML = "";
        result.assignments.forEach(function (ta) {
            var card = document.createElement("div");
            card.className = "wa-suggestion-card";
            var details = [];
            Object.keys(ta.workCount).sort().forEach(function (workName) {
                details.push(workName + " x " + ta.workCount[workName]);
            });
            card.innerHTML =
                "<strong>" + ta.name + "</strong>" +
                "<div class='wa-suggestion-hours'>Estimated load: " + ta.hours.toFixed(2) + " h</div>" +
                "<div class='wa-suggestion-work'>" + (details.length ? details.join(" | ") : "No assigned items") + "</div>";
            suggestionList.appendChild(card);
        });
    }

    if (suggestBtn) {
        suggestBtn.addEventListener("click", function () {
            var rows = collectRows();
            var units = [];
            var unknownDurationRows = 0;
            rows.forEach(function (r, idx) {
                if (!r.workName || r.occurrenceCount < 1 || r.taCount < 1) return;
                var hours = parseDurationHours(r.durationText);
                if (hours == null) {
                    unknownDurationRows += 1;
                    hours = 1;
                }
                var totalUnits = r.occurrenceCount * r.taCount;
                for (var i = 0; i < totalUnits; i++) {
                    units.push({ workName: r.workName, hours: hours, rowIndex: idx });
                }
            });
            if (!units.length) {
                alert("Please complete at least one valid work arrangement row before requesting recommendations.");
                return;
            }
            var taCountRaw = parseInt((plannedTaInput && plannedTaInput.value ? plannedTaInput.value : "0"), 10);
            var taCount = isFinite(taCountRaw) ? taCountRaw : 0;
            if (taCount < 1) {
                alert("Planned recruits must be at least 1.");
                return;
            }
            var tas = [];
            for (var t = 0; t < taCount; t++) {
                tas.push({ name: "TA " + (t + 1), hours: 0, workCount: {} });
            }
            units.sort(function (a, b) { return b.hours - a.hours; });
            units.forEach(function (u) {
                tas.sort(function (a, b) { return a.hours - b.hours; });
                var pick = tas[0];
                pick.hours += u.hours;
                pick.workCount[u.workName] = (pick.workCount[u.workName] || 0) + 1;
            });
            var totalHours = units.reduce(function (s, u) { return s + u.hours; }, 0);
            var avg = totalHours / taCount;
            var max = Math.max.apply(null, tas.map(function (x) { return x.hours; }));
            var min = Math.min.apply(null, tas.map(function (x) { return x.hours; }));
            var meta = "Planned recruits: " + taCount + "; total estimated workload: " + totalHours.toFixed(2) + " h; average per TA: " + avg.toFixed(2) + " h; imbalance (max-min): " + (max - min).toFixed(2) + " h.";
            if (unknownDurationRows > 0) {
                meta += " " + unknownDurationRows + " row(s) used default 1h because duration text could not be parsed.";
            }
            renderSuggestion({ meta: meta, assignments: tas });
        });
    }

    waForm.addEventListener("submit", function (e) {
        if (!confirm("Save changes to work arrangements and planned recruits? This updates the published posting summary.")) {
            e.preventDefault();
            return;
        }
        if (!confirm("Second confirmation: apply these work arrangement edits now?")) {
            e.preventDefault();
        }
    });
})();
</script>
<% } %>
</body>
</html>
