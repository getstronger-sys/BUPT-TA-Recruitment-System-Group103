<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.Application" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.SiteNotification" %>
<%@ page import="bupt.ta.model.TAProfile" %>
<%@ page import="bupt.ta.model.User" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%
    AdminService.TADetailReport report = (AdminService.TADetailReport) request.getAttribute("report");
    if (report == null) {
        response.sendRedirect(request.getContextPath() + "/admin/users?error=invalid_ta");
        return;
    }
    User user = report.getUser();
    TAProfile profile = report.getProfile();
    String displayName = user.getRealName() != null && !user.getRealName().trim().isEmpty() ? user.getRealName().trim()
            : (user.getUsername() != null && !user.getUsername().trim().isEmpty() ? user.getUsername().trim() : user.getId());
    request.setAttribute("adminNavActive", "users");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= escHtml(displayName) %> - Admin TA Detail</title>
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
                <div class="icon-dot">D</div>
                <div class="icon-dot">W</div>
                <div class="icon-dot">M</div>
                <div class="icon-dot">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot active">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <p class="breadcrumb-line"><a href="${pageContext.request.contextPath}/admin/users">&larr; Back to user directory</a></p>
            <header class="ta-page-header">
                <p class="ta-page-kicker">User detail</p>
                <h1>TA Detail: <%= escHtml(displayName) %></h1>
                <p class="ta-page-lead">Read-only traceability for this TA account (profile, saved jobs, applications, interviews, notifications). Use it to audit submissions, timestamps, status changes, and system messages—nothing here is editable.</p>
            </header>

            <div class="stats-row admin-stat-grid">
                <div class="stat-card">
                    <div class="stat-icon">A</div>
                    <div>
                        <div class="stat-title">Applications</div>
                        <div class="stat-value"><%= report.getTotalApplications() %></div>
                        <div class="stat-meta">Pending <%= report.getPendingCount() %> | Interview <%= report.getInterviewCount() %> | Selected <%= report.getSelectedCount() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">W</div>
                    <div>
                        <div class="stat-title">Decision outcomes</div>
                        <div class="stat-value"><%= report.getSelectedCount() + report.getRejectedCount() + report.getAutoClosedCount() + report.getWithdrawnCount() %></div>
                        <div class="stat-meta">Waitlist <%= report.getWaitlistCount() %> | Rejected <%= report.getRejectedCount() %> | Auto-closed <%= report.getAutoClosedCount() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">S</div>
                    <div>
                        <div class="stat-title">Saved jobs</div>
                        <div class="stat-value"><%= report.getSavedJobsCount() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">N</div>
                    <div>
                        <div class="stat-title">Notifications</div>
                        <div class="stat-value"><%= report.getNotificationCount() %></div>
                        <div class="stat-meta">Unread <%= report.getUnreadNotificationCount() %> | Read <%= report.getReadNotificationCount() %></div>
                    </div>
                </div>
            </div>

            <div class="admin-metric-strip" role="region" aria-label="Application status counts">
                <span class="admin-metric-strip__item">Pending <strong><%= report.getPendingCount() %></strong></span>
                <span class="admin-metric-strip__item">Interview <strong><%= report.getInterviewCount() %></strong></span>
                <span class="admin-metric-strip__item">Waitlist <strong><%= report.getWaitlistCount() %></strong></span>
                <span class="admin-metric-strip__item">Selected <strong><%= report.getSelectedCount() %></strong></span>
                <span class="admin-metric-strip__item">Rejected <strong><%= report.getRejectedCount() %></strong></span>
                <span class="admin-metric-strip__item">Withdrawn <strong><%= report.getWithdrawnCount() %></strong></span>
                <span class="admin-metric-strip__item">Auto-closed <strong><%= report.getAutoClosedCount() %></strong></span>
            </div>

            <div class="detail-grid admin-detail-grid">
                <section class="detail-card">
                    <h3>Account record</h3>
                    <dl class="admin-kv-list">
                        <dt>User ID</dt><dd><code><%= escHtml(user.getId()) %></code></dd>
                        <dt>Role</dt><dd>TA</dd>
                        <dt>Username</dt><dd><%= escHtml(user.getUsername() != null ? user.getUsername() : "-") %></dd>
                        <dt>Real name</dt><dd><%= escHtml(user.getRealName() != null && !user.getRealName().isEmpty() ? user.getRealName() : "-") %></dd>
                        <dt>Email</dt><dd><%= escHtml(profile.getEmail() != null && !profile.getEmail().isEmpty() ? profile.getEmail() : (user.getEmail() != null ? user.getEmail() : "-")) %></dd>
                        <dt>Student ID</dt><dd><%= escHtml(profile.getStudentId() != null && !profile.getStudentId().isEmpty() ? profile.getStudentId() : (user.getStudentId() != null ? user.getStudentId() : "-")) %></dd>
                        <dt>Phone</dt><dd><%= escHtml(profile.getPhone() != null && !profile.getPhone().isEmpty() ? profile.getPhone() : "-") %></dd>
                    </dl>
                </section>

                <section class="detail-card">
                    <h3>Academic profile</h3>
                    <dl class="admin-kv-list">
                        <dt>Degree</dt><dd><%= escHtml(profile.getDegree() != null && !profile.getDegree().isEmpty() ? profile.getDegree() : "-") %></dd>
                        <dt>Programme</dt><dd><%= escHtml(profile.getProgramme() != null && !profile.getProgramme().isEmpty() ? profile.getProgramme() : "-") %></dd>
                        <dt>Year of study</dt><dd><%= escHtml(profile.getYearOfStudy() != null && !profile.getYearOfStudy().isEmpty() ? profile.getYearOfStudy() : "-") %></dd>
                        <dt>Availability</dt><dd class="pre-wrap"><%= escHtml(profile.getAvailability() != null && !profile.getAvailability().isEmpty() ? profile.getAvailability() : "-") %></dd>
                        <dt>Skills</dt><dd><%= profile.getSkills() != null && !profile.getSkills().isEmpty() ? escHtml(String.join(", ", profile.getSkills())) : "-" %></dd>
                        <dt>CV</dt>
                        <dd>
                            <% if (profile.getCvFilePath() != null && !profile.getCvFilePath().trim().isEmpty()) { %>
                            <a href="${pageContext.request.contextPath}/view-cv?userId=<%= user.getId() %>" target="_blank" rel="noopener">View CV</a>
                            <span class="muted-inline"> | </span>
                            <a href="${pageContext.request.contextPath}/view-cv?userId=<%= user.getId() %>&amp;download=1">Download</a>
                            <% } else { %>
                            Not uploaded
                            <% } %>
                        </dd>
                    </dl>
                </section>
            </div>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Profile narrative</span>
                </summary>
                <div class="admin-section-collapse__body">
                <div class="admin-text-block">
                    <strong>TA experience</strong>
                    <p class="pre-wrap"><%= escHtml(profile.getTaExperience() != null && !profile.getTaExperience().isEmpty() ? profile.getTaExperience() : "Not provided.") %></p>
                </div>
                <div class="admin-text-block">
                    <strong>Introduction</strong>
                    <p class="pre-wrap"><%= escHtml(profile.getIntroduction() != null && !profile.getIntroduction().isEmpty() ? profile.getIntroduction() : "Not provided.") %></p>
                </div>
                </div>
            </details>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Saved jobs</span>
                    <span class="admin-section-collapse__meta"><%= report.getSavedJobsCount() %> saved</span>
                </summary>
                <div class="admin-section-collapse__body">
                <% if (report.getSavedJobs().isEmpty()) { %>
                <p class="section-empty section-empty--card">This TA has no saved jobs.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                    <table class="admin-table admin-table--compact">
                        <thead>
                        <tr>
                            <th>Job</th>
                            <th>Module</th>
                            <th>Status</th>
                            <th>Deadline</th>
                            <th>Posted by</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (Job job : report.getSavedJobs()) { %>
                        <tr>
                            <td><strong><%= escHtml(job.getTitle() != null ? job.getTitle() : job.getId()) %></strong><div class="admin-row-subtext">Job ID: <code><%= escHtml(job.getId()) %></code></div></td>
                            <td><%= escHtml(job.getModuleCode() != null ? job.getModuleCode() : "-") %></td>
                            <td><span class="status-pill <%= "OPEN".equalsIgnoreCase(job.getStatus()) ? "status-pill-pending" : "status-pill-rejected" %>"><%= escHtml(job.getStatus() != null ? job.getStatus() : "-") %></span></td>
                            <td><%= escHtml(job.getDeadline() != null && !job.getDeadline().isEmpty() ? job.getDeadline() : "-") %></td>
                            <td><%= escHtml(job.getPostedByName() != null && !job.getPostedByName().isEmpty() ? job.getPostedByName() : job.getPostedBy()) %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
                </div>
            </details>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Application history</span>
                    <span class="admin-section-collapse__meta"><%= report.getApplicationRows().size() %> application(s)</span>
                </summary>
                <div class="admin-section-collapse__body">
                <% if (report.getApplicationRows().isEmpty()) { %>
                <p class="section-empty section-empty--card">No applications recorded for this TA.</p>
                <% } else { %>
                <p class="admin-section-hint muted-inline">Application IDs, timestamps, and notes stay visible here for traceability. Scroll horizontally on narrow screens.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Job</th>
                            <th>Applied</th>
                            <th>Status</th>
                            <th>Interview record</th>
                            <th>Notes</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.AdminApplicationRow row : report.getApplicationRows()) {
                               Application app = row.getApplication();
                               Job job = row.getJob();
                               String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "UNKNOWN";
                               String statusClass = "status-pill-pending";
                               if ("SELECTED".equals(status)) statusClass = "status-pill-selected";
                               else if ("REJECTED".equals(status) || "WITHDRAWN".equals(status) || AdminService.STATUS_AUTO_CLOSED.equals(status)) statusClass = "status-pill-rejected";
                               else if ("INTERVIEW".equals(status)) statusClass = "status-pill-interview";
                        %>
                        <tr>
                            <td>
                                <code><%= escHtml(app.getId()) %></code>
                                <div class="admin-row-subtext">Applicant ID: <code><%= escHtml(app.getApplicantId()) %></code></div>
                            </td>
                            <td>
                                <strong><%= escHtml(job != null && job.getTitle() != null ? job.getTitle() : app.getJobId()) %></strong>
                                <div class="admin-row-subtext"><%= escHtml(job != null && job.getModuleCode() != null ? job.getModuleCode() : "-") %></div>
                            </td>
                            <td><%= escHtml(app.getAppliedAt() != null && !app.getAppliedAt().isEmpty() ? app.getAppliedAt() : "-") %></td>
                            <td><span class="status-pill <%= statusClass %>"><%= escHtml(status) %></span></td>
                            <td>
                                <div class="admin-summary-stack">
                                    <span>Time: <%= escHtml(app.getInterviewTime() != null && !app.getInterviewTime().isEmpty() ? app.getInterviewTime() : "-") %></span>
                                    <span>Location: <%= escHtml(app.getInterviewLocation() != null && !app.getInterviewLocation().isEmpty() ? app.getInterviewLocation() : "-") %></span>
                                    <span>Assessment: <%= escHtml(app.getInterviewAssessment() != null && !app.getInterviewAssessment().isEmpty() ? app.getInterviewAssessment() : "-") %></span>
                                </div>
                            </td>
                            <td class="pre-wrap"><%= escHtml(app.getNotes() != null && !app.getNotes().isEmpty() ? app.getNotes() : "-") %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
                </div>
            </details>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Site notifications</span>
                    <span class="admin-section-collapse__meta"><%= report.getNotificationCount() %> notification(s)</span>
                </summary>
                <div class="admin-section-collapse__body">
                <% if (report.getNotifications().isEmpty()) { %>
                <p class="section-empty section-empty--card">No site notifications have been sent to this TA.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Created</th>
                            <th>Notification</th>
                            <th>Status</th>
                            <th>Kind</th>
                            <th>Title</th>
                            <th>Body</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (SiteNotification note : report.getNotifications()) { %>
                        <tr>
                            <td><%= escHtml(note.getCreatedAt() != null && !note.getCreatedAt().isEmpty() ? note.getCreatedAt() : "-") %></td>
                            <td>
                                <code><%= escHtml(note.getId()) %></code>
                                <div class="admin-row-subtext">Application <%= escHtml(note.getApplicationId() != null && !note.getApplicationId().isEmpty() ? note.getApplicationId() : "-") %></div>
                            </td>
                            <td><span class="admin-read-state <%= note.isRead() ? "admin-read-state-read" : "admin-read-state-unread" %>"><%= note.isRead() ? "Read" : "Unread" %></span></td>
                            <td><%= escHtml(note.getKind() != null && !note.getKind().isEmpty() ? note.getKind() : "-") %></td>
                            <td><%= escHtml(note.getTitle() != null && !note.getTitle().isEmpty() ? note.getTitle() : "-") %></td>
                            <td class="pre-wrap"><%= escHtml(note.getBody() != null && !note.getBody().isEmpty() ? note.getBody() : "-") %></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
                <% } %>
                </div>
            </details>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">TA Snapshot</div>
                <p class="widget-line">Selected: <%= report.getSelectedCount() %></p>
                <p class="widget-line">Pending: <%= report.getPendingCount() %> | Interview: <%= report.getInterviewCount() %></p>
                <p class="widget-line">Waitlist: <%= report.getWaitlistCount() %> | Withdrawn: <%= report.getWithdrawnCount() %></p>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Quick Links</div>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/users">Back to user directory</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/workload">Review workload</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/monitoring">Open monitoring</a></p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
