<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="bupt.ta.model.SiteNotification" %>
<%@ page import="bupt.ta.service.StudentNotificationService" %>
<%!
    static String boxQuery(String f) {
        if (f == null || "all".equals(f)) {
            return "";
        }
        return "&box=" + java.net.URLEncoder.encode(f, java.nio.charset.StandardCharsets.UTF_8);
    }

    static String kindLabel(String kind) {
        if (kind == null) return "Update";
        switch (kind) {
            case StudentNotificationService.KIND_APPLICATION_SUBMITTED: return "Submitted";
            case StudentNotificationService.KIND_STATUS_INTERVIEW: return "Interview";
            case StudentNotificationService.KIND_STATUS_WAITLIST: return "Waitlist";
            case StudentNotificationService.KIND_STATUS_SELECTED: return "Selected";
            case StudentNotificationService.KIND_STATUS_REJECTED: return "REJECTED";
            case StudentNotificationService.KIND_INTERVIEW_DETAILS: return "Interview info";
            case StudentNotificationService.KIND_AUTO_CLOSED: return "Auto-closed";
            case StudentNotificationService.KIND_WITHDRAWN: return "Withdrawn";
            case StudentNotificationService.KIND_AUTO_PROMOTED: return "Promoted";
            default: return "Update";
        }
    }

    static String kindClass(String kind) {
        if (kind == null) return "ta-msg-kind--default";
        if (StudentNotificationService.KIND_STATUS_REJECTED.equals(kind)
                || StudentNotificationService.KIND_WITHDRAWN.equals(kind)) {
            return "ta-msg-kind--danger";
        }
        if (StudentNotificationService.KIND_STATUS_SELECTED.equals(kind) || StudentNotificationService.KIND_AUTO_PROMOTED.equals(kind)) {
            return "ta-msg-kind--success";
        }
        if (StudentNotificationService.KIND_AUTO_CLOSED.equals(kind)) {
            return "ta-msg-kind--muted";
        }
        if (StudentNotificationService.KIND_INTERVIEW_DETAILS.equals(kind) || StudentNotificationService.KIND_STATUS_INTERVIEW.equals(kind)) {
            return "ta-msg-kind--info";
        }
        if (StudentNotificationService.KIND_STATUS_WAITLIST.equals(kind)) {
            return "ta-msg-kind--waitlist";
        }
        return "ta-msg-kind--default";
    }

    static String formatCreatedAt(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String t = raw.trim();
        int tPos = t.indexOf('T');
        if (tPos < 0) {
            return t.length() >= 16 ? t.substring(0, 16) : t;
        }
        String datePart = t.substring(0, tPos);
        String timePart = t.substring(tPos + 1);
        int dot = timePart.indexOf('.');
        if (dot >= 0) timePart = timePart.substring(0, dot);
        int plus = timePart.indexOf('+');
        if (plus >= 0) timePart = timePart.substring(0, plus);
        int z = timePart.toUpperCase().indexOf('Z');
        if (z >= 0) timePart = timePart.substring(0, z);
        String[] segs = timePart.split(":");
        if (segs.length >= 2) {
            String h = segs[0];
            String m = segs[1];
            if (h.length() == 1) h = "0" + h;
            if (m.length() >= 2) m = m.substring(0, 2);
            else if (m.length() == 1) m = "0" + m;
            return datePart + " " + h + ":" + m;
        }
        return datePart + " " + timePart;
    }

    static String summaryPreview(String body, int maxLen) {
        if (body == null) return "";
        String s = body.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen).trim() + "\u2026";
    }
%>
<%
    request.setAttribute("taNavActive", "messages");
    SiteNotification detail = (SiteNotification) request.getAttribute("detailNotification");
    String ctx = request.getContextPath();
    String listFilter = (String) request.getAttribute("messagesFilter");
    if (listFilter == null) {
        listFilter = "all";
    }
    boolean listMode = detail == null;
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title><%= detail != null ? "Message detail" : "Messages" %> - TA Recruitment</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="<%= listMode ? "ta-messages-list-body" : "" %>">
<div class="container<%= listMode ? " container--ta-messages-list" : "" %>">
    <div class="nav top-nav">
        <span class="brand">BUPT Teaching Assistant Recruitment System</span>
        <div class="user user-inline-actions"><span><%= session.getAttribute("realName") %> |</span><form action="<%= ctx %>/logout" method="post" class="inline-form logout-form"><%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %><button type="submit" class="logout-button">Logout</button></form></div>
    </div>
    <div class="page-layout">
        <div class="left-nav-wrap">
            <div class="icon-rail">
                <div class="icon-dot">H</div>
                <div class="icon-dot">F</div>
                <div class="icon-dot active">M</div>
                <div class="icon-dot">P</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel ta-main ta-page ta-page--messages ta-messages-main<%= listMode ? " ta-messages-main--list" : "" %>">
