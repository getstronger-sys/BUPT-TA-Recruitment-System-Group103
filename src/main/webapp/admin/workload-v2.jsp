<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="bupt.ta.model.AdminSettings" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%
    List<AdminService.WorkloadRow> workloadRows = (List<AdminService.WorkloadRow>) request.getAttribute("workloadRows");
    if (workloadRows == null) workloadRows = java.util.Collections.emptyList();
    Double avgWorkload = (Double) request.getAttribute("avgWorkload");
    if (avgWorkload == null) avgWorkload = 0.0;
    Double avgEstimatedHours = (Double) request.getAttribute("avgEstimatedHours");
    if (avgEstimatedHours == null) avgEstimatedHours = 0.0;
    AdminSettings settings = (AdminSettings) request.getAttribute("adminSettings");
    if (settings == null) settings = new AdminSettings();
    request.setAttribute("adminNavActive", "workload");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>TA Workload - Admin</title>
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
                <div class="icon-dot">D</div>
                <div class="icon-dot active">W</div>
                <div class="icon-dot">M</div>
                <div class="icon-dot">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Workload</p>
                <h1>TA Overall Workload</h1>
                <p class="ta-page-lead">Review selected and pending load distribution across TAs to keep recruitment decisions balanced and policy-compliant.</p>
            </header>
            <div class="context-card admin-workload-rule">
                <strong>Workload rule</strong>
                <dl class="admin-workload-rule__list">
                    <div class="admin-workload-rule__row">
                        <dt>Average selected jobs per TA</dt>
                        <dd><%= String.format("%.1f", avgWorkload) %></dd>
                    </div>
                    <div class="admin-workload-rule__row">
                        <dt>Average estimated hours (structured arrangements)</dt>
                        <dd><%= String.format("%.1f", avgEstimatedHours) %> h</dd>
                    </div>
                    <% if (settings.usesHourWorkloadLimit()) { %>
                    <div class="admin-workload-rule__row">
                        <dt>Hour cap</dt>
                        <dd><%= String.format("%.1f", settings.getMaxWorkloadHoursPerTa()) %> h</dd>
                    </div>
                    <% } else { %>
                    <div class="admin-workload-rule__row">
                        <dt>Job cap</dt>
                        <dd><%= settings.hasWorkloadLimit() ? settings.getMaxSelectedJobsPerTa() : 0 %></dd>
                    </div>
                    <% } %>
                    <div class="admin-workload-rule__row">
                        <dt>Auto-close pending</dt>
                        <dd><%= settings.isAutoClosePendingWhenLimitReached() ? "ON" : "OFF" %></dd>
                    </div>
                </dl>
            </div>
            <p><a href="${pageContext.request.contextPath}/admin/export-workload" class="btn btn-primary">Export to CSV</a></p>

            <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
            <div class="table-scroll-wrap">
                <table class="admin-table">
                    <thead>
                    <tr>
                        <th>TA Name</th>
                        <th>User ID</th>
                        <th>Selected</th>
                        <th>Est. hours</th>
                        <th>Pending</th>
                        <th>Average flag</th>
                        <th>Limit flag</th>
                        <th>Selected jobs</th>
                    </tr>
                    </thead>
                    <tbody>
                    <% for (AdminService.WorkloadRow row : workloadRows) { %>
                    <tr class="<%= row.isAboveLimit() ? "workload-high" : row.isAtOrOverLimit() ? "workload-warning" : "" %>">
                        <td><a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getApplicantId() %>" class="admin-inline-link"><%= escHtml(row.getApplicantName()) %></a></td>
                        <td><%= escHtml(row.getApplicantId()) %></td>
                        <td><strong><%= row.getSelectedCount() %></strong></td>
                        <td><%= String.format("%.1f", row.getEstimatedSelectedHours()) %></td>
                        <td><%= row.getPendingCount() %></td>
                        <td>
                            <% if (row.isAboveAverage()) { %>
                            <span class="balance-warn">Above average</span>
                            <% } else { %>
                            <span class="balance-ok">Balanced</span>
                            <% } %>
                        </td>
                        <td>
                            <% if (!settings.hasWorkloadLimit()) { %>
                            <span class="muted-inline">No limit</span>
                            <% } else if (settings.usesHourWorkloadLimit()) { %>
                                <% if (row.isAboveLimit()) { %>
                                <span class="balance-warn">Over hour cap</span>
                                <% } else if (row.isAtOrOverLimit()) { %>
                                <span class="balance-warn">At hour cap</span>
                                <% } else { %>
                                <span class="balance-ok">Within hour cap</span>
                                <% } %>
                            <% } else if (row.isAboveLimit()) { %>
                            <span class="balance-warn">Over job cap</span>
                            <% } else if (row.isAtOrOverLimit()) { %>
                            <span class="balance-warn">At job cap</span>
                            <% } else { %>
                            <span class="balance-ok">Within job cap</span>
                            <% } %>
                        </td>
                        <td><%= row.getSelectedJobTitles().isEmpty() ? "-" : escHtml(String.join(", ", row.getSelectedJobTitles())) %></td>
                    </tr>
                    <% } %>
                    <% if (workloadRows.isEmpty()) { %>
                    <tr>
                        <td colspan="8">No TA has been selected for any job yet.</td>
                    </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Interpretation</div>
                <p class="widget-line">Est. hours come from each job’s work arrangements (per-TA average), same as MO planning.</p>
                <p class="widget-line">With hour cap on, limit flags compare total estimated hours to the admin hour ceiling.</p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/users">Open user directory</a></p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
