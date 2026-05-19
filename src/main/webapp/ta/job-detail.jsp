<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.WorkArrangementItem" %>
<%@ page import="bupt.ta.ai.AIMatchService" %>
<%@ page import="bupt.ta.util.WorkQuotaPlanner" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%
    request.setAttribute("taNavActive", "jobs");
    Job job = (Job) request.getAttribute("job");
    AIMatchService.MatchResult match = (AIMatchService.MatchResult) request.getAttribute("match");
    if (job == null) {
        response.sendRedirect(request.getContextPath() + "/ta/jobs?error=job_not_found");
        return;
    }
    boolean isOpen = "OPEN".equals(job.getStatus());
    String wh = job.getWorkingHours() != null && !job.getWorkingHours().isEmpty() ? escHtml(job.getWorkingHours()) : "—";
    String wl = job.getWorkload() != null && !job.getWorkload().isEmpty() ? escHtml(job.getWorkload()) : "—";
    String pay = job.getPayment() != null && !job.getPayment().isEmpty() ? escHtml(job.getPayment()) : "—";
    String deadline = job.getDeadline() != null && !job.getDeadline().isEmpty() ? escHtml(job.getDeadline()) : "—";
    String examTimeline = job.getExamTimeline() != null && !job.getExamTimeline().isEmpty() ? escHtml(job.getExamTimeline()) : "—";
    String interviewSchedule = job.getInterviewSchedule() != null && !job.getInterviewSchedule().isEmpty() ? escHtml(job.getInterviewSchedule()) : "—";
    String interviewLocation = job.getInterviewLocation() != null && !job.getInterviewLocation().isEmpty() ? escHtml(job.getInterviewLocation()) : "—";
    int plannedRecruits = job.getTaSlots() > 0 ? job.getTaSlots() : 1;
    boolean hasWorkArrangementsTable = job.getWorkArrangements() != null && !job.getWorkArrangements().isEmpty();
    WorkQuotaPlanner.Recommendation quotaRec = WorkQuotaPlanner.recommend(job.getWorkArrangements(), plannedRecruits);
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
<%
    String respText = job.getResponsibilities() != null && !job.getResponsibilities().isEmpty() ? escHtml(job.getResponsibilities()) : "—";
    String desc = job.getDescription() != null && !job.getDescription().isEmpty() ? escHtml(job.getDescription()) : "—";
    String safeTitle = escHtml(job.getTitle() != null ? job.getTitle() : "");
    int timelineMaxWeek = 14;
    for (String[] item : weekMilestones) {
        try {
            int w = Integer.parseInt(item[0]);
            if (w > timelineMaxWeek) {
                timelineMaxWeek = w;
            }
        } catch (Exception ignored) { /* keep */ }
    }
    if (timelineMaxWeek < 1) {
        timelineMaxWeek = 14;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= safeTitle %> - Job Detail</title>
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
                <div class="icon-dot active">F</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">P</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel ta-main ta-page ta-page--job-detail">
            <header class="ta-job-detail-hero">
                <p class="breadcrumb-line ta-job-detail-breadcrumb"><a href="${pageContext.request.contextPath}/ta/jobs">&larr; Back to job list</a></p>
                <div class="ta-job-detail-hero__main">
                    <p class="ta-page-kicker ta-job-detail-kicker">Vacancy</p>
                    <h1><%= safeTitle %></h1>
                    <p class="job-detail-meta">
                        <span class="status-pill <%= isOpen ? "status-pill-pending" : "status-pill-rejected" %>"><%= job.getStatus() %></span>
                        <% if (job.getJobType() != null && !job.getJobType().isEmpty()) { %>
                        <span class="muted-inline">Type: <%= "MODULE_TA".equals(job.getJobType()) ? "Module TA" : "INVIGILATION".equals(job.getJobType()) ? "Invigilation" : "Other" %></span>
                        <% } %>
                    </p>
                    <% if (match != null) { %>
                    <div class="ta-job-match-strip" role="region" aria-label="Skill match summary">
                        <div class="ta-job-match-strip__row">
                            <span class="ta-job-match-strip__label">Your match</span>
                            <span class="ta-job-match-pill match-badge" title="<%= escHtml(match.explanation) %>"><%= (int) match.score %>%</span>
                        </div>
                        <% if (match.matched != null && !match.matched.isEmpty()) { %>
                        <p class="ta-job-match-strip__detail ta-job-match-strip__detail--ok muted-inline"><strong>Aligned skills:</strong> <%= escHtml(String.join(", ", match.matched)) %></p>
                        <% } %>
                        <% if (match.missing != null && !match.missing.isEmpty()) { %>
                        <p class="ta-job-match-strip__detail ta-job-match-strip__detail--gap muted-inline"><strong>Gaps vs posting:</strong> <%= escHtml(String.join(", ", match.missing)) %></p>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </header>
            <% if (Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
            <div class="ta-panel ta-panel--tip ta-job-llm-panel llm-insight-card" data-match-insight data-job-id="<%= escHtml(job.getId()) %>">
                <strong class="ta-panel__title">AI match insight (DeepSeek)</strong>
                <p class="ta-panel__body muted-inline">Optional narrative on strengths, gaps, and practical fit. The rule-based score in the header is unchanged.</p>
                <div class="ai-summary-actions">
                    <button type="button" class="btn btn-secondary btn-sm match-insight-btn">Generate AI insight</button>
                </div>
                <div class="match-insight-result"></div>
            </div>
            <% } %>

            <div class="ta-job-detail">

            <% if (hasWorkArrangementsTable) { %>
            <section class="ta-job-detail__section" aria-labelledby="ta-job-wa-title">
            <h2 id="ta-job-wa-title" class="ta-job-detail__heading">Work arrangements</h2>
            <p class="ta-job-detail__lede muted-inline">Concrete duties, durations, occurrences, and how many TAs share each line.</p>
            <div class="ta-wa-table-wrap">
            <table class="job-wa-table">
                <thead>
                <tr>
                    <th scope="col">Work name</th>
                    <th scope="col">Per-session duration</th>
                    <th scope="col">Occurrences</th>
                    <th scope="col">TAs needed</th>
                    <th scope="col">Specific time</th>
                </tr>
                </thead>
                <tbody>
                <% for (WorkArrangementItem wa : job.getWorkArrangements()) {
                       String wn = wa.getWorkName() != null ? escHtml(wa.getWorkName()) : "—";
                       String sd = escHtml(wa.getResolvedSessionDuration());
                       if (sd.isEmpty()) sd = "—";
                       int occ = wa.getResolvedOccurrenceCount();
                       int wc = wa.getTaCount() > 0 ? wa.getTaCount() : 0;
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
            </div>
            </section>
            <% } %>

            <section class="ta-job-detail__section" aria-labelledby="ta-job-key-title">
            <h2 id="ta-job-key-title" class="ta-job-detail__heading">Key information</h2>
            <p class="ta-job-detail__lede muted-inline">Compensation, identifiers, planned headcount, deadline, and whether applications are open.</p>
            <dl class="job-detail-dl job-detail-dl--ta-summary">
                <dt>Module</dt>
                <dd><%
                    String mcodeRaw = job.getModuleCode() != null ? job.getModuleCode().trim() : "";
                    String mnameRaw = job.getModuleName() != null ? job.getModuleName().trim() : "";
                    if (mcodeRaw.isEmpty() && mnameRaw.isEmpty()) { %>—<% }
                    else if (!mcodeRaw.isEmpty() && !mnameRaw.isEmpty()) { %><%= escHtml(mcodeRaw) %> <span class="muted-inline">&middot;</span> <%= escHtml(mnameRaw) %><% }
                    else if (!mcodeRaw.isEmpty()) { %><%= escHtml(mcodeRaw) %><% }
                    else { %><%= escHtml(mnameRaw) %><% }
                %></dd>
                <% if (!hasWorkArrangementsTable) { %>
                <dt>Hours / schedule</dt><dd class="pre-wrap"><%= wh %></dd>
                <% } %>
                <dt>Payment</dt><dd class="pre-wrap"><%= pay %></dd>
                <dt>Required skills</dt><dd><%= job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty() ? escHtml(String.join(", ", job.getRequiredSkills())) : "—" %></dd>
                <% if (!hasWorkArrangementsTable) { %>
                <dt>Workload</dt><dd class="pre-wrap"><%= wl %></dd>
                <% } %>
                <dt>Planned recruits</dt><dd><%= plannedRecruits %></dd>
                <dt>Application deadline</dt><dd><%= deadline %></dd>
                <dt>Open status</dt><dd><%= isOpen ? "Open for applications" : "Closed" %></dd>
            </dl>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="ta-job-role-title">
            <h2 id="ta-job-role-title" class="ta-job-detail__heading">Role &amp; scope</h2>
            <p class="ta-job-detail__lede muted-inline">What the module expects from TAs in this posting.</p>
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

            <section class="ta-job-detail__section" aria-labelledby="ta-job-schedule-title">
            <h2 id="ta-job-schedule-title" class="ta-job-detail__heading">Schedule &amp; interview</h2>
            <p class="ta-job-detail__lede muted-inline">Milestones use <strong>teaching weeks</strong> (W) from the module. Interview times are a preview only until you receive process notifications.</p>
            <div class="ta-job-panel">
                <div class="ta-job-panel__chunk">
                    <h3 class="ta-job-panel__title">Course milestones</h3>
                    <% if (weekMilestones.isEmpty()) { %>
                    <p class="ta-job-panel__body pre-wrap"><%= examTimeline %></p>
                    <% } else { %>
                    <p class="ta-job-panel__caption muted-inline">Bar length is relative to the latest week listed below (week <%= timelineMaxWeek %>).</p>
                    <div class="week-timeline-list week-timeline-list--panel">
                        <% for (int ix = 0; ix < weekMilestones.size(); ix++) {
                               String[] item = weekMilestones.get(ix);
                               int weekNum = 1;
                               try { weekNum = Integer.parseInt(item[0]); } catch (Exception ignored) {}
                               int progress = Math.max(0, Math.min(100, (int) Math.round((weekNum / (double) timelineMaxWeek) * 100)));
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
                            <div class="week-progress week-progress--full" role="img" aria-label="Relative position in module weeks: week <%= weekNum %> of <%= timelineMaxWeek %>">
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
                <div class="ta-job-panel__chunk ta-job-panel__chunk--sep ta-job-panel__chunk--interview">
                    <h3 class="ta-job-panel__title">Interview <span class="ta-job-panel__title-tag">preview</span></h3>
                    <div class="job-interview-inline job-interview-inline--panel" role="group" aria-label="Estimated interview time and location">
                        <div class="job-interview-inline-col job-interview-inline-col--time">
                            <span class="interview-info-label">Time</span>
                            <p class="interview-info-value pre-wrap"><% if (job.getInterviewSchedule() == null || job.getInterviewSchedule().trim().isEmpty()) { %><span class="job-detail-empty">Not published</span><% } else { %><%= interviewSchedule %><% } %></p>
                        </div>
                        <div class="job-interview-inline-col job-interview-inline-col--loc">
                            <span class="interview-info-label">Location</span>
                            <p class="interview-info-value pre-wrap"><% if (job.getInterviewLocation() == null || job.getInterviewLocation().trim().isEmpty()) { %><span class="job-detail-empty">Not published</span><% } else { %><%= interviewLocation %><% } %></p>
                        </div>
                    </div>
                    <div class="interview-inline-notes interview-inline-notes--panel">
                        <p class="muted-inline">Arrive a few minutes early; bring your student ID if the module team requires it.</p>
                        <p class="muted-inline ta-interview-process-note">The exact interview time is subject to the notification issued during the application process.</p>
                    </div>
                </div>
            </div>
            </section>

            <section class="ta-job-detail__section" aria-labelledby="ta-job-workload-title">
            <h2 id="ta-job-workload-title" class="ta-job-detail__heading">Workload estimate</h2>
            <p class="muted-inline job-wa-edit-hint">
                If this posting is fully staffed with <strong><%= plannedRecruits %></strong> recruit(s),
                total <strong><%= String.format(Locale.US, "%.2f", quotaRec.getTotalHours()) %> h</strong>,
                average <strong><%= String.format(Locale.US, "%.2f", quotaRec.getAverageHours()) %> h</strong> per selected TA.
                Specific work is shared by the selected TA group rather than tied to numbered TA roles.
            </p>
            </section>

            <p class="ta-job-detail__posted"><em>Posted by <%= escHtml(job.getPostedByName() != null ? job.getPostedByName() : "MO") %></em></p>

            <% if (isOpen) { %>
            <section class="ta-job-detail__section ta-job-detail__section--apply" aria-labelledby="ta-job-apply-title">
            <div class="job-detail-apply ta-apply-panel">
                <h2 id="ta-job-apply-title">Apply for this position</h2>
                <p class="muted-inline">Review the job details above, then continue to confirm what will be shared from your profile and CV.</p>
                <p><a href="${pageContext.request.contextPath}/ta/apply-confirm?jobId=<%= escHtml(job.getId()) %>" class="btn btn-primary btn-lg">Review and apply</a></p>
            </div>
            </section>
            <% } else { %>
            <p class="error">This job is closed; applications are not accepted.</p>
            <% } %>

            </div>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Before you apply</div>
                <p class="widget-line">Check deadline and workload fit.</p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/ta/profile">Update your skills</a> to improve match.</p>
            </div>
        </aside>
    </div>
</div>
<script src="${pageContext.request.contextPath}/js/llm-stream.js"></script>
<script>
(function () {
    function escapeHtml(text) {
        return String(text == null ? "" : text)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    var card = document.querySelector("[data-match-insight]");
    if (!card) return;
    var btn = card.querySelector(".match-insight-btn");
    var resultBox = card.querySelector(".match-insight-result");
    var jobId = card.getAttribute("data-job-id");

    function renderInsight(text, showDisclaimer) {
        var html = "<p class='pre-wrap llm-insight-body'></p>";
        if (showDisclaimer) {
            html += "<p class='muted-inline llm-insight-disclaimer'>Narrative only; verify facts before decisions.</p>";
        }
        resultBox.innerHTML = html;
        resultBox.querySelector(".llm-insight-body").textContent = text;
    }

    btn.addEventListener("click", function () {
        if (!jobId) return;
        var oldText = btn.textContent;
        btn.disabled = true;
        btn.textContent = "Generating...";
        resultBox.innerHTML = "<p class='muted-inline'>Calling AI, this may take up to 30 seconds...</p>";

        var url = "${pageContext.request.contextPath}/ta/match-insight?jobId=" + encodeURIComponent(jobId);
        LlmStream.call(url, {
            onChunk: function (accumulated) {
                renderInsight(accumulated, false);
            },
            onDone: function (accumulated) {
                renderInsight(accumulated, true);
                btn.textContent = "Regenerate AI insight";
                btn.disabled = false;
            },
            onJson: function (body) {
                renderInsight(body.insight || "", true);
                btn.textContent = "Regenerate AI insight";
                btn.disabled = false;
            },
            onError: function (msg) {
                resultBox.innerHTML = "<p class='error'>AI insight failed: " + escapeHtml(msg || "Unknown error") + "</p>";
                btn.textContent = oldText;
                btn.disabled = false;
            }
        });
    });
})();
</script>
</body>
</html>
