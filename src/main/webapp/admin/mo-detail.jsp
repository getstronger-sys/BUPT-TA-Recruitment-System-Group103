<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="bupt.ta.model.Application" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.User" %>
<%@ page import="bupt.ta.model.WorkArrangementItem" %>
<%@ page import="bupt.ta.model.AssignedModule" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%
    AdminService.MODetailReport report = (AdminService.MODetailReport) request.getAttribute("report");
    if (report == null) {
        response.sendRedirect(request.getContextPath() + "/admin/users?error=invalid_mo");
        return;
    }
    User user = report.getUser();
    String displayName = user.getRealName() != null && !user.getRealName().trim().isEmpty() ? user.getRealName().trim()
            : (user.getUsername() != null && !user.getUsername().trim().isEmpty() ? user.getUsername().trim() : user.getId());
    List<AssignedModule> assignedModules = (List<AssignedModule>) request.getAttribute("assignedModules");
    if (assignedModules == null) assignedModules = java.util.Collections.emptyList();
    StringBuilder assignedModulesText = new StringBuilder();
    for (AssignedModule m : assignedModules) {
        if (m == null || m.getModuleCode() == null || m.getModuleCode().trim().isEmpty()) continue;
        if (assignedModulesText.length() > 0) assignedModulesText.append("\n");
        assignedModulesText.append(m.getModuleCode().trim().toUpperCase());
        if (m.getModuleName() != null && !m.getModuleName().trim().isEmpty()) {
            assignedModulesText.append(" | ").append(m.getModuleName().trim());
        }
    }
    String assignError = (String) request.getAttribute("assignError");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= escHtml(displayName) %> - Admin MO Detail</title>
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
                <div class="icon-dot">M</div>
                <div class="icon-dot active">U</div>
            </div>
            <aside class="side-nav">
                <a href="${pageContext.request.contextPath}/admin/dashboard">Summary</a>
                <a href="${pageContext.request.contextPath}/admin/workload">Workload</a>
                <a href="${pageContext.request.contextPath}/admin/monitoring">Monitoring</a>
                <a class="active" href="${pageContext.request.contextPath}/admin/users">Users</a>
            </aside>
        </div>
        <main class="main-panel admin-main">
            <p class="breadcrumb-line"><a href="${pageContext.request.contextPath}/admin/users">&larr; Back to user directory</a></p>
            <h1>MO Detail: <%= escHtml(displayName) %></h1>
            <p class="ta-page-lead">Read-only traceability for this module organiser (postings, risks, applications). Only <strong>Assigned modules for this term</strong> can be edited below; everything else is for auditing.</p>
            <% if ("1".equals(request.getParameter("assignedUpdated"))) { %>
            <p class="success">Assigned modules updated successfully.</p>
            <% } %>
            <% if (assignError != null && !assignError.trim().isEmpty()) { %>
            <p class="error"><%= escHtml(assignError) %></p>
            <% } %>

            <div class="stats-row admin-stat-grid">
                <div class="stat-card">
                    <div class="stat-icon">J</div>
                    <div>
                        <div class="stat-title">Posted jobs</div>
                        <div class="stat-value"><%= report.getTotalJobs() %></div>
                        <div class="stat-meta">Active <%= report.getActiveJobs() %> | Inactive <%= report.getInactiveJobs() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">A</div>
                    <div>
                        <div class="stat-title">Applications received</div>
                        <div class="stat-value"><%= report.getTotalApplications() %></div>
                        <div class="stat-meta">Distinct applicants <%= report.getDistinctApplicants() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">S</div>
                    <div>
                        <div class="stat-title">Decision distribution</div>
                        <div class="stat-value"><%= report.getSelectedCount() %></div>
                        <div class="stat-meta">Interview <%= report.getInterviewCount() %> | Waitlist <%= report.getWaitlistCount() %> | Rejected <%= report.getRejectedCount() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">R</div>
                    <div>
                        <div class="stat-title">Risk flags</div>
                        <div class="stat-value"><%= report.getCapacityRiskCount() + report.getInactiveActiveRiskCount() %></div>
                        <div class="stat-meta">Capacity <%= report.getCapacityRiskCount() %> | Inactive-active <%= report.getInactiveActiveRiskCount() %></div>
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

            <section class="detail-card">
                <h3>Account record</h3>
                <dl class="admin-kv-list">
                    <dt>User ID</dt><dd><code><%= escHtml(user.getId()) %></code></dd>
                    <dt>Role</dt><dd>MO</dd>
                    <dt>Username</dt><dd><%= escHtml(user.getUsername() != null ? user.getUsername() : "-") %></dd>
                    <dt>Real name</dt><dd><%= escHtml(user.getRealName() != null && !user.getRealName().isEmpty() ? user.getRealName() : "-") %></dd>
                    <dt>Email</dt><dd><%= escHtml(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "-") %></dd>
                </dl>
            </section>

            <details class="detail-card admin-section-collapse"
                <% if ("1".equals(request.getParameter("assignedUpdated")) || (assignError != null && !assignError.trim().isEmpty())) { %>open<% } %>>
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Assigned modules for this term</span>
                </summary>
                <div class="admin-section-collapse__body">
                <p class="muted-inline">MO can only post jobs for these module codes. One line per module, format: <code>MODULE_CODE | Module Name</code>.</p>
                <form action="${pageContext.request.contextPath}/admin/mo-detail" method="post" class="form">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <input type="hidden" name="userId" value="<%= escHtml(user.getId()) %>">
                    <label>Assigned modules list</label>
                    <div class="module-editor" data-module-editor>
                        <div class="module-editor__toolbar">
                            <button type="button" class="btn btn-secondary" data-add-row>Add row</button>
                            <button type="button" class="btn btn-secondary" data-sort>Sort by code</button>
                            <button type="button" class="btn btn-secondary" data-clear>Clear</button>
                        </div>

                        <div class="module-editor__hint muted-inline">
                            Tip: You can paste multiple lines. Supported formats: <code>CODE | Name</code> or <code>CODE</code>.
                        </div>

                        <div class="table-scroll-wrap module-editor__table-wrap" role="region" aria-label="Assigned modules table">
                            <table class="admin-table admin-table--compact module-editor__table">
                                <thead>
                                <tr>
                                    <th style="width: 220px;">Module code</th>
                                    <th>Module name (optional)</th>
                                    <th style="width: 140px;">Order</th>
                                </tr>
                                </thead>
                                <tbody data-rows></tbody>
                            </table>
                        </div>

                        <div class="module-editor__summary">
                            <span class="muted-inline">Valid rows: <strong data-valid-count>0</strong></span>
                            <span class="muted-inline"> | Duplicates: <strong data-dup-count>0</strong></span>
                            <span class="muted-inline"> | Invalid: <strong data-invalid-count>0</strong></span>
                        </div>

                        <div class="module-editor__paste">
                            <label class="module-editor__paste-label">Paste / import (optional)</label>
                            <textarea rows="3" class="module-editor__paste-box" placeholder="EBU6304 | Software Engineering&#10;EBU6202 | Data Structures and Algorithms" data-paste-box></textarea>
                            <div class="module-editor__paste-actions">
                                <button type="button" class="btn btn-primary" data-import>Import</button>
                                <span class="muted-inline" data-import-status></span>
                            </div>
                        </div>
                    </div>

                    <textarea name="assignedModulesText" rows="6" class="module-editor__raw" placeholder="EBU6304 | Software Engineering&#10;EBU6202 | Data Structures and Algorithms"><%= escHtml(assignedModulesText.toString()) %></textarea>
                    <button type="submit" class="btn btn-primary">Save assigned modules</button>
                </form>
                </div>
            </details>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Posted jobs summary</span>
                    <span class="admin-section-collapse__meta"><%= report.getTotalJobs() %> job(s)</span>
                </summary>
                <div class="admin-section-collapse__body">
                <p class="admin-section-hint muted-inline">Overview table; expand a row in <strong>Posting details</strong> below for full job text and work arrangements.</p>
                <% if (report.getJobRows().isEmpty()) { %>
                <p class="section-empty section-empty--card">This MO has not posted any jobs.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Job</th>
                            <th>Status</th>
                            <th>Deadline</th>
                            <th>Applications</th>
                            <th>Selection</th>
                            <th>Capacity</th>
                            <th>Risk flags</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.MOJobDetailRow row : report.getJobRows()) {
                               Job job = row.getJob();
                        %>
                        <tr>
                            <td>
                                <strong><%= escHtml(job.getTitle() != null ? job.getTitle() : job.getId()) %></strong>
                                <div class="admin-row-subtext"><%= escHtml(job.getModuleCode() != null ? job.getModuleCode() : "-") %> | Job ID: <code><%= escHtml(job.getId()) %></code></div>
                            </td>
                            <td><span class="status-pill <%= "OPEN".equalsIgnoreCase(job.getStatus()) ? "status-pill-pending" : "status-pill-rejected" %>"><%= escHtml(job.getStatus() != null ? job.getStatus() : "-") %></span></td>
                            <td><%= escHtml(job.getDeadline() != null && !job.getDeadline().isEmpty() ? job.getDeadline() : "-") %></td>
                            <td><strong><%= row.getTotalApplications() %></strong><div class="admin-row-subtext">Pending <%= row.getPendingCount() %> | Interview <%= row.getInterviewCount() %> | Waitlist <%= row.getWaitlistCount() %></div></td>
                            <td><strong><%= row.getSelectedCount() %></strong><div class="admin-row-subtext">Rejected <%= row.getRejectedCount() %> | Withdrawn <%= row.getWithdrawnCount() %> | Auto-closed <%= row.getAutoClosedCount() %></div></td>
                            <td>
                                <% if (job.getMaxApplicants() > 0) { %>
                                <%= row.getSelectedCount() %> / <%= job.getMaxApplicants() %>
                                <% } else { %>
                                No limit
                                <% } %>
                            </td>
                            <td>
                                <div class="admin-summary-stack">
                                    <span class="<%= row.isAtOrOverCapacity() ? "balance-warn" : "balance-ok" %>"><%= row.isAtOrOverCapacity() ? "At or over capacity" : "Within capacity" %></span>
                                    <% if (row.isInactiveWithActiveApplications()) { %>
                                    <span class="balance-warn">Inactive job still has active applications</span>
                                    <% } %>
                                    <% if (row.isOverCapacity()) { %>
                                    <span class="balance-warn">Selected count exceeds max applicants</span>
                                    <% } %>
                                </div>
                            </td>
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
                    <span class="admin-section-collapse__title">Posting details</span>
                    <span class="admin-section-collapse__meta"><%= report.getJobRows().size() %> posting(s)</span>
                </summary>
                <div class="admin-section-collapse__body">
                <p class="admin-section-hint muted-inline">Each job is collapsed by default—click the bar to show description, responsibilities, and arrangements.</p>
                <% if (report.getJobRows().isEmpty()) { %>
                <p class="section-empty section-empty--card">No posting detail is available because this MO has no jobs.</p>
                <% } else { %>
                <div class="admin-posting-stack">
                    <% for (AdminService.MOJobDetailRow row : report.getJobRows()) {
                           Job job = row.getJob();
                    %>
                    <details class="admin-job-expand">
                        <summary class="admin-job-expand__summary">
                            <span class="admin-job-expand__chev" aria-hidden="true"></span>
                            <span class="admin-job-expand__lead">
                                <span class="admin-job-expand__title"><%= escHtml(job.getTitle() != null ? job.getTitle() : job.getId()) %></span>
                                <span class="admin-job-expand__meta"><%= escHtml(job.getModuleCode() != null ? job.getModuleCode() : "-") %> · Created <%= escHtml(job.getCreatedAt() != null && !job.getCreatedAt().isEmpty() ? job.getCreatedAt() : "-") %></span>
                            </span>
                            <span class="status-pill <%= "OPEN".equalsIgnoreCase(job.getStatus()) ? "status-pill-pending" : "status-pill-rejected" %>"><%= escHtml(job.getStatus() != null ? job.getStatus() : "-") %></span>
                        </summary>
                        <div class="admin-job-expand__body">
                        <div class="admin-posting-subgrid">
                            <div class="admin-kv-panel">
                                <h4 class="admin-kv-panel__title">Core fields</h4>
                                <dl class="admin-kv-list">
                                    <dt>Job ID</dt><dd><code><%= escHtml(job.getId()) %></code></dd>
                                    <dt>Job type</dt><dd><%= escHtml(job.getJobType() != null && !job.getJobType().isEmpty() ? job.getJobType() : "-") %></dd>
                                    <dt>Working hours</dt><dd class="pre-wrap"><%= escHtml(job.getWorkingHours() != null && !job.getWorkingHours().isEmpty() ? job.getWorkingHours() : "-") %></dd>
                                    <dt>Workload</dt><dd class="pre-wrap"><%= escHtml(job.getWorkload() != null && !job.getWorkload().isEmpty() ? job.getWorkload() : "-") %></dd>
                                    <dt>Payment</dt><dd class="pre-wrap"><%= escHtml(job.getPayment() != null && !job.getPayment().isEmpty() ? job.getPayment() : "-") %></dd>
                                    <dt>Deadline</dt><dd><%= escHtml(job.getDeadline() != null && !job.getDeadline().isEmpty() ? job.getDeadline() : "-") %></dd>
                                    <dt>Max applicants</dt><dd><%= job.getMaxApplicants() > 0 ? job.getMaxApplicants() : 0 %></dd>
                                    <dt>Planned recruits</dt><dd><%= job.getTaSlots() > 0 ? job.getTaSlots() : 0 %></dd>
                                </dl>
                            </div>
                            <div class="admin-kv-panel">
                                <h4 class="admin-kv-panel__title">Recruitment planning</h4>
                                <dl class="admin-kv-list">
                                    <dt>Required skills</dt><dd><%= job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty() ? escHtml(String.join(", ", job.getRequiredSkills())) : "-" %></dd>
                                    <dt>Estimated interview time</dt><dd class="pre-wrap"><%= escHtml(job.getInterviewSchedule() != null && !job.getInterviewSchedule().isEmpty() ? job.getInterviewSchedule() : "-") %></dd>
                                    <dt>Estimated interview location</dt><dd class="pre-wrap"><%= escHtml(job.getInterviewLocation() != null && !job.getInterviewLocation().isEmpty() ? job.getInterviewLocation() : "-") %></dd>
                                    <dt>Auto-fill from waitlist</dt><dd><%= job.isAutoFillFromWaitlist() ? "Yes" : "No" %></dd>
                                    <dt>Application summary</dt><dd>Applications <strong><%= row.getTotalApplications() %></strong> | Selected <strong><%= row.getSelectedCount() %></strong> | Waitlist <strong><%= row.getWaitlistCount() %></strong></dd>
                                </dl>
                            </div>
                        </div>
                        <div class="admin-text-block">
                            <strong>Description</strong>
                            <p class="pre-wrap"><%= escHtml(job.getDescription() != null && !job.getDescription().isEmpty() ? job.getDescription() : "Not provided.") %></p>
                        </div>
                        <div class="admin-text-block">
                            <strong>Responsibilities</strong>
                            <p class="pre-wrap"><%= escHtml(job.getResponsibilities() != null && !job.getResponsibilities().isEmpty() ? job.getResponsibilities() : "Not provided.") %></p>
                        </div>
                        <div class="admin-text-block">
                            <strong>Course timeline</strong>
                            <p class="pre-wrap"><%= escHtml(job.getExamTimeline() != null && !job.getExamTimeline().isEmpty() ? job.getExamTimeline() : "Not provided.") %></p>
                        </div>
                        <div class="admin-text-block">
                            <strong>Multi-TA allocation plan</strong>
                            <p class="pre-wrap"><%= escHtml(job.getTaAllocationPlan() != null && !job.getTaAllocationPlan().isEmpty() ? job.getTaAllocationPlan() : "Not provided.") %></p>
                        </div>
                        <div class="admin-text-block">
                            <strong>Work arrangements</strong>
                            <% if (job.getWorkArrangements() == null || job.getWorkArrangements().isEmpty()) { %>
                            <p>No structured work arrangements were provided.</p>
                            <% } else { %>
                            <div class="table-scroll-wrap">
                                <table class="admin-table admin-table--compact">
                                    <thead>
                                    <tr>
                                        <th>Work item</th>
                                        <th>Per-session duration</th>
                                        <th>Occurrences</th>
                                        <th>TA count</th>
                                        <th>Specific time</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <% for (WorkArrangementItem item : job.getWorkArrangements()) { %>
                                    <tr>
                                        <td><%= escHtml(item.getWorkName() != null && !item.getWorkName().isEmpty() ? item.getWorkName() : "-") %></td>
                                        <td class="pre-wrap"><%= escHtml(item.getResolvedSessionDuration() != null && !item.getResolvedSessionDuration().isEmpty() ? item.getResolvedSessionDuration() : "-") %></td>
                                        <td><%= item.getResolvedOccurrenceCount() %></td>
                                        <td><%= item.getTaCount() > 0 ? item.getTaCount() : 0 %></td>
                                        <td class="pre-wrap"><%= escHtml(item.getSpecificTime() != null && !item.getSpecificTime().isEmpty() ? item.getSpecificTime() : "-") %></td>
                                    </tr>
                                    <% } %>
                                    </tbody>
                                </table>
                            </div>
                            <% } %>
                        </div>
                        </div>
                    </details>
                    <% } %>
                </div>
                <% } %>
                </div>
            </details>

            <details class="detail-card admin-section-collapse">
                <summary class="admin-section-collapse__summary">
                    <span class="admin-section-collapse__chev" aria-hidden="true"></span>
                    <span class="admin-section-collapse__title">Application history across MO postings</span>
                    <span class="admin-section-collapse__meta"><%= report.getApplicationRows().size() %> application(s)</span>
                </summary>
                <div class="admin-section-collapse__body">
                <p class="admin-section-hint muted-inline">Scroll horizontally on narrow screens; applicant names link to TA detail.</p>
                <% if (report.getApplicationRows().isEmpty()) { %>
                <p class="section-empty section-empty--card">No applications have been submitted to this MO's jobs.</p>
                <% } else { %>
                <div class="table-scroll-wrap">
                    <table class="admin-table">
                        <thead>
                        <tr>
                            <th>Application</th>
                            <th>Applicant</th>
                            <th>Job</th>
                            <th>Applied</th>
                            <th>Preferred role</th>
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
                            <td><code><%= escHtml(app.getId()) %></code></td>
                            <td>
                                <a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= app.getApplicantId() %>" class="admin-inline-link"><%= escHtml(app.getApplicantName() != null && !app.getApplicantName().isEmpty() ? app.getApplicantName() : app.getApplicantId()) %></a>
                                <div class="admin-row-subtext">Applicant ID: <code><%= escHtml(app.getApplicantId()) %></code></div>
                            </td>
                            <td>
                                <strong><%= escHtml(job != null && job.getTitle() != null ? job.getTitle() : app.getJobId()) %></strong>
                                <div class="admin-row-subtext"><%= escHtml(job != null && job.getModuleCode() != null ? job.getModuleCode() : "-") %></div>
                            </td>
                            <td><%= escHtml(app.getAppliedAt() != null && !app.getAppliedAt().isEmpty() ? app.getAppliedAt() : "-") %></td>
                            <td><%= escHtml(app.getPreferredRole() != null && !app.getPreferredRole().isEmpty() ? app.getPreferredRole() : "-") %></td>
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
        </main>
        <aside class="right-sidebar">
            <div class="widget-card">
                <div class="widget-title">MO Snapshot</div>
                <p class="widget-line">Jobs: <%= report.getTotalJobs() %> | Active: <%= report.getActiveJobs() %></p>
                <p class="widget-line">Pending: <%= report.getPendingCount() %> | Interview: <%= report.getInterviewCount() %></p>
                <p class="widget-line">Selected: <%= report.getSelectedCount() %> | Waitlist: <%= report.getWaitlistCount() %></p>
            </div>
            <div class="widget-card">
                <div class="widget-title">Quick Links</div>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/users">Back to user directory</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/monitoring">Open monitoring</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/workload">Review TA workload</a></p>
            </div>
        </aside>
    </div>
</div>

<style>
    .module-editor { margin: 10px 0 8px; }
    .module-editor__toolbar { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 8px; }
    .module-editor__hint { margin: 6px 0 10px; }
    .module-editor__paste { border: 1px solid rgba(0,0,0,.08); border-radius: 10px; padding: 10px; background: rgba(255,255,255,.6); margin-bottom: 12px; }
    .module-editor__paste-label { display: block; font-weight: 600; margin-bottom: 6px; }
    .module-editor__paste-box { width: 100%; resize: vertical; }
    .module-editor__paste-actions { display: flex; gap: 10px; align-items: center; margin-top: 8px; }
    .module-editor__table td { vertical-align: top; }
    .module-editor__input { width: 100%; min-height: 36px; padding: 8px 10px; border-radius: 10px; border: 1px solid rgba(0,0,0,.15); background: #fff; }
    .module-editor__row-actions { display: flex; gap: 10px; justify-content: flex-end; align-items: center; }
    .module-editor__drag { display: inline-flex; align-items: center; gap: 8px; cursor: grab; user-select: none; padding: 6px 10px; border-radius: 10px; border: 1px dashed rgba(0,0,0,.18); background: rgba(255,255,255,.8); }
    .module-editor__drag:active { cursor: grabbing; }
    .module-editor__drag-handle { font-weight: 700; letter-spacing: 1px; opacity: .75; }
    tr.module-editor__dragging { opacity: .6; }
    tr.module-editor__drag-over { outline: 2px solid rgba(0,0,0,.18); outline-offset: -2px; }
    .module-editor__row-state { font-size: 12px; margin-top: 4px; }
    .module-editor__row-state--bad { color: #b00020; }
    .module-editor__row-state--dup { color: #8a5a00; }
    .module-editor__summary { margin-top: 10px; }
    .module-editor__raw { margin-top: 10px; }
    .js-module-editor .module-editor__raw { position: absolute; left: -99999px; width: 1px; height: 1px; opacity: 0; }
</style>

<script>
    (function () {
        function normalizeCode(code) {
            return (code || "").trim().toUpperCase().replace(/\s+/g, "");
        }

        function parseLine(line) {
            var raw = (line || "").trim();
            if (!raw) return null;
            var parts = raw.split("|");
            var code = normalizeCode(parts[0] || "");
            var name = (parts.slice(1).join("|") || "").trim();
            return { code: code, name: name };
        }

        function parseTextToRows(text) {
            var lines = (text || "").split(/\r?\n/);
            var out = [];
            lines.forEach(function (ln) {
                var p = parseLine(ln);
                if (p && (p.code || p.name)) out.push(p);
            });
            return out;
        }

        function serializeRows(rows) {
            return rows
                .map(function (r) {
                    var c = normalizeCode(r.code);
                    var n = (r.name || "").trim();
                    if (!c) return "";
                    return n ? (c + " | " + n) : c;
                })
                .filter(Boolean)
                .join("\n");
        }

        function isValidCode(code) {
            return /^[A-Z]{2,6}\d{3,5}$/.test(code);
        }

        function buildRowDom(row) {
            var tr = document.createElement("tr");
            tr.setAttribute("draggable", "true");
            tr.innerHTML = ""
                + "<td>"
                + "  <input type='text' class='module-editor__input' data-code placeholder='e.g. EBU6304' />"
                + "  <div class='module-editor__row-state' data-state></div>"
                + "</td>"
                + "<td><input type='text' class='module-editor__input' data-name placeholder='e.g. Software Engineering (optional)' /></td>"
                + "<td>"
                + "  <div class='module-editor__row-actions'>"
                + "    <span class='module-editor__drag' title='Drag to reorder' aria-label='Drag to reorder'>"
                + "      <span class='module-editor__drag-handle' aria-hidden='true'>⋮⋮</span>"
                + "      <span>Drag</span>"
                + "    </span>"
                + "    <button type='button' class='btn btn-danger' data-remove>Remove</button>"
                + "  </div>"
                + "</td>";

            tr.querySelector("[data-code]").value = row && row.code ? row.code : "";
            tr.querySelector("[data-name]").value = row && row.name ? row.name : "";
            return tr;
        }

        function initOne(editor) {
            // pick the textarea that is the immediate sibling after the editor block
            var raw = editor.parentElement.querySelector("textarea.module-editor__raw[name='assignedModulesText']");
            if (!raw) return;

            document.documentElement.classList.add("js-module-editor");

            var tbody = editor.querySelector("[data-rows]");
            var addBtn = editor.querySelector("[data-add-row]");
            var sortBtn = editor.querySelector("[data-sort]");
            var clearBtn = editor.querySelector("[data-clear]");
            var pasteBox = editor.querySelector("[data-paste-box]");
            var importBtn = editor.querySelector("[data-import]");
            var importStatus = editor.querySelector("[data-import-status]");
            var validCount = editor.querySelector("[data-valid-count]");
            var dupCount = editor.querySelector("[data-dup-count]");
            var invalidCount = editor.querySelector("[data-invalid-count]");

            function getCurrentRows() {
                return Array.prototype.slice.call(tbody.querySelectorAll("tr")).map(function (tr) {
                    return {
                        code: normalizeCode(tr.querySelector("[data-code]").value),
                        name: (tr.querySelector("[data-name]").value || "").trim()
                    };
                });
            }

            function syncRaw() {
                raw.value = serializeRows(getCurrentRows());
            }

            function recomputeState() {
                var seen = new Map();
                var valid = 0, dup = 0, invalid = 0;

                Array.prototype.slice.call(tbody.querySelectorAll("tr")).forEach(function (tr) {
                    var codeEl = tr.querySelector("[data-code]");
                    var stateEl = tr.querySelector("[data-state]");
                    var code = normalizeCode(codeEl.value);

                    stateEl.className = "module-editor__row-state";
                    stateEl.textContent = "";

                    if (!code) {
                        invalid += 1;
                        stateEl.classList.add("module-editor__row-state--bad");
                        stateEl.textContent = "Module code is required.";
                        return;
                    }

                    if (!isValidCode(code)) {
                        invalid += 1;
                        stateEl.classList.add("module-editor__row-state--bad");
                        stateEl.textContent = "Format looks unusual. Expected like EBU6304.";
                        return;
                    }

                    if (seen.has(code)) {
                        dup += 1;
                        stateEl.classList.add("module-editor__row-state--dup");
                        stateEl.textContent = "Duplicate module code.";
                        return;
                    }

                    seen.set(code, true);
                    valid += 1;
                });

                if (validCount) validCount.textContent = String(valid);
                if (dupCount) dupCount.textContent = String(dup);
                if (invalidCount) invalidCount.textContent = String(invalid);
            }

            function wireRow(tr) {
                tr.addEventListener("input", function () {
                    syncRaw();
                    recomputeState();
                });

                tr.querySelector("[data-remove]").addEventListener("click", function () {
                    tr.remove();
                    syncRaw();
                    recomputeState();
                });

                tr.addEventListener("dragstart", function (e) {
                    tr.classList.add("module-editor__dragging");
                    e.dataTransfer.effectAllowed = "move";
                    try { e.dataTransfer.setData("text/plain", "drag"); } catch (ignored) {}
                });

                tr.addEventListener("dragend", function () {
                    tr.classList.remove("module-editor__dragging");
                    Array.prototype.slice.call(tbody.querySelectorAll("tr")).forEach(function (row) {
                        row.classList.remove("module-editor__drag-over");
                    });
                    syncRaw();
                    recomputeState();
                });

                tr.addEventListener("dragover", function (e) {
                    e.preventDefault();
                    e.dataTransfer.dropEffect = "move";
                    tr.classList.add("module-editor__drag-over");
                });

                tr.addEventListener("dragleave", function () {
                    tr.classList.remove("module-editor__drag-over");
                });

                tr.addEventListener("drop", function (e) {
                    e.preventDefault();
                    tr.classList.remove("module-editor__drag-over");
                    var dragging = tbody.querySelector("tr.module-editor__dragging");
                    if (!dragging || dragging === tr) return;

                    var rect = tr.getBoundingClientRect();
                    var before = e.clientY < (rect.top + rect.height / 2);
                    if (before) {
                        tbody.insertBefore(dragging, tr);
                    } else {
                        tbody.insertBefore(dragging, tr.nextSibling);
                    }
                    syncRaw();
                    recomputeState();
                });
            }

            function addRow(row) {
                var tr = buildRowDom(row || { code: "", name: "" });
                tbody.appendChild(tr);
                wireRow(tr);
            }

            function importFromText(text) {
                var rows = parseTextToRows(text);
                var added = 0;
                rows.forEach(function (r) {
                    addRow({ code: r.code, name: r.name });
                    added += 1;
                });
                syncRaw();
                recomputeState();
                return added;
            }

            // seed from existing raw textarea
            var seeded = importFromText(raw.value || "");
            if (seeded === 0) {
                addRow({ code: "", name: "" });
                syncRaw();
                recomputeState();
            }

            if (addBtn) addBtn.addEventListener("click", function () {
                addRow({ code: "", name: "" });
                syncRaw();
                recomputeState();
            });

            if (sortBtn) sortBtn.addEventListener("click", function () {
                var rows = getCurrentRows().filter(function (r) { return r.code || r.name; });
                rows.sort(function (a, b) {
                    return normalizeCode(a.code).localeCompare(normalizeCode(b.code));
                });
                tbody.innerHTML = "";
                rows.forEach(function (r) { addRow(r); });
                if (!rows.length) addRow({ code: "", name: "" });
                syncRaw();
                recomputeState();
            });

            if (clearBtn) clearBtn.addEventListener("click", function () {
                tbody.innerHTML = "";
                addRow({ code: "", name: "" });
                if (pasteBox) pasteBox.value = "";
                if (importStatus) importStatus.textContent = "";
                syncRaw();
                recomputeState();
            });

            if (importBtn) importBtn.addEventListener("click", function () {
                var txt = pasteBox ? pasteBox.value : "";
                if (!txt.trim()) {
                    if (importStatus) importStatus.textContent = "Nothing to import.";
                    return;
                }
                var added = importFromText(txt);
                if (importStatus) importStatus.textContent = "Imported " + added + " line(s).";
                if (pasteBox) pasteBox.value = "";
            });

            var form = raw.closest("form");
            if (form) {
                form.addEventListener("submit", function () {
                    syncRaw();
                    recomputeState();
                });
            }

            // initial state (after seeding)
            syncRaw();
            recomputeState();
        }

        document.querySelectorAll("[data-module-editor]").forEach(initOne);
    })();
</script>
</body>
</html>
