<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.service.AdminService" %>
<%
    AdminService.UserDirectoryReport directory = (AdminService.UserDirectoryReport) request.getAttribute("directory");
    if (directory == null) {
        directory = new AdminService.UserDirectoryReport(
                0, 0, 0, 0, "ALL", "", java.util.Collections.emptyList()
        );
    }
    request.setAttribute("adminNavActive", "users");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Admin Users - TA Recruitment</title>
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
                <div class="icon-dot">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot active">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Directory</p>
                <h1>User Directory</h1>
                <p class="ta-page-lead">Review all TA, MO, and admin accounts in one place, then open complete detail pages for read-only traceability.</p>
            </header>
            <div class="context-card">
                <strong>Admin view</strong>
                <p>This page is read-only. Use it to cross-check roles, contact information, workload signals, and role-specific activity before opening the full TA or MO detail view.</p>
            </div>

            <% if ("invalid_ta".equals(request.getParameter("error")) || "invalid_mo".equals(request.getParameter("error"))) { %>
            <div class="ta-page-flashes">
            <% if ("invalid_ta".equals(request.getParameter("error"))) { %><p class="error">The requested TA record was not found.</p><% } %>
            <% if ("invalid_mo".equals(request.getParameter("error"))) { %><p class="error">The requested MO record was not found.</p><% } %>
            </div>
            <% } %>

            <section class="detail-card">
                <h3>Search and filter</h3>
                <form action="${pageContext.request.contextPath}/admin/users" method="get" class="form form--admin-users-filter">
                    <div class="admin-filter-grid">
                        <div>
                            <label for="role-filter">Role</label>
                            <select id="role-filter" name="role">
                                <option value="ALL" <%= "ALL".equals(directory.getRoleFilter()) ? "selected" : "" %>>All roles</option>
                                <option value="TA" <%= "TA".equals(directory.getRoleFilter()) ? "selected" : "" %>>TA</option>
                                <option value="MO" <%= "MO".equals(directory.getRoleFilter()) ? "selected" : "" %>>MO</option>
                                <option value="ADMIN" <%= "ADMIN".equals(directory.getRoleFilter()) ? "selected" : "" %>>Admin</option>
                            </select>
                        </div>
                        <div>
                            <label for="query-filter">Search</label>
                            <input id="query-filter" type="text" name="q" value="<%= escHtml(directory.getQuery()) %>" placeholder="Username, real name, email, student ID, programme">
                        </div>
                        <div class="admin-filter-actions">
                            <button type="submit" class="btn btn-primary">Apply filters</button>
                            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-secondary">Reset</a>
                        </div>
                    </div>
                </form>
            </section>

            <div class="stats-row admin-stat-grid">
                <div class="stat-card">
                    <div class="stat-icon">U</div>
                    <div>
                        <div class="stat-title">Visible users</div>
                        <div class="stat-value"><%= directory.getVisibleCount() %></div>
                        <div class="stat-meta">Total in system <%= directory.getTotalUsers() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">T</div>
                    <div>
                        <div class="stat-title">TA accounts</div>
                        <div class="stat-value"><%= directory.getTotalTas() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">M</div>
                    <div>
                        <div class="stat-title">MO accounts</div>
                        <div class="stat-value"><%= directory.getTotalMos() %></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">A</div>
                    <div>
                        <div class="stat-title">Admin accounts</div>
                        <div class="stat-value"><%= directory.getTotalAdmins() %></div>
                    </div>
                </div>
            </div>

            <section class="detail-card">
                <h3>All users</h3>
                <% if (directory.getRows().isEmpty()) { %>
                <p class="section-empty section-empty--card">No users matched the current filter.</p>
                <% } else { %>
                <p class="table-scroll-wrap-hint">Tip: swipe horizontally on narrow screens to view all columns.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table admin-table--users">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Role</th>
                            <th>Username</th>
                            <th>Email</th>
                            <th>Student ID</th>
                            <th>Activity summary</th>
                            <th>Action</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AdminService.UserListRow row : directory.getRows()) {
                               String role = row.getUser() != null ? row.getUser().getRole() : "";
                        %>
                        <tr>
                            <td>
                                <strong><%= escHtml(row.getDisplayName()) %></strong>
                                <div class="admin-row-subtext">User ID: <code><%= escHtml(row.getUser() != null ? row.getUser().getId() : "-") %></code></div>
                            </td>
                            <td>
                                <span class="admin-role-pill admin-role-pill-<%= escHtml(role != null ? role.toLowerCase() : "other") %>"><%= escHtml(role != null && !role.isEmpty() ? role : "-") %></span>
                            </td>
                            <td><%= escHtml(row.getUser() != null ? row.getUser().getUsername() : "-") %></td>
                            <td><%= escHtml(row.getEmail() != null && !row.getEmail().isEmpty() ? row.getEmail() : "-") %></td>
                            <td><%= escHtml(row.getStudentId() != null && !row.getStudentId().isEmpty() ? row.getStudentId() : "-") %></td>
                            <td>
                                <% if ("TA".equalsIgnoreCase(role)) { %>
                                <div class="admin-summary-stack">
                                    <span>Selected <strong><%= row.getSelectedCount() %></strong> | Pending <strong><%= row.getPendingCount() %></strong> | Interview <strong><%= row.getInterviewCount() %></strong></span>
                                    <span>Waitlist <strong><%= row.getWaitlistCount() %></strong> | Auto-closed <strong><%= row.getAutoClosedCount() %></strong></span>
                                    <span>Saved jobs <strong><%= row.getSavedJobsCount() %></strong> | Unread notices <strong><%= row.getUnreadNotificationCount() %></strong></span>
                                </div>
                                <% } else if ("MO".equalsIgnoreCase(role)) { %>
                                <div class="admin-summary-stack">
                                    <span>Posted jobs <strong><%= row.getPostedJobsCount() %></strong> | Active <strong><%= row.getActiveJobsCount() %></strong></span>
                                    <span>Applications received <strong><%= row.getTotalApplicationsReceived() %></strong></span>
                                    <span>Selected applicants <strong><%= row.getTotalSelectedForMo() %></strong></span>
                                </div>
                                <% } else { %>
                                <span class="muted-inline">System administration account.</span>
                                <% } %>
                            </td>
                            <td>
                                <% if ("TA".equalsIgnoreCase(role)) { %>
                                <a href="${pageContext.request.contextPath}/admin/ta-detail?userId=<%= row.getUser().getId() %>" class="admin-inline-link">View TA detail</a>
                                <% } else if ("MO".equalsIgnoreCase(role)) { %>
                                <a href="${pageContext.request.contextPath}/admin/mo-detail?userId=<%= row.getUser().getId() %>" class="admin-inline-link">View MO detail</a>
                                <% } else { %>
                                <span class="muted-inline">No extra detail page</span>
                                <% } %>
                            </td>
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
                <div class="widget-title">Directory Tips</div>
                <p class="widget-line">Search supports username, real name, email, student ID, and programme.</p>
                <p class="widget-line">Open TA detail for profile, applications, saved jobs, and notification history.</p>
                <p class="widget-line">Open MO detail for posting history, capacity, and applicant handling.</p>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Quick Links</div>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/dashboard">Summary dashboard</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/workload">TA workload</a></p>
                <p class="widget-line"><a href="${pageContext.request.contextPath}/admin/monitoring">Monitoring issues</a></p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
