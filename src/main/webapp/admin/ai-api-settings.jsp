<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.AiApiSettings" %>
<%
    AiApiSettings settings = (AiApiSettings) request.getAttribute("aiApiSettings");
    if (settings == null) settings = new AiApiSettings();
    Boolean effectiveOk = (Boolean) request.getAttribute("deepSeekEffectiveConfigured");
    boolean effectiveConfigured = effectiveOk != null && effectiveOk;
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>AI API Settings - TA Recruitment</title>
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
                <div class="icon-dot active">A</div>
                <div class="icon-dot">U</div>
            </div>
            <aside class="side-nav">
                <a href="${pageContext.request.contextPath}/admin/dashboard">Summary</a>
                <a href="${pageContext.request.contextPath}/admin/workload">Workload</a>
                <a href="${pageContext.request.contextPath}/admin/monitoring">Monitoring</a>
                <a href="${pageContext.request.contextPath}/admin/email">Email</a>
                <a class="active" href="${pageContext.request.contextPath}/admin/ai-api">AI API</a>
                <a href="${pageContext.request.contextPath}/admin/users">Users</a>
            </aside>
        </div>
        <main class="main-panel admin-main">
            <h1>AI / LLM API (DeepSeek)</h1>
            <p class="ta-page-lead">Configure the DeepSeek-compatible endpoint used for CV pre-fill, match insight and MO screening summaries.</p>

            <% if ("1".equals(request.getParameter("saved"))) { %>
            <p class="success">AI API settings saved.</p>
            <% } %>
            <% String err = (String) request.getAttribute("error"); if (err != null) { %>
            <p class="error"><%= escHtml(err) %></p>
            <% } %>

            <div class="context-card">
                <strong>Current status</strong>
                <p>Runtime state: <strong><%= effectiveConfigured ? "Configured (calls allowed)" : "Not configured" %></strong>.</p>
            </div>

            <section class="detail-card admin-settings-card">
                <h3>API configuration</h3>
                <p class="muted-inline">Leave the API key empty to keep the previously saved key.</p>
                <form action="${pageContext.request.contextPath}/admin/ai-api" method="post" class="form form--admin-settings">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <label class="checkbox-line" for="admin-ai-enabled">
                        <input id="admin-ai-enabled" type="checkbox" name="apiEnabled" <%= settings.isApiEnabled() ? "checked" : "" %>>
                        Enable LLM features (calls are skipped when unchecked, even if a key is saved)
                    </label>
                    <label class="checkbox-line" for="admin-ai-streaming">
                        <input id="admin-ai-streaming" type="checkbox" name="streamingEnabled" <%= settings.isStreamingEnabled() ? "checked" : "" %>>
                        Enable streaming responses (insight and summary text appears token-by-token instead of after the full response)
                    </label>
                    <label for="admin-ai-provider">Provider <span class="muted-inline">(label only)</span></label>
                    <input id="admin-ai-provider" type="text" name="provider" value="<%= escHtml(settings.getProvider()) %>" placeholder="deepseek / openai / moonshot / ollama ...">
                    <label for="admin-ai-base">API Base URL</label>
                    <input id="admin-ai-base" type="text" name="baseUrl" value="<%= escHtml(settings.getBaseUrl()) %>" placeholder="https://api.deepseek.com (any OpenAI-compatible endpoint)">
                    <label for="admin-ai-model">Model name</label>
                    <input id="admin-ai-model" type="text" name="model" value="<%= escHtml(settings.getModel()) %>" placeholder="deepseek-chat / gpt-4o-mini / qwen2.5 ...">
                    <label for="admin-ai-key">API key</label>
                    <input id="admin-ai-key" type="password" name="apiKey" value="" autocomplete="off" placeholder="(unchanged if empty)">
                    <button type="submit" class="btn btn-primary">Save AI API settings</button>
                </form>
            </section>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card">
                <div class="widget-title">Where this is used</div>
                <p class="widget-line">CV auto-fill when a TA uploads a resume.</p>
                <p class="widget-line">Skill match insight on TA job detail and MO applicant detail pages.</p>
                <p class="widget-line">MO screening summary card on the applicant review panel.</p>
            </div>
            <div class="widget-card">
                <div class="widget-title">Tips</div>
                <p class="widget-line">Uncheck "Enable LLM features" to pause AI calls without clearing your saved key.</p>
                <p class="widget-line">Leave Base URL / Model empty to use DeepSeek defaults.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
