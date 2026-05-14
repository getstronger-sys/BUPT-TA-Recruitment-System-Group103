<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.Job" %>
<%@ page import="bupt.ta.model.TAProfile" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%
    Job job = (Job) request.getAttribute("job");
    TAProfile profile = (TAProfile) request.getAttribute("profile");
    String profileEditUrl = (String) request.getAttribute("profileEditUrl");
    if (job == null) {
        response.sendRedirect(request.getContextPath() + "/ta/jobs?error=job_not_found");
        return;
    }
    if (profile == null) profile = new TAProfile();
    request.setAttribute("taNavActive", "jobs");
    String uid = (String) session.getAttribute("userId");
    boolean hasCv = profile.getCvFilePath() != null && !profile.getCvFilePath().trim().isEmpty();
    if (profileEditUrl == null) profileEditUrl = request.getContextPath() + "/ta/profile";
    int taSlots = job.getTaSlots() > 0 ? job.getTaSlots() : 1;
    List<String> allocationItems = new ArrayList<>();
    String taPlanRaw = job.getTaAllocationPlan();
    if (taPlanRaw != null) {
        String[] planParts = taPlanRaw.split("[\\n;；]+");
        for (String p : planParts) {
            String t = p != null ? p.trim() : "";
            if (!t.isEmpty()) allocationItems.add(escHtml(t));
        }
    }
    List<String[]> weekMilestones = new ArrayList<>();
    String timelineRaw = job.getExamTimeline() != null ? job.getExamTimeline() : "";
    Matcher weekMatcher = Pattern.compile("(?:Week|W)\\s*(\\d{1,2})\\s*[:\\-]?\\s*([^;\\n]+)?", Pattern.CASE_INSENSITIVE).matcher(timelineRaw);
    while (weekMatcher.find()) {
        String weekNo = weekMatcher.group(1);
        String detail = weekMatcher.group(2) != null ? weekMatcher.group(2).trim() : "";
        weekMilestones.add(new String[]{weekNo, escHtml(detail)});
    }
    if (weekMilestones.isEmpty() && !timelineRaw.trim().isEmpty()) {
        String[] fallback = timelineRaw.split("[;\\n]+");
        int wk = 1;
        for (String f : fallback) {
            String t = f != null ? f.trim() : "";
            if (t.isEmpty()) continue;
            weekMilestones.add(new String[]{String.valueOf(wk), escHtml(t)});
            wk += 3;
        }
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Confirm application - TA</title>
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
                <div class="icon-dot">H</div>
                <div class="icon-dot active">F</div>
                <div class="icon-dot">S</div>
                <div class="icon-dot">A</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel">
            <p class="breadcrumb-line"><a href="${pageContext.request.contextPath}/ta/job?jobId=<%= escHtml(job.getId()) %>">&larr; Back to job details</a></p>
            <h1>Confirm your application</h1>
            <% if ("invalid_role".equals(request.getParameter("error"))) { %>
            <p class="error">Please choose a valid TA role for this job before submitting.</p>
            <% } %>
            <% if ("workload_hours_cap".equals(request.getParameter("error"))) { %>
            <p class="error">You cannot apply: your already selected posts plus this job’s estimated workload would exceed the system’s hourly cap. Withdraw from a role or contact an administrator.</p>
            <% } %>
            <div class="context-card">
                <strong>What happens next</strong>
                <p>Your application will be sent to the module organiser. They will review your <strong>current saved profile and CV</strong> (not a frozen copy). You can update them anytime before a decision.</p>
            </div>

            <div class="detail-card">
                <h3>Job</h3>
                <p><strong><%= escHtml(job.getTitle() != null ? job.getTitle() : "") %></strong></p>
                <p>Module: <%= escHtml(job.getModuleCode() != null ? job.getModuleCode() : "-") %>
                    <% if (job.getModuleName() != null && !job.getModuleName().isEmpty()) { %> | <%= escHtml(job.getModuleName()) %><% } %></p>
                <p><strong>TA slots:</strong> <%= job.getTaSlots() > 0 ? job.getTaSlots() : 1 %></p>
                <p><strong>Course timeline:</strong></p>
                <% if (weekMilestones.isEmpty()) { %>
                <p class="pre-wrap"><%= escHtml(job.getExamTimeline() != null && !job.getExamTimeline().isEmpty() ? job.getExamTimeline() : "Not provided") %></p>
                <% } else { %>
                <div class="week-timeline-list">
                    <% for (String[] item : weekMilestones) {
                           int weekNum = 1;
                           try { weekNum = Integer.parseInt(item[0]); } catch (Exception ignored) {}
                           int progress = Math.max(0, Math.min(100, (int) Math.round((weekNum / 14.0) * 100)));
                    %>
                    <div class="week-timeline-row">
                        <div class="week-line">
                            <span class="week-label">W<%= weekNum %></span>
                            <span class="week-progress"><span class="week-progress-fill" style="width:<%= progress %>%"></span></span>
                        </div>
                        <div class="week-desc"><%= item[1] != null && !item[1].isEmpty() ? item[1] : "Milestone" %></div>
                    </div>
                    <% } %>
                </div>
                <% } %>
                <p class="pre-wrap"><strong>Multi-TA allocation:</strong> <%= escHtml(job.getTaAllocationPlan() != null && !job.getTaAllocationPlan().isEmpty() ? job.getTaAllocationPlan() : "Not provided") %></p>
                <p class="pre-wrap"><strong>Interview arrangement:</strong>
                    <%= escHtml(job.getInterviewSchedule() != null && !job.getInterviewSchedule().isEmpty() ? job.getInterviewSchedule() : "Not provided") %><%
                    if (job.getInterviewLocation() != null && !job.getInterviewLocation().trim().isEmpty()) { %>
                    <br><span class="muted-inline">Location:</span> <%= escHtml(job.getInterviewLocation().trim()) %><%
                    } else { %>
                    <br><span class="muted-inline">Location:</span> Not provided<%
                    } %>
                </p>
                <div class="ta-duty-board">
                    <% for (int idx = 1; idx <= taSlots; idx++) {
                           String allocation = idx <= allocationItems.size()
                                   ? allocationItems.get(idx - 1)
                                   : "General support: lab/tutorial assistance, Q&A, and exam-day backup.";
                    %>
                    <article class="ta-duty-card">
                        <div class="ta-duty-head"><span class="arr-icon arr-icon-slots" aria-hidden="true">TA</span>TA-<%= idx %></div>
                        <p class="pre-wrap"><%= allocation %></p>
                    </article>
                    <% } %>
                </div>
            </div>

            <div class="detail-card">
                <h3>What will be shared from your profile</h3>
                <p><strong>Student ID:</strong> <% if (profile.getStudentId() != null && !profile.getStudentId().isEmpty()) { %><%= escHtml(profile.getStudentId()) %><% } else { %><span class="muted-inline">Not set</span><% } %></p>
                <p><strong>Degree:</strong> <%= profile.getDegree() != null && !profile.getDegree().isEmpty() ? escHtml(profile.getDegree()) : "-" %></p>
                <p><strong>Programme:</strong> <%= profile.getProgramme() != null && !profile.getProgramme().isEmpty() ? escHtml(profile.getProgramme()) : "-" %></p>
                <p><strong>Skills:</strong> <% if (profile.getSkills() != null && !profile.getSkills().isEmpty()) { %><%= escHtml(String.join(", ", profile.getSkills())) %><% } else { %><span class="muted-inline">Not set</span><% } %></p>
                <p><strong>TA experience:</strong> <%= profile.getTaExperience() != null && !profile.getTaExperience().isEmpty() ? escHtml(profile.getTaExperience()) : "-" %></p>
                <p><strong>Availability:</strong> <%= profile.getAvailability() != null && !profile.getAvailability().isEmpty() ? escHtml(profile.getAvailability()) : "-" %></p>
                <div class="detail-block-text">
                    <strong>Introduction</strong>
                    <p class="pre-wrap"><%= profile.getIntroduction() != null && !profile.getIntroduction().isEmpty() ? escHtml(profile.getIntroduction()) : "Not provided." %></p>
                </div>
                <p><strong>CV:</strong>
                    <% if (hasCv && uid != null) { %>
                    <a href="${pageContext.request.contextPath}/view-cv?userId=<%= escHtml(uid) %>" target="_blank" rel="noopener">Preview</a>
                    <% } else { %>
                    <span class="muted-inline">Not uploaded</span>
                    <% } %>
                </p>
            </div>

            <div class="apply-confirm-actions">
                <a href="<%= escHtml(profileEditUrl) %>" class="btn btn-primary">Edit profile first</a>
                <form action="${pageContext.request.contextPath}/ta/apply" method="post" class="apply-confirm-submit-form">
                    <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                    <input type="hidden" name="jobId" value="<%= escHtml(job.getId()) %>">
                    <label for="preferredRole"><strong>Choose your preferred TA role</strong></label>
                    <select id="preferredRole" name="preferredRole" class="note-input" required>
                        <% for (int idx = 1; idx <= taSlots; idx++) { %>
                        <option value="TA-<%= idx %>">TA-<%= idx %></option>
                        <% } %>
                    </select>
                    <button type="submit" class="btn btn-success btn-lg">Submit application</button>
                </form>
            </div>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card">
                <div class="widget-title">Checklist</div>
                <p class="widget-line">Update skills and CV before submitting if anything is missing.</p>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
