<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.AdminSettings" %>
<%
    AdminSettings settings = (AdminSettings) request.getAttribute("adminSettings");
    if (settings == null) settings = new AdminSettings();
    request.setAttribute("adminNavActive", "email");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Email Settings - TA Recruitment</title>
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
                <div class="icon-dot active">E</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot">U</div>
            </div>
            <%@ include file="/WEB-INF/jspf/admin-side-nav.jspf" %>
        </div>
        <main class="main-panel admin-main admin-page">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Messaging</p>
                <h1>Email (SMTP) settings</h1>
                <p class="ta-page-lead">Configure SMTP delivery for email notifications and unread-message reminders.</p>
            </header>

            <% boolean adminEmailFlash = "1".equals(request.getParameter("saved"));
               String adminEmailErr = (String) request.getAttribute("error");
               if (adminEmailFlash || adminEmailErr != null) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("saved"))) { %>
            <p class="success">Email settings saved.</p>
            <% } %>
            <% if (adminEmailErr != null) { %>
            <p class="error"><%= escHtml(adminEmailErr) %></p>
            <% } %>
            </div>
            <% } %>

            <section class="detail-card admin-settings-card">
                <h3>SMTP configuration</h3>
                <p class="muted-inline">Leave password empty to keep the existing saved password.</p>
                <form action="${pageContext.request.contextPath}/admin/email" method="post" class="form form--admin-settings">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <label class="checkbox-line" for="admin-mail-enabled">
                        <input id="admin-mail-enabled" type="checkbox" name="mailEnabled" <%= settings.isMailEnabled() ? "checked" : "" %>>
                        Enable email delivery
                    </label>
                    <label for="admin-mail-host">SMTP host</label>
                    <input id="admin-mail-host" type="text" name="mailHost" value="<%= escHtml(settings.getMailHost()) %>" placeholder="e.g. smtp.example.com">
                    <label for="admin-mail-port">SMTP port</label>
                    <input id="admin-mail-port" type="number" min="1" max="65535" name="mailPort" value="<%= settings.getMailPort() %>" placeholder="587">
                    <label for="admin-mail-from">Sender address (from)</label>
                    <input id="admin-mail-from" type="email" name="mailFrom" value="<%= escHtml(settings.getMailFrom()) %>" placeholder="no-reply@example.com">
                    <label for="admin-mail-username">SMTP username</label>
                    <input id="admin-mail-username" type="text" name="mailUsername" value="<%= escHtml(settings.getMailUsername()) %>">
                    <label for="admin-mail-password">SMTP password</label>
                    <input id="admin-mail-password" type="password" name="mailPassword" value="" placeholder="(unchanged if empty)">
                    <label class="checkbox-line" for="admin-mail-auth">
                        <input id="admin-mail-auth" type="checkbox" name="mailAuth" <%= settings.isMailAuth() ? "checked" : "" %>>
                        Use SMTP auth
                    </label>
                    <label class="checkbox-line" for="admin-mail-starttls">
                        <input id="admin-mail-starttls" type="checkbox" name="mailStartTls" <%= settings.isMailStartTls() ? "checked" : "" %>>
                        Enable STARTTLS
                    </label>
                    <label class="checkbox-line" for="admin-mail-ssl">
                        <input id="admin-mail-ssl" type="checkbox" name="mailSsl" <%= settings.isMailSsl() ? "checked" : "" %>>
                        Enable SSL (mail.smtp.ssl.enable)
                    </label>
                    <label for="admin-mail-app-base">Portal base URL (optional)</label>
                    <input id="admin-mail-app-base" type="text" name="mailAppBaseUrl" value="<%= escHtml(settings.getMailAppBaseUrl()) %>" placeholder="e.g. http://localhost:8080/ta-recruitment">
                    <button type="submit" class="btn btn-primary">Save email settings</button>
                </form>
            </section>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Tips</div>
                <p class="widget-line">Port 587: STARTTLS on, SSL off.</p>
                <p class="widget-line">Port 465: SSL on, STARTTLS off.</p>
                <p class="widget-line">Sender (from) should usually match the SMTP username.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>

