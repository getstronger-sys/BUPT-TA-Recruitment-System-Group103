<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.ai.AIMatchService" %>
<% List<Object[]> jobsWithMatch = (List<Object[]>) request.getAttribute("jobsWithMatch"); if (jobsWithMatch == null) jobsWithMatch = java.util.Collections.emptyList();
   int savedCount = request.getAttribute("savedCount") != null ? (Integer) request.getAttribute("savedCount") : jobsWithMatch.size();
   request.setAttribute("taNavActive", "saved"); %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Saved Jobs - TA Recruitment</title>
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
                <div class="icon-dot">F</div>
                <div class="icon-dot active">S</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">P</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel ta-main ta-page ta-page--saved">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Shortlist</p>
                <h1>Saved jobs</h1>
                <p class="ta-page-lead">Your shortlist is ordered by AI match score so the strongest-fit roles stay at the top.</p>
            </header>

            <div class="ta-panel ta-panel--tip">
                <strong class="ta-panel__title">Saved list</strong>
                <p class="ta-panel__body">You have <strong><%= savedCount %></strong> saved job<%= savedCount == 1 ? "" : "s" %>. Closed roles stay here until you remove them, so you can compare later.</p>
            </div>

            <div class="ta-job-results ta-saved-results" aria-live="polite">
            <div class="ta-results-head">
                <h2 class="ta-results-title">Saved vacancies</h2>
                <span class="ta-results-count"><%= jobsWithMatch.size() %> <%= jobsWithMatch.size() == 1 ? "role" : "roles" %></span>
            </div>

            <% for (Object[] row : jobsWithMatch) {
                Job j = (Job) row[0];
                AIMatchService.MatchResult match = (AIMatchService.MatchResult) row[1];
                boolean isOpen = "OPEN".equals(j.getStatus());
            %>
            <div class="job-card ta-job-card">
                <h3><%= escHtml(j.getTitle() != null ? j.getTitle() : "") %> - <%= escHtml(j.getModuleCode() != null ? j.getModuleCode() : "") %>
                    <span class="match-badge" title="<%= escHtml(match.explanation) %>">Match: <%= (int) match.score %>%</span>
                </h3>
                <p><strong><%= escHtml(j.getModuleName() != null ? j.getModuleName() : "") %></strong>
                    <% if (j.getJobType() != null && !j.getJobType().isEmpty()) { %>
                    | Type: <%= "MODULE_TA".equals(j.getJobType()) ? "Module TA" : "INVIGILATION".equals(j.getJobType()) ? "Invigilation" : "Other" %>
                    <% } %>
                    | Status: <%= isOpen ? "Open" : "Closed" %>
                </p>
                <p><%= escHtml(j.getDescription() != null ? j.getDescription() : "") %></p>
                <% if (j.getWorkingHours() != null && !j.getWorkingHours().isEmpty()) { %>
                <p><strong>Hours:</strong> <%= escHtml(j.getWorkingHours()) %></p>
                <% } %>
                <% if (j.getDeadline() != null && !j.getDeadline().isEmpty()) { %>
                <p class="job-list-deadline"><strong>Apply by:</strong> <%= escHtml(j.getDeadline()) %></p>
                <% } %>
                <% if (match.matched != null && !match.matched.isEmpty()) { %>
                <p class="ai-matched">Your matched skills: <%= escHtml(String.join(", ", match.matched)) %></p>
                <% } %>
                <% if (match.missing != null && !match.missing.isEmpty()) { %>
                <p class="ai-missing">Missing skills for this job: <strong><%= escHtml(String.join(", ", match.missing)) %></strong>.</p>
                <% } %>
                <div class="ta-job-actions">
                    <a href="${pageContext.request.contextPath}/ta/job?jobId=<%= escHtml(j.getId()) %>" class="btn btn-primary">View details</a>
                    <form action="${pageContext.request.contextPath}/ta/save-job" method="post" class="inline-form">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="jobId" value="<%= escHtml(j.getId()) %>">
                        <input type="hidden" name="action" value="unsave">
                        <input type="hidden" name="returnTo" value="/ta/saved-jobs">
                        <button type="submit" class="btn btn-danger">Remove saved</button>
                    </form>
                </div>
            </div>
            <% }
               if (jobsWithMatch.isEmpty()) { %>
            <p class="section-empty section-empty--card ta-empty">You have not saved any jobs yet. Browse <a href="${pageContext.request.contextPath}/ta/jobs">open roles</a> and save the ones you want to compare later.</p>
            <% } %>
            </div>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Sorting</div>
                <p class="widget-line">Saved jobs are ranked by match score.</p>
                <p class="widget-line">Open roles are kept ahead of closed ones when scores tie.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
