<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="bupt.ta.servlet.MOInterviewCalendarServlet.CalendarRow" %>
<%
    @SuppressWarnings("unchecked")
    TreeMap<LocalDate, List<CalendarRow>> calendarByDay =
            (TreeMap<LocalDate, List<CalendarRow>>) request.getAttribute("calendarByDay");
    if (calendarByDay == null) {
        calendarByDay = new TreeMap<>();
    }
    @SuppressWarnings("unchecked")
    List<CalendarRow> expiredRows = (List<CalendarRow>) request.getAttribute("calendarExpiredRows");
    if (expiredRows == null) {
        expiredRows = java.util.Collections.emptyList();
    }
    @SuppressWarnings("unchecked")
    List<CalendarRow> unscheduled = (List<CalendarRow>) request.getAttribute("calendarUnscheduled");
    if (unscheduled == null) {
        unscheduled = java.util.Collections.emptyList();
    }
    Integer nUp = (Integer) request.getAttribute("calendarTotalUpcoming");
    Integer nEx = (Integer) request.getAttribute("calendarTotalExpired");
    Integer nUnsched = (Integer) request.getAttribute("calendarTotalUnscheduled");
    int totalUpcoming = nUp != null ? nUp : 0;
    int totalExpired = nEx != null ? nEx : 0;
    int totalUnsched = nUnsched != null ? nUnsched : 0;
    String ctx = request.getContextPath();
    DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd", java.util.Locale.ENGLISH);
    DateTimeFormatter shortDay = DateTimeFormatter.ISO_LOCAL_DATE;
    request.setAttribute("moNavActive", "calendar");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Interview calendar - MO</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="nav top-nav">
        <span class="brand">BUPT Teaching Assistant Recruitment System</span>
        <div class="user user-inline-actions"><span><%= session.getAttribute("realName") %> |</span><form action="<%= ctx %>/logout" method="post" class="inline-form logout-form"><%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %><button type="submit" class="logout-button">Logout</button></form></div>
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
        <main class="main-panel mo-main mo-page mo-page--mo-calendar">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Schedule</p>
                <h1>Interview calendar</h1>
                <p class="ta-page-lead">All <strong>Interview</strong> and <strong>Waitlist</strong> applications across your postings, grouped by booked slot time when available, or by the legacy per-applicant interview notice field otherwise. Dates <strong>before today</strong> are listed as expired; today and future appear as your to-do schedule.</p>
            </header>

            <div class="stats-row mo-cal-stats mo-cal-stats--three">
                <div class="stat-card stat-card--todo">
                    <div class="stat-icon">D</div>
                    <div>
                        <div class="stat-title">To-do (today &amp; future)</div>
                        <div class="stat-value"><%= totalUpcoming %></div>
                        <div class="stat-meta">Parseable date, not yet passed</div>
                    </div>
                </div>
                <div class="stat-card stat-card--expired">
                    <div class="stat-icon">E</div>
                    <div>
                        <div class="stat-title">Past / expired</div>
                        <div class="stat-value"><%= totalExpired %></div>
                        <div class="stat-meta">Interview day before today</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">N</div>
                    <div>
                        <div class="stat-title">Needs date / text</div>
                        <div class="stat-value"><%= totalUnsched %></div>
                        <div class="stat-meta">No recognised date in the time field</div>
                    </div>
                </div>
            </div>

            <% if (calendarByDay.isEmpty() && expiredRows.isEmpty() && unscheduled.isEmpty()) { %>
            <p class="section-empty section-empty--card">No interview or waitlist applicants yet. Use <strong>My Jobs</strong> &rarr; Interview tab to move candidates and send notices.</p>
            <% } else { %>
            <div class="mo-cal-day-list">
                <% for (Map.Entry<LocalDate, List<CalendarRow>> e : calendarByDay.entrySet()) {
                    LocalDate d = e.getKey();
                    List<CalendarRow> rows = e.getValue();
                %>
                <section class="detail-card mo-cal-day-card mo-cal-day-card--upcoming">
                    <h2 class="mo-cal-day-title"><%= escHtml(d.format(dayFmt)) %></h2>
                    <div class="table-scroll-wrap">
                        <table class="admin-table mo-cal-table">
                            <thead>
                            <tr>
                                <th>Time</th>
                                <th>Applicant</th>
                                <th>Job</th>
                                <th>Module</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            <% for (CalendarRow r : rows) { %>
                            <tr class="mo-cal-row mo-cal-row--upcoming">
                                <td><%= escHtml(r.getTimeDisplay()) %></td>
                                <td><%= escHtml(r.getApplicantName()) %></td>
                                <td><%= escHtml(r.getJobTitle()) %></td>
                                <td><%= escHtml(r.getModuleCode()) %></td>
                                <td class="pre-wrap"><%= escHtml(r.getLocation().isEmpty() ? "—" : r.getLocation()) %></td>
                                <td><span class="status-pill status-pill-interview"><%= escHtml(r.getStatus()) %></span></td>
                                <td><a class="btn btn-secondary btn-sm" href="<%= r.manageHref(ctx) %>">Manage posting</a></td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </section>
                <% } %>
            </div>

            <% if (!expiredRows.isEmpty()) { %>
            <section class="detail-card mo-cal-expired">
                <h2 class="mo-cal-day-title mo-cal-day-title--expired">Past interview dates (expired)</h2>
                <p class="muted-inline">These rows had a recognisable date that is <strong>before today</strong>. They are kept for history; follow up in the posting if outcomes still need recording.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table mo-cal-table">
                        <thead>
                        <tr>
                            <th>Date</th>
                            <th>Time</th>
                            <th>Applicant</th>
                            <th>Job</th>
                            <th>Module</th>
                            <th>Location</th>
                            <th>Status</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (CalendarRow r : expiredRows) {
                               LocalDate rd = r.getDate();
                               String dateCell = rd != null ? escHtml(rd.format(shortDay)) : "—";
                        %>
                        <tr class="mo-cal-row mo-cal-row--expired">
                            <td><%= dateCell %></td>
                            <td><%= escHtml(r.getTimeDisplay()) %></td>
                            <td><%= escHtml(r.getApplicantName()) %></td>
                            <td><%= escHtml(r.getJobTitle()) %></td>
                            <td><%= escHtml(r.getModuleCode()) %></td>
                            <td class="pre-wrap"><%= escHtml(r.getLocation().isEmpty() ? "—" : r.getLocation()) %></td>
                            <td>
                                <span class="status-pill status-pill-interview"><%= escHtml(r.getStatus()) %></span>
                                <span class="mo-cal-expired-tag" title="Interview date is before today">· Expired</span>
                            </td>
                            <td><a class="btn btn-secondary btn-sm" href="<%= r.manageHref(ctx) %>">Manage posting</a></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </section>
            <% } %>

            <% if (!unscheduled.isEmpty()) { %>
            <section class="detail-card mo-cal-unscheduled">
                <h2 class="mo-cal-day-title">Awaiting a parseable date</h2>
                <p class="muted-inline">These applicants are in Interview or Waitlist but the interview time field is empty or not in a recognised format (use e.g. <code>yyyy-MM-dd</code> or <code>yyyy-M-d</code> at the start). You can still open the posting and edit notices.</p>
                <div class="table-scroll-wrap">
                    <table class="admin-table mo-cal-table">
                        <thead>
                        <tr>
                            <th>Applicant</th>
                            <th>Job</th>
                            <th>Module</th>
                            <th>Time (raw)</th>
                            <th>Location</th>
                            <th>Status</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (CalendarRow r : unscheduled) { %>
                        <tr class="mo-cal-row mo-cal-row--unscheduled">
                            <td><%= escHtml(r.getApplicantName()) %></td>
                            <td><%= escHtml(r.getJobTitle()) %></td>
                            <td><%= escHtml(r.getModuleCode()) %></td>
                            <td class="pre-wrap"><%= escHtml(r.getInterviewTimeRaw() == null || r.getInterviewTimeRaw().isEmpty() ? "—" : r.getInterviewTimeRaw()) %></td>
                            <td class="pre-wrap"><%= escHtml(r.getLocation().isEmpty() ? "—" : r.getLocation()) %></td>
                            <td><span class="status-pill status-pill-interview"><%= escHtml(r.getStatus()) %></span></td>
                            <td><a class="btn btn-secondary btn-sm" href="<%= r.manageHref(ctx) %>">Manage posting</a></td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </section>
            <% } %>
            <% } %>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Calendar tip</div>
                <p class="widget-line">Use a clear leading date (<code>2026-04-09</code> or <code>2026-4-9</code>) so rows land on the calendar; past dates move to the expired section automatically.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
