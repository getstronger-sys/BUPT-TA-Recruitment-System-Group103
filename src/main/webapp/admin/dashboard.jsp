<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.AdminSettings" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%
    AdminSettings settings = (AdminSettings) request.getAttribute("adminSettings");
    if (settings == null) settings = new AdminSettings();
    AdminService.DashboardSummary summary = (AdminService.DashboardSummary) request.getAttribute("summary");
    AdminService.MonitoringReport monitoring = (AdminService.MonitoringReport) request.getAttribute("monitoring");
    if (summary == null) {
        summary = new AdminService.DashboardSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, new java.util.LinkedHashMap<String, Integer>());
    }
    if (monitoring == null) {
        monitoring = new AdminService.MonitoringReport(
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );
    }
    request.setAttribute("adminNavActive", "summary");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Admin Dashboard - TA Recruitment</title>
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
                <div class="icon-dot active">D</div>
                <div class="icon-dot">W</div>
                <div class="icon-dot">M</div>
                <div class="icon-dot">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Overview</p>
                <h1>Recruitment Summary</h1>
                <p class="ta-page-lead">Track the platform at a glance, configure workload rules, and jump into exception monitoring when something needs intervention.</p>
            </header>
            <div class="context-card">
                <strong>Admin scope</strong>
                <p>This dashboard summarizes jobs, applications, system rules, and cross-role workload pressure for the whole recruitment process.</p>
            </div>

            <% boolean adminDashFlash = "1".equals(request.getParameter("saved")) || request.getParameter("autoClosed") != null;
               String adminDashErr = (String) request.getAttribute("error");
               if (adminDashFlash || adminDashErr != null) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("saved"))) { %>
            <p class="success">Admin settings saved.</p>
            <% } %>
            <% if (request.getParameter("autoClosed") != null) { %>
            <p class="success">Pending applications auto-closed after applying the workload rule: <strong><%= request.getParameter("autoClosed") %></strong>.</p>
            <% } %>
            <% if (adminDashErr != null) { %>
            <p class="error"><%= escHtml(adminDashErr) %></p>
            <% } %>
            </div>
            <% } %>

            <div class="stats-row admin-stat-grid">
                <div class="stat-card">
                    <div class="stat-icon">J</div>
                    <div>
                        <div class="stat-title">Jobs</div>
                        <div class="stat-value"><%= summary.getTotalJobs() %></div>
                        <div class="stat-meta">Open <%= summary.getOpenJobs() %> | Inactive <%= summary.getInactiveJobs() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">A</div>
                    <div>
                        <div class="stat-title">Applications</div>
                        <div class="stat-value"><%= summary.getTotalApplications() %></div>
                        <div class="stat-meta">Pending <%= summary.getCount("PENDING") %> | Interview <%= summary.getCount("INTERVIEW") %> | Selected <%= summary.getCount("SELECTED") %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">U</div>
                    <div>
                        <div class="stat-title">Users</div>
                        <div class="stat-value"><%= summary.getTotalTas() + summary.getTotalMos() + summary.getTotalAdmins() %></div>
                        <div class="stat-meta">TA <%= summary.getTotalTas() %> | MO <%= summary.getTotalMos() %> | Admin <%= summary.getTotalAdmins() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">L</div>
                    <div>
                        <div class="stat-title">Workload Rule</div>
                        <div class="stat-value"><%= settings.usesHourWorkloadLimit()
                                ? String.format("%.0f h", settings.getMaxWorkloadHoursPerTa())
                                : (settings.hasWorkloadLimit() ? String.valueOf(settings.getMaxSelectedJobsPerTa()) : "0") %></div>
                        <div class="stat-meta"><%= settings.usesHourWorkloadLimit()
                                ? "Hour cap (structured workload)"
                                : settings.hasWorkloadLimit() ? "Job-count cap" : "No hard limit" %> | Auto-close <%= settings.isAutoClosePendingWhenLimitReached() ? "ON" : "OFF" %></div>
                    </div>
                </div>
            </div>

            <div class="detail-grid admin-detail-grid">
                <section class="detail-card">
                    <h3>Application status breakdown</h3>
                    <div class="admin-chip-grid">
                        <span class="admin-chip">Pending: <strong><%= summary.getCount("PENDING") %></strong></span>
                        <span class="admin-chip">Interview: <strong><%= summary.getCount("INTERVIEW") %></strong></span>
                        <span class="admin-chip">Waitlist: <strong><%= summary.getCount("WAITLIST") %></strong></span>
                        <span class="admin-chip">Selected: <strong><%= summary.getCount("SELECTED") %></strong></span>
                        <span class="admin-chip">Rejected: <strong><%= summary.getCount("REJECTED") %></strong></span>
                        <span class="admin-chip">Auto-closed: <strong><%= summary.getCount(AdminService.STATUS_AUTO_CLOSED) %></strong></span>
                        <span class="admin-chip">Withdrawn: <strong><%= summary.getCount("WITHDRAWN") %></strong></span>
                    </div>
                </section>

                <section class="detail-card">
                    <h3>System pressure snapshot</h3>
                    <p><strong>TAs at or over workload limit:</strong> <%= summary.getTasAtOrOverLimit() %></p>
                    <p><strong>Jobs at capacity:</strong> <%= summary.getJobsAtCapacity() %></p>
                    <p><strong>Current monitoring issues:</strong> <%= monitoring.getTotalIssues() %></p>
                    <p><strong>User directory:</strong> TA <%= summary.getTotalTas() %> | MO <%= summary.getTotalMos() %> | Admin <%= summary.getTotalAdmins() %></p>
                    <p><a href="${pageContext.request.contextPath}/admin/monitoring" class="btn btn-primary">Open monitoring</a></p>
                </section>
            </div>

            <section class="detail-card admin-settings-card">
                <h3>Workload limit settings</h3>
                <p><strong>Hour cap (recommended):</strong> set a maximum total of <em>estimated hours per selected TA</em> from each job’s structured work arrangements (same basis as MO workload planning). When auto-close is on, once a TA’s selected posts reach that hour total, their other <strong>pending</strong> applications are closed. New applications are also blocked if current selected hours plus this job’s estimate would exceed the cap.</p>
                <p><strong>Job-count cap (legacy):</strong> used only when the hour cap is set to 0 — limits how many distinct selected posts a TA may hold.</p>
                <form action="${pageContext.request.contextPath}/admin/dashboard" method="post" class="form form--admin-settings">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <label for="admin-hour-cap">Maximum workload hours per TA (0 = use job-count rule below)</label>
                    <input id="admin-hour-cap" type="number" min="0" step="0.5" name="maxWorkloadHoursPerTa" value="<%= settings.getMaxWorkloadHoursPerTa() > 0 ? settings.getMaxWorkloadHoursPerTa() : "" %>" placeholder="e.g. 40">
                    <label for="admin-limit">Maximum selected jobs per TA (only when hour cap is 0)</label>
                    <input id="admin-limit" type="number" min="0" name="maxSelectedJobsPerTa" value="<%= settings.getMaxSelectedJobsPerTa() %>">
                    <label class="checkbox-line" for="admin-auto-close">
                        <input id="admin-auto-close" type="checkbox" name="autoClosePendingWhenLimitReached" <%= settings.isAutoClosePendingWhenLimitReached() ? "checked" : "" %>>
                        Automatically close pending applications once the limit is reached
                    </label>
                    <button type="submit" class="btn btn-primary">Save admin settings</button>
                </form>
            </section>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Monitoring Snapshot</div>
                <p class="widget-line">Limit alerts: <%= monitoring.getLimitAlerts().size() %></p>
                <p class="widget-line">Interview notice issues: <%= monitoring.getInterviewNoticeAlerts().size() %></p>
                <p class="widget-line">Capacity issues: <%= monitoring.getCapacityAlerts().size() %></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/users">Open user directory</a></p>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Rule Reminder</div>
                <p class="widget-line">Hour cap 0 with job cap 0 disables both caps.</p>
                <p class="widget-line">Auto-close only affects pending applications, not interview-stage records.</p>
            </div>
        </aside>
    </div>
</div>
<%@ include file="/WEB-INF/jspf/login-celebration.jspf" %>
</body>
</html>
