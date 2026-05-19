<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.ai.AIMatchService" %>
<%!
    /** Split long MO "hours" / workload strings into readable segments (semicolons or Lab/Exam/Assignment blocks). */
    private static List<String> splitJobSegments(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String t = raw.trim().replace('\r', '\n').replace("\n\n", "\n");
        if (t.isEmpty()) {
            return Collections.emptyList();
        }
        if (t.contains(";")) {
            List<String> out = new ArrayList<>();
            for (String p : t.split(";")) {
                String x = p.trim().replace('\n', ' ');
                if (!x.isEmpty()) {
                    out.add(x);
                }
            }
            if (out.size() > 1) {
                return out;
            }
        }
        String[] parts = t.split("(?i)(?=\\s+(?:Lab|Exam|Assignment)\\s*:)");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String x = p.trim();
            if (!x.isEmpty()) {
                out.add(x);
            }
        }
        return out.isEmpty() ? Collections.singletonList(t) : out;
    }
%>
<% List<Object[]> jobsWithMatch = (List<Object[]>) request.getAttribute("jobsWithMatch"); if (jobsWithMatch == null) jobsWithMatch = java.util.Collections.emptyList();
   int taJobResultCount = jobsWithMatch.size();
   request.setAttribute("taNavActive", "jobs"); %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Find Jobs - TA Recruitment</title>
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
                <div class="icon-dot">S</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">P</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel ta-main ta-page ta-page--jobs">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Vacancies</p>
                <h1>Find available jobs</h1>
                <p class="ta-page-lead">Browse open positions matched to your profile. Use search filters to narrow by module or skill.</p>
            </header>

            <div class="ta-panel ta-panel--tip">
                <strong class="ta-panel__title">Quick tip</strong>
                <p class="ta-panel__body">Open <strong>View details</strong> for full module info, schedule, payment, workload estimate, deadline, and responsibilities, then use <strong>Review and apply</strong> to confirm your profile before submitting.</p>
            </div>
            <p class="ta-callout ta-callout--ai"><strong>AI skill matching</strong>: Jobs are ordered by your match score. Complete your <a href="${pageContext.request.contextPath}/ta/profile">profile skills</a> for better matching.</p>

            <div class="ta-panel ta-panel--search">
                <div class="ta-panel__headrow">
                    <h2 class="ta-panel__h">Search &amp; filters</h2>
                    <span class="ta-panel__meta">Refine the list below</span>
                </div>
            <form action="${pageContext.request.contextPath}/ta/jobs" method="get" class="search-form search-form--ta">
                <input type="text" name="keyword" placeholder="Keyword (title, module)" value="<%= escHtml(request.getParameter("keyword")) %>">
                <input type="text" name="moduleCode" placeholder="Module code" value="<%= escHtml(request.getParameter("moduleCode")) %>">
                <input type="text" name="skill" placeholder="Required skill" value="<%= escHtml(request.getParameter("skill")) %>">
                <select name="jobType">
                    <option value="">All types</option>
                    <option value="MODULE_TA" <%= "MODULE_TA".equals(request.getParameter("jobType")) ? "selected" : "" %>>Module TA</option>
                    <option value="INVIGILATION" <%= "INVIGILATION".equals(request.getParameter("jobType")) ? "selected" : "" %>>Invigilation</option>
                    <option value="OTHER" <%= "OTHER".equals(request.getParameter("jobType")) ? "selected" : "" %>>Other</option>
                </select>
                <button type="submit" class="btn btn-primary">Search</button>
            </form>
            </div>

            <% String err = request.getParameter("error");
               if ("already_applied".equals(err)) { %><p class="error">You have already applied for this job.</p>
            <% } else if ("job_closed".equals(err)) { %><p class="error">This job is no longer open.</p>
            <% } else if ("job_not_found".equals(err)) { %><p class="error">Job not found.</p>
            <% } else if ("invalid_job".equals(err)) { %><p class="error">Invalid job.</p><% } %>

            <div class="ta-job-results" aria-live="polite">
            <div class="ta-results-head">
                <h2 class="ta-results-title">Matching vacancies</h2>
                <span class="ta-results-count"><%= taJobResultCount %> <%= taJobResultCount == 1 ? "listing" : "listings" %></span>
            </div>

            <% for (Object[] row : jobsWithMatch) {
                Job j = (Job) row[0];
                AIMatchService.MatchResult match = (AIMatchService.MatchResult) row[1];
                boolean saved = row.length > 2 && Boolean.TRUE.equals(row[2]);
                String title = escHtml(j.getTitle() != null ? j.getTitle() : "");
                String moduleCode = escHtml(j.getModuleCode() != null ? j.getModuleCode() : "");
                String moduleName = escHtml(j.getModuleName() != null ? j.getModuleName() : "");
                String desc = escHtml(j.getDescription() != null ? j.getDescription() : "");
                String deadline = escHtml(j.getDeadline() != null ? j.getDeadline() : "");
                String postedByName = escHtml(j.getPostedByName() != null ? j.getPostedByName() : "MO");
                String safeJobId = escHtml(j.getId() != null ? j.getId() : "");
            %>
            <article class="job-card ta-job-card">
                <header class="ta-job-card__head">
                    <h3 class="ta-job-card__title"><%= title %> <span class="ta-job-card__dash">&mdash;</span> <%= moduleCode %></h3>
                    <span class="match-badge ta-job-card__match" title="<%= escHtml(match.explanation) %>">Match <%= (int)match.score %>%</span>
                </header>
                <p class="ta-job-card__moduleline"><span class="ta-job-card__module-name"><%= moduleName %></span>
                    <% if (j.getJobType() != null && !j.getJobType().isEmpty()) { %>
                    <span class="ta-job-card__type-pill"><%= "MODULE_TA".equals(j.getJobType()) ? "Module TA" : "INVIGILATION".equals(j.getJobType()) ? "Invigilation" : "Other" %></span>
                    <% } %>
                </p>
                <% if (j.getDescription() != null && !j.getDescription().trim().isEmpty()) { %>
                <p class="ta-job-card__desc"><%= desc %></p>
                <% } %>
                <% if (j.getRequiredSkills() != null && !j.getRequiredSkills().isEmpty()) { %>
                <p class="ta-job-card__skills"><span class="ta-job-card__skills-label">Required</span> <%= escHtml(String.join(", ", j.getRequiredSkills())) %></p>
                <% } %>

                <dl class="ta-job-card__facts">
                <% if (j.getWorkingHours() != null && !j.getWorkingHours().trim().isEmpty()) {
                       List<String> whParts = splitJobSegments(j.getWorkingHours());
                %>
                    <dt>Hours / schedule</dt>
                    <dd>
                        <% if (whParts.size() > 1) { %>
                        <ul class="ta-job-card__seglist">
                            <% for (String seg : whParts) { %><li class="pre-wrap"><%= escHtml(seg) %></li><% } %>
                        </ul>
                        <% } else { %>
                        <span class="pre-wrap"><%= escHtml(whParts.get(0)) %></span>
                        <% } %>
                    </dd>
                <% } %>
                <% if (!deadline.isEmpty()) { %>
                    <dt>Apply by</dt><dd class="ta-job-card__deadline"><%= deadline %></dd>
                <% } %>
                    <dt>Planned recruits</dt><dd class="ta-job-card__deadline"><%= j.getTaSlots() > 0 ? j.getTaSlots() : 1 %></dd>
                <% if (j.getExamTimeline() != null && !j.getExamTimeline().isEmpty()) { %>
                    <dt>Timeline</dt><dd class="pre-wrap"><%= escHtml(j.getExamTimeline()) %></dd>
                <% } %>
                <% if (j.getInterviewSchedule() != null && !j.getInterviewSchedule().trim().isEmpty()) { %>
                    <dt>Interview</dt>
                    <dd class="pre-wrap"><%= escHtml(j.getInterviewSchedule().trim()) %></dd>
                <% } %>
                <% if (j.getInterviewLocation() != null && !j.getInterviewLocation().trim().isEmpty()) { %>
                    <dt>Interview location</dt>
                    <dd class="pre-wrap"><%= escHtml(j.getInterviewLocation().trim()) %></dd>
                <% } %>
                </dl>

                <% if (match.matched != null && !match.matched.isEmpty()) { %>
                <p class="ta-job-card__ai ta-job-card__ai--ok">Your matched skills: <%= escHtml(String.join(", ", match.matched)) %></p>
                <% } %>
                <% if (match.missing != null && !match.missing.isEmpty()) { %>
                <p class="ta-job-card__ai ta-job-card__ai--gap">Missing skills for this job: <strong><%= escHtml(String.join(", ", match.missing)) %></strong>. Consider adding them to your profile.</p>
                <% } %>
                <p class="ta-job-card__posted"><em>Posted by <%= postedByName %></em></p>
                <div class="ta-job-actions">
                    <a href="${pageContext.request.contextPath}/ta/job?jobId=<%= safeJobId %>" class="btn btn-primary">Open vacancy details</a>
                    <form action="${pageContext.request.contextPath}/ta/save-job" method="post" class="inline-form">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="jobId" value="<%= safeJobId %>">
                        <input type="hidden" name="action" value="<%= saved ? "unsave" : "save" %>">
                        <input type="hidden" name="returnTo" value="/ta/jobs">
                        <button type="submit" class="btn <%= saved ? "btn-danger" : "btn-success" %>"><%= saved ? "Remove saved" : "Save job" %></button>
                    </form>
                </div>
            </article>
            <% }
               if (jobsWithMatch.isEmpty()) { %>
            <p class="section-empty section-empty--card ta-empty">No jobs match your search. Try different keywords or clear filters.</p>
            <% } %>
            </div>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">TA status</div>
                <p class="widget-line">Role: Teaching Assistant</p>
                <p class="widget-line">Stronger profiles rank higher in match scoring.</p>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Reminders</div>
                <p class="widget-line">Check deadlines before applying.</p>
                <p class="widget-line">Upload a CV so organisers can review your background.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