<% if (detail != null) {
    Integer backPageObj = (Integer) request.getAttribute("listPageForBack");
    int backPage = backPageObj != null ? backPageObj : 1;
    String backHref = ctx + "/ta/messages?page=" + backPage + boxQuery(listFilter);
%>
            <header class="ta-page-header ta-message-detail-page-header">
                <p class="breadcrumb-line ta-job-detail-breadcrumb"><a href="<%= backHref %>">&larr; Back to messages</a></p>
                <p class="ta-page-kicker">Notification</p>
            </header>
            <article class="ta-message-detail-card <%= detail.isRead() ? "ta-message-detail-card--read" : "ta-message-detail-card--unread" %>">
                <div class="ta-message-detail-meta">
                    <span class="ta-msg-kind <%= kindClass(detail.getKind()) %>"><%= escHtml(kindLabel(detail.getKind())) %></span>
                    <div class="ta-message-detail-meta-right">
                        <% if (!detail.isRead()) { %>
                        <span class="ta-msg-unread-pill">Unread</span>
                        <% } else { %>
                        <span class="ta-msg-read-pill">Read</span>
                        <% } %>
                        <time class="ta-message-time"><%= escHtml(formatCreatedAt(detail.getCreatedAt())) %></time>
                    </div>
                </div>
                <h1 class="ta-message-detail-title"><%= escHtml(detail.getTitle()) %></h1>
                <div class="ta-message-detail-body"><%= escHtml(detail.getBody()) %></div>
                <% if (!detail.isRead()) { %>
                <div class="ta-message-detail-actions">
                    <form action="<%= ctx %>/ta/messages" method="post" class="inline-form">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="action" value="markRead">
                        <input type="hidden" name="notificationId" value="<%= detail.getId() %>">
                        <input type="hidden" name="fromPage" value="<%= backPage %>">
                        <input type="hidden" name="box" value="<%= escHtml(listFilter) %>">
                        <input type="hidden" name="stayOnDetail" value="1">
                        <button type="submit" class="btn btn-primary">Mark as read</button>
                    </form>
                </div>
                <% } %>
                <p class="ta-message-detail-foot muted-inline">Application status: <a href="<%= ctx %>/ta/applications">My Applications</a>.
                    <% if (detail.getApplicationId() != null && !detail.getApplicationId().isEmpty()
                            && StudentNotificationService.KIND_INTERVIEW_DETAILS.equals(detail.getKind())) { %>
                    <span class="muted-inline"> | </span><a href="<%= ctx %>/ta/interview-calendar?applicationId=<%= escHtml(detail.getApplicationId()) %>">Download interview .ics</a>
                    <% } %>
                </p>
            </article>
<% } else {
    List<SiteNotification> notifications = (List<SiteNotification>) request.getAttribute("notifications");
    if (notifications == null) notifications = java.util.Collections.emptyList();
    Integer totalObj = (Integer) request.getAttribute("notificationsTotal");
    int total = totalObj != null ? totalObj : 0;
    Integer allTotalObj = (Integer) request.getAttribute("notificationsAllTotal");
    int allTotal = allTotalObj != null ? allTotalObj : 0;
    Integer unreadTotalObj = (Integer) request.getAttribute("notificationsUnreadTotal");
    int unreadTotal = unreadTotalObj != null ? unreadTotalObj : 0;
    Integer readTotalObj = (Integer) request.getAttribute("notificationsReadTotal");
    int readTotal = readTotalObj != null ? readTotalObj : 0;
    Integer pageObj = (Integer) request.getAttribute("messagesPage");
    int curPage = pageObj != null ? pageObj : 1;
    Integer totalPagesObj = (Integer) request.getAttribute("messagesTotalPages");
    int totalPages = totalPagesObj != null ? totalPagesObj : 1;
%>
            <header class="ta-page-header ta-messages-header">
                <div class="ta-messages-header__row">
                    <div class="ta-messages-header__intro">
                        <p class="ta-page-kicker">Inbox</p>
                        <h1>Messages</h1>
                        <p class="ta-page-lead">Filter by read state, open a row for the full message, or mark items in bulk. Ten per page; scroll inside the list.</p>
                    </div>
                    <% if (allTotal > 0 && unreadTotal > 0) { %>
                    <form action="<%= ctx %>/ta/messages" method="post" class="ta-messages-markall">
                        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                        <input type="hidden" name="action" value="markAllRead">
                        <input type="hidden" name="page" value="<%= curPage %>">
                        <input type="hidden" name="box" value="<%= escHtml(listFilter) %>">
                        <button type="submit" class="btn btn-secondary">Mark all read</button>
                    </form>
                    <% } %>
                </div>
            </header>

            <nav class="ta-msg-filter-bar" aria-label="Filter messages">
                <a class="ta-msg-filter-tab <%= "all".equals(listFilter) ? "ta-msg-filter-tab--active" : "" %>" href="<%= ctx %>/ta/messages?page=1">All <span class="ta-msg-filter-count">(<%= allTotal %>)</span></a>
                <a class="ta-msg-filter-tab <%= "unread".equals(listFilter) ? "ta-msg-filter-tab--active" : "" %>" href="<%= ctx %>/ta/messages?page=1&box=unread">Unread <span class="ta-msg-filter-count">(<%= unreadTotal %>)</span></a>
                <a class="ta-msg-filter-tab <%= "read".equals(listFilter) ? "ta-msg-filter-tab--active" : "" %>" href="<%= ctx %>/ta/messages?page=1&box=read">Read <span class="ta-msg-filter-count">(<%= readTotal %>)</span></a>
            </nav>

            <% if (allTotal == 0) { %>
            <div class="ta-panel ta-panel--tip ta-messages-empty-panel ta-messages-empty">
                <p class="ta-messages-empty-title">No messages yet</p>
                <p class="ta-panel__body muted-inline">When you apply or when a module organiser updates your application, a message will appear here.</p>
                <p class="ta-messages-empty-actions"><a href="<%= ctx %>/ta/jobs" class="btn btn-primary">Browse jobs</a></p>
            </div>
            <% } else if (total == 0) { %>
            <div class="ta-panel ta-panel--tip ta-messages-empty-panel ta-messages-empty">
                <p class="ta-messages-empty-title">No messages in this view</p>
                <p class="ta-panel__body muted-inline"><% if ("unread".equals(listFilter)) { %>There are no unread messages.<% } else { %>There are no read messages.<% } %></p>
                <p class="ta-messages-empty-actions"><a href="<%= ctx %>/ta/messages?page=1" class="btn btn-primary">Show all</a></p>
            </div>
            <% } else { %>
            <div class="ta-messages-list-layout">
            <form action="<%= ctx %>/ta/messages" method="post" class="ta-messages-batch-form" id="ta-messages-batch-form">
                <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                <input type="hidden" name="page" value="<%= curPage %>">
                <input type="hidden" name="box" value="<%= escHtml(listFilter) %>">
                <div class="ta-messages-toolbar">
                    <div class="ta-messages-toolbar-btns">
                        <button type="submit" name="action" value="markSelectedRead" class="btn btn-primary">Mark selected read</button>
                        <button type="submit" name="action" value="markSelectedUnread" class="btn btn-secondary">Mark selected unread</button>
                        <% if (unreadTotal > 0) { %>
                        <button type="submit" name="action" value="markAllRead" class="btn btn-secondary">Mark all read</button>
                        <% } %>
                    </div>
                    <span class="muted-inline ta-messages-toolbar-hint">Select rows, then choose an action.</span>
                </div>
                <div class="ta-messages-list-scroll">
            <ul class="ta-message-list ta-message-list--compact">
                <% for (SiteNotification n : notifications) {
                    String detailHref = ctx + "/ta/messages?id=" + java.net.URLEncoder.encode(n.getId(), java.nio.charset.StandardCharsets.UTF_8)
                            + "&fromPage=" + curPage + boxQuery(listFilter);
                %>
                <li class="ta-message-row <%= n.isRead() ? "ta-message-row--read" : "ta-message-row--unread" %>">
                    <label class="ta-message-row-check" title="Select for batch actions">
                        <input type="checkbox" name="notificationId" value="<%= n.getId() %>">
                    </label>
                    <a class="ta-message-row-main" href="<%= detailHref %>">
                        <div class="ta-message-row-top">
                            <span class="ta-msg-kind <%= kindClass(n.getKind()) %>"><%= escHtml(kindLabel(n.getKind())) %></span>
                            <div class="ta-message-row-meta">
                                <% if (!n.isRead()) { %>
                                <span class="ta-msg-unread-dot" title="Unread"></span>
                                <% } %>
                                <time class="ta-message-time"><%= escHtml(formatCreatedAt(n.getCreatedAt())) %></time>
                            </div>
                        </div>
                        <span class="ta-message-row-title"><%= escHtml(n.getTitle()) %></span>
                        <span class="ta-message-row-summary"><%= escHtml(summaryPreview(n.getBody(), 140)) %></span>
                    </a>
                </li>
                <% } %>
            </ul>
                </div>
            </form>

            <nav class="ta-msg-pagination ta-msg-pagination--list" aria-label="Message pages">
                <% if (curPage > 1) { %>
                <a class="btn btn-secondary btn-sm" href="<%= ctx %>/ta/messages?page=<%= curPage - 1 %><%= boxQuery(listFilter) %>">Previous</a>
                <% } else { %>
                <span class="btn btn-secondary btn-sm ta-msg-page-disabled" aria-disabled="true">Previous</span>
                <% } %>
                <span class="ta-msg-page-status">Page <%= curPage %> of <%= totalPages %> (10 per page)</span>
                <% if (curPage < totalPages) { %>
                <a class="btn btn-secondary btn-sm" href="<%= ctx %>/ta/messages?page=<%= curPage + 1 %><%= boxQuery(listFilter) %>">Next</a>
                <% } else { %>
                <span class="btn btn-secondary btn-sm ta-msg-page-disabled" aria-disabled="true">Next</span>
                <% } %>
            </nav>
            </div>
            <% } %>
<% } %>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Inbox</div>
<% if (detail != null) {
    Integer unreadDetailObj = (Integer) request.getAttribute("notificationsUnreadTotal");
    int unreadDetail = unreadDetailObj != null ? unreadDetailObj : 0;
%>
                <p class="widget-line">Viewing one message.</p>
                <p class="widget-line"><a href="<%= ctx %>/ta/messages?page=<%= request.getAttribute("listPageForBack") != null ? (Integer) request.getAttribute("listPageForBack") : 1 %><%= boxQuery(listFilter) %>">Return to list</a></p>
                <% if (unreadDetail > 0) { %>
                <form action="<%= ctx %>/ta/messages" method="post" class="ta-messages-markall-widget">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <input type="hidden" name="action" value="markAllRead">
                    <input type="hidden" name="page" value="1">
                    <input type="hidden" name="box" value="all">
                    <button type="submit" class="btn btn-secondary btn-sm">Mark all read</button>
                </form>
                <p class="widget-line muted-inline"><%= unreadDetail %> unread</p>
                <% } %>
<% } else {
    Integer unreadTotalObjW = (Integer) request.getAttribute("notificationsUnreadTotal");
    int unreadTotalW = unreadTotalObjW != null ? unreadTotalObjW : 0;
    Integer readTotalObjW = (Integer) request.getAttribute("notificationsReadTotal");
    int readTotalW = readTotalObjW != null ? readTotalObjW : 0;
    Integer allTotalObjW = (Integer) request.getAttribute("notificationsAllTotal");
    int allTotalW = allTotalObjW != null ? allTotalObjW : 0;
%>
                <p class="widget-line">Unread: <strong><%= unreadTotalW %></strong></p>
                <p class="widget-line">Read: <%= readTotalW %></p>
                <p class="widget-line">Total: <%= allTotalW %></p>
<% } %>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Tip</div>
                <p class="widget-line">Use <strong>Mark selected unread</strong> on the list to restore unread state.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
