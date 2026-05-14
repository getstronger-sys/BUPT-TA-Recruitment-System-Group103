<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.AdminSettings" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%@ page import="bupt.ta.service.NotificationReminderService" %>
<%
    AdminSettings settings = (AdminSettings) request.getAttribute("adminSettings");
    if (settings == null) settings = new AdminSettings();
    AdminService.MonitoringReport monitoring = (AdminService.MonitoringReport) request.getAttribute("monitoring");
    NotificationReminderService.ReminderPreview reminderPreview =
            (NotificationReminderService.ReminderPreview) request.getAttribute("reminderPreview");
    if (reminderPreview == null) {
        reminderPreview = new NotificationReminderService.ReminderPreview(false, 0, 0, 0);
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
    request.setAttribute("adminNavActive", "monitoring");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Admin Monitoring - TA Recruitment</title>
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
                <div class="icon-dot">W</div>
                <div class="icon-dot active">M</div>
                <div class="icon-dot">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Health</p>
                <h1>Application Monitoring</h1>
                <p class="ta-page-lead">Review exception cases that may need admin attention: workload-rule conflicts, incomplete interview notices, inactive-job activity, and job capacity problems.</p>
            </header>
            <div class="context-card admin-workload-rule">
                <strong>Current workload rule</strong>
                <dl class="admin-workload-rule__list">
                    <div class="admin-workload-rule__row">
                        <dt>Max selected jobs per TA</dt>
                        <dd><%= settings.hasWorkloadLimit() ? settings.getMaxSelectedJobsPerTa() : 0 %></dd>
                    </div>
                    <div class="admin-workload-rule__row">
                        <dt>Auto-close pending</dt>
                        <dd><%= settings.isAutoClosePendingWhenLimitReached() ? "ON" : "OFF" %></dd>
                    </div>
                </dl>
            </div>
            <% if ("1".equals(request.getParameter("remindDone"))) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("remindConfigured"))) { %>
            <p class="success">Unread reminder emails processed. Attempted: <strong><%= escHtml(request.getParameter("remindAttempted")) %></strong>, sent: <strong><%= escHtml(request.getParameter("remindSent")) %></strong>, skipped: <strong><%= escHtml(request.getParameter("remindSkipped")) %></strong>.</p>
            <% } else { %>
            <p class="error">Unread reminders were not sent because SMTP email is not configured yet.</p>
            <% } %>
            </div>
            <% } %>

            <section class="detail-card">
                <h3>Unread message reminders</h3>
                <p class="muted-inline">Batch email reminders for users who still have unread in-app recruitment updates.</p>
                <p><strong>Users with unread messages:</strong> <%= reminderPreview.getUsersWithUnread() %> | <strong>Unread messages:</strong> <%= reminderPreview.getUnreadMessages() %> | <strong>Users with an email ready to remind:</strong> <%= reminderPreview.getRemindableUsers() %></p>
                <p><strong>Email channel:</strong> <%= reminderPreview.isEmailConfigured() ? "Configured" : "Not configured" %></p>
                <form action="${pageContext.request.contextPath}/admin/monitoring" method="post" class="inline-form">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <input type="hidden" name="action" value="sendUnreadReminders">
                    <button type="submit" class="btn btn-primary" <%= reminderPreview.getUsersWithUnread() == 0 ? "disabled" : "" %>>Send unread message reminders</button>
                </form>
                <% if (!reminderPreview.isEmailConfigured()) { %>
                <p class="muted-inline">Configure SMTP via `TA_MAIL_HOST`, `TA_MAIL_PORT`, `TA_MAIL_USERNAME`, `TA_MAIL_PASSWORD`, and `TA_MAIL_FROM` to enable delivery.</p>
                <% } %>
            </section>

            <div class="stats-row admin-stat-grid">
                <div class="stat-card">
                    <div class="stat-icon">!</div>
                    <div>
                        <div class="stat-title">Total issues</div>
                        <div class="stat-value"><%= monitoring.getTotalIssues() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">L</div>
                    <div>
                        <div class="stat-title">Limit alerts</div>
                        <div class="stat-value"><%= monitoring.getLimitAlerts().size() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">I</div>
                    <div>
                        <div class="stat-title">Interview notice issues</div>
                        <div class="stat-value"><%= monitoring.getInterviewNoticeAlerts().size() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">C</div>
                    <div>
                        <div class="stat-title">Capacity issues</div>
                        <div class="stat-value"><%= monitoring.getCapacityAlerts().size() %></div>
                    </div>
                </div>
            </div>

            <section class="detail-card">
                <h3>Workload-limit conflicts</h3>
                <p class="muted-inline">Applicants who are already at or over the configured workload cap but still have pending applications open.</p>
                <% if (monitoring.getLimitAlerts().isEmpty()) { %>
                <p class="section-empty section-empty--card">No workload-limit conflicts found.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Applicant</th>
                            <th>User ID</th>
                            <th>Load vs cap</th>
                            <th>Selected jobs</th>
                            <th>Pending jobs</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.LimitAlert row : monitoring.getLimitAlerts()) { %>
                        <tr>
                            <td><a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getApplicantId() %>" class="admin-inline-link"><%= escHtml(row.getApplicantName()) %></a></td>
                            <td><%= escHtml(row.getApplicantId()) %></td>
                            <td><%= escHtml(row.getLoadVsCap()) %></td>
                            <td><strong><%= row.getSelectedCount() %></strong></td>
                            <td><%= row.getPendingCount() %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            </section>

            <section class="detail-card">
                <h3>Interview records missing notice details</h3>
                <% if (monitoring.getInterviewNoticeAlerts().isEmpty()) { %>
                <p class="section-empty section-empty--card">All interview-stage applications have time and location filled in.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Applicant</th>
                            <th>Job</th>
                            <th>Module</th>
                            <th>Missing</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.InterviewNoticeAlert row : monitoring.getInterviewNoticeAlerts()) { %>
                        <tr>
                            <td><%= escHtml(row.getApplicationId()) %></td>
                            <td><a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getApplicantId() %>" class="admin-inline-link"><%= escHtml(row.getApplicantName()) %></a></td>
                            <td><%= escHtml(row.getJobTitle()) %></td>
                            <td><%= escHtml(row.getModuleCode()) %></td>
                            <td>
                                <% if (row.isMissingTime()) { %>Time<% } %>
                                <% if (row.isMissingTime() && row.isMissingLocation()) { %> + <% } %>
                                <% if (row.isMissingLocation()) { %>Location<% } %>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            </section>

            <section class="detail-card">
                <h3>Active applications on inactive jobs</h3>
                <% if (monitoring.getInactiveJobAlerts().isEmpty()) { %>
                <p class="section-empty section-empty--card">No pending or interview applications remain on closed / expired jobs.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Applicant</th>
                            <th>Job</th>
                            <th>Status</th>
                            <th>Issue</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.ApplicationAlert row : monitoring.getInactiveJobAlerts()) { %>
                        <tr>
                            <td><%= escHtml(row.getApplicationId()) %></td>
                            <td><a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getApplicantId() %>" class="admin-inline-link"><%= escHtml(row.getApplicantName()) %></a></td>
                            <td><%= escHtml(row.getJobTitle()) %> (<%= escHtml(row.getModuleCode()) %>)</td>
                            <td><%= escHtml(row.getStatus()) %></td>
                            <td><%= escHtml(row.getIssue()) %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            </section>

            <section class="detail-card">
                <h3>Applications pointing to missing jobs</h3>
                <% if (monitoring.getMissingJobAlerts().isEmpty()) { %>
                <p class="section-empty section-empty--card">No missing-job references found.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Applicant</th>
                            <th>Job ID</th>
                            <th>Status</th>
                            <th>Issue</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.ApplicationAlert row : monitoring.getMissingJobAlerts()) { %>
                        <tr>
                            <td><%= escHtml(row.getApplicationId()) %></td>
                            <td><a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getApplicantId() %>" class="admin-inline-link"><%= escHtml(row.getApplicantName()) %></a></td>
                            <td><%= escHtml(row.getJobTitle()) %></td>
                            <td><%= escHtml(row.getStatus()) %></td>
                            <td><%= escHtml(row.getIssue()) %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            </section>

            <section class="detail-card">
                <h3>Jobs over capacity</h3>
                <% if (monitoring.getCapacityAlerts().isEmpty()) { %>
                <p class="section-empty section-empty--card">No job exceeds its max-applicant capacity.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Job</th>
                            <th>Module</th>
                            <th>Selected</th>
                            <th>Max applicants</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.CapacityAlert row : monitoring.getCapacityAlerts()) { %>
                        <tr>
                            <td><%= escHtml(row.getJobTitle()) %></td>
                            <td><%= escHtml(row.getModuleCode()) %></td>
                            <td><strong><%= row.getSelectedCount() %></strong></td>
                            <td><%= row.getMaxApplicants() %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
            </section>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Admin Actions</div>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/dashboard">Adjust workload settings</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/workload">Review TA distribution</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/users">Open user directory</a></p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
