<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="bupt.ta.model.User" %>
<%@ page import="bupt.ta.model.TAProfile" %>
<%@ page import="bupt.ta.model.Application" %>
<%@ page import="bupt.ta.model.ApplicationEvent" %>
<%@ page import="bupt.ta.model.InterviewEvaluation" %>
<%@ page import="bupt.ta.model.Job" %>
<%
    User user = (User) request.getAttribute("applicantUser");
    TAProfile profile = (TAProfile) request.getAttribute("applicantProfile");
    List<Object[]> appRows = (List<Object[]>) request.getAttribute("appRows");
    if (appRows == null) appRows = java.util.Collections.emptyList();
    Integer selectedObj = (Integer) request.getAttribute("selectedCount");
    Integer pendingObj = (Integer) request.getAttribute("pendingCount");
    Integer otherObj = (Integer) request.getAttribute("otherCount");
    Integer interviewObj = (Integer) request.getAttribute("interviewCount");
    java.util.Map<String, InterviewEvaluation> evaluationByApplicationId = (java.util.Map<String, InterviewEvaluation>) request.getAttribute("evaluationByApplicationId");
    if (evaluationByApplicationId == null) evaluationByApplicationId = java.util.Collections.emptyMap();
    java.util.Map<String, List<ApplicationEvent>> eventsByApplicationId = (java.util.Map<String, List<ApplicationEvent>>) request.getAttribute("eventsByApplicationId");
    if (eventsByApplicationId == null) eventsByApplicationId = java.util.Collections.emptyMap();
    int selected = selectedObj != null ? selectedObj : 0;
    int pending = pendingObj != null ? pendingObj : 0;
    int interview = interviewObj != null ? interviewObj : 0;
    int other = otherObj != null ? otherObj : 0;
    Boolean hidePi = (Boolean) request.getAttribute("hideApplicantPersonalInfo");
    boolean hidePersonal = hidePi != null && hidePi.booleanValue();
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Applicant Detail - MO</title>
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
                <div class="icon-dot">J</div>
                <div class="icon-dot">P</div>
                <div class="icon-dot active">D</div>
            </div>
            <%@ include file="/WEB-INF/jspf/mo-side-nav.jspf" %>
        </div>
        <main class="main-panel mo-main">
    <div class="mo-applicant-head">
    <h1><%= hidePersonal ? "Withdrawn application record" : "Applicant Detail" %></h1>
    <% if (hidePersonal) { %>
    <p class="mo-manage-hero-lead">Personal data is hidden for withdrawn applicants.</p>
    <% } else { %>
    <p class="mo-manage-hero-lead">Review profile, academic background, and applications to your postings.</p>
    <% } %>
    </div>
    <% if (hidePersonal) { %>
    <div class="context-card">
        <strong>Privacy</strong>
        <p>This applicant withdrew all applications to your jobs. Name, contact details, profile, and CV are not shown. Only status and job reference are listed below.</p>
    </div>
    <% } else { %>
    <div class="context-card">
        <strong>Review Tip</strong>
        <p>Check skills fit, availability and previous application status together for a balanced selection.</p>
    </div>
    <% if (user != null && Boolean.TRUE.equals(request.getAttribute("llmEnabled"))) { %>
    <div class="llm-insight-card context-card" data-applicant-insight data-applicant-id="<%= escHtml(user.getId()) %>">
        <strong>AI applicant insight (DeepSeek)</strong>
        <p class="muted-inline">Click to generate a short narrative comparing this applicant to their most recent application to your jobs.</p>
        <div class="ai-summary-actions">
            <button type="button" class="btn btn-secondary btn-sm applicant-insight-btn">Generate AI insight</button>
        </div>
        <div class="applicant-insight-result"></div>
    </div>
    <% } %>
    <% } %>
    <% if (!hidePersonal) { %>
    <% if ("1".equals(request.getParameter("evaluationSaved"))) { %><p class="success">Interview evaluation saved.</p><% } %>
    <% String detailErr = request.getParameter("error");
       if ("evaluation_status".equals(detailErr)) { %><p class="error">Only interview, waitlist, selected, or rejected applications can be evaluated.</p><% }
       else if ("evaluation_invalid".equals(detailErr)) { %><p class="error">Please choose a valid recommendation.</p><% } %>
    <div class="detail-grid">
        <div class="detail-card">
            <h3>Basic Information</h3>
            <p><strong>Name:</strong> <%= user != null && user.getRealName() != null && !user.getRealName().isEmpty() ? escHtml(user.getRealName()) : "-" %></p>
            <p><strong>Email:</strong> <%= (profile != null && profile.getEmail() != null && !profile.getEmail().isEmpty()) ? escHtml(profile.getEmail()) : (user != null && user.getEmail() != null && !user.getEmail().isEmpty() ? escHtml(user.getEmail()) : "-") %></p>
            <p><strong>Student ID:</strong> <%= profile != null && profile.getStudentId() != null && !profile.getStudentId().isEmpty() ? escHtml(profile.getStudentId()) : "-" %></p>
            <p><strong>Phone:</strong> <%= profile != null && profile.getPhone() != null && !profile.getPhone().isEmpty() ? escHtml(profile.getPhone()) : "-" %></p>
            <p><strong>Availability:</strong> <%= profile != null && profile.getAvailability() != null && !profile.getAvailability().isEmpty() ? escHtml(profile.getAvailability()) : "-" %></p>
        </div>
        <div class="detail-card">
            <h3>Application Summary</h3>
            <p><strong>Total (for your jobs):</strong> <%= appRows.size() %></p>
            <p><strong>Selected:</strong> <span class="status-selected"><%= selected %></span></p>
            <p><strong>Pending:</strong> <span class="status-pending"><%= pending %></span></p>
            <p><strong>Interview:</strong> <span class="status-pending"><%= interview %></span></p>
            <p><strong>Other:</strong> <span class="status-rejected"><%= other %></span></p>
        </div>
    </div>

    <div class="detail-card applicant-academic-card">
        <h3>Academic &amp; experience</h3>
        <p><strong>Degree:</strong> <%= profile != null && profile.getDegree() != null && !profile.getDegree().isEmpty() ? escHtml(profile.getDegree()) : "-" %></p>
        <p><strong>Programme:</strong> <%= profile != null && profile.getProgramme() != null && !profile.getProgramme().isEmpty() ? escHtml(profile.getProgramme()) : "-" %></p>
        <p><strong>Year of study:</strong> <%= profile != null && profile.getYearOfStudy() != null && !profile.getYearOfStudy().isEmpty() ? escHtml(profile.getYearOfStudy()) : "-" %></p>
        <p><strong>Skills:</strong> <%= profile != null && profile.getSkills() != null && !profile.getSkills().isEmpty() ? escHtml(String.join(", ", profile.getSkills())) : "-" %></p>
        <div class="detail-block-text">
            <strong>TA experience</strong>
            <p class="pre-wrap"><%= profile != null && profile.getTaExperience() != null && !profile.getTaExperience().isEmpty() ? escHtml(profile.getTaExperience()) : "Not provided." %></p>
        </div>
        <p><strong>CV:</strong>
            <% if (profile != null && profile.getCvFilePath() != null && !profile.getCvFilePath().isEmpty() && user != null) { %>
            <a href="${pageContext.request.contextPath}/view-cv?userId=<%= user.getId() %>" target="_blank" rel="noopener">View</a>
            <span class="muted-inline"> | </span>
            <a href="${pageContext.request.contextPath}/view-cv?userId=<%= user.getId() %>&amp;download=1">Download</a>
            <% } else { %>
            Not uploaded
            <% } %>
        </p>
    </div>

    <div class="detail-card">
        <h3>Self introduction</h3>
        <p class="pre-wrap"><%= profile != null && profile.getIntroduction() != null && !profile.getIntroduction().isEmpty() ? escHtml(profile.getIntroduction()) : "No introduction provided." %></p>
    </div>
    <% } %>

    <h2>Applications to Your Jobs</h2>
    <div class="mo-detail-table-wrap">
    <table>
        <thead>
        <tr>
            <th>Job Title</th>
            <th>Module</th>
            <th>Applied At</th>
            <th>Status</th>
            <th>Evaluation</th>
            <th>Notes</th>
        </tr>
        </thead>
        <tbody>
        <% for (Object[] row : appRows) {
            Application a = (Application) row[0];
            Job j = (Job) row[1];
            String statusClass = "status-pending";
            if ("SELECTED".equals(a.getStatus())) statusClass = "status-selected";
            else if ("INTERVIEW".equals(a.getStatus())) statusClass = "status-pending";
            else if ("REJECTED".equals(a.getStatus()) || "WITHDRAWN".equals(a.getStatus())) statusClass = "status-rejected";
            InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
        %>
        <tr>
            <td><%= j != null ? j.getTitle() : a.getJobId() %></td>
            <td><%= j != null ? j.getModuleCode() : "-" %></td>
            <td><%= a.getAppliedAt() != null ? a.getAppliedAt() : "-" %></td>
            <td class="<%= statusClass %>"><%= a.getStatus() %></td>
            <td><% if (ev != null) { %><strong><%= ev.getTotalScore() %>/100</strong><br><span class="muted-inline"><%= escHtml(ev.getRecommendationLabel()) %></span><% } else { %><span class="muted-inline">Not evaluated</span><% } %></td>
            <td>
                <%= a.getNotes() != null && !a.getNotes().isEmpty() ? escHtml(a.getNotes()) : "-" %>
                <% if (a.getDecisionReason() != null && !a.getDecisionReason().isEmpty()) { %>
                <div class="muted-inline"><strong>Decision:</strong> <%= escHtml(a.getDecisionReason()) %></div>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    </div>
    <% if (!hidePersonal) { %>
    <h2>Interview Evaluation</h2>
    <div class="evaluation-card-grid">
        <% for (Object[] row : appRows) {
            Application a = (Application) row[0];
            Job j = (Job) row[1];
            boolean canEvaluate = "INTERVIEW".equals(a.getStatus()) || "WAITLIST".equals(a.getStatus()) || "SELECTED".equals(a.getStatus()) || "REJECTED".equals(a.getStatus());
            InterviewEvaluation ev = evaluationByApplicationId.get(a.getId());
            int tech = ev != null ? ev.getTechnicalScore() : 3;
            int teach = ev != null ? ev.getTeachingScore() : 3;
            int comm = ev != null ? ev.getCommunicationScore() : 3;
            int avail = ev != null ? ev.getAvailabilityScore() : 3;
            int resp = ev != null ? ev.getResponsibilityScore() : 3;
            String rec = ev != null && ev.getRecommendation() != null ? ev.getRecommendation() : "HIRE";
        %>
        <section class="detail-card evaluation-card">
            <h3><%= j != null ? escHtml(j.getTitle()) : escHtml(a.getJobId()) %></h3>
            <p class="muted-inline">Application <code><%= escHtml(a.getId()) %></code> | Status <strong><%= escHtml(a.getStatus()) %></strong></p>
            <% if (ev != null) { %>
            <p><strong>Current score:</strong> <%= ev.getTotalScore() %>/100 | <strong>Recommendation:</strong> <%= escHtml(ev.getRecommendationLabel()) %></p>
            <% } else { %>
            <p class="muted-inline">No interview evaluation saved yet.</p>
            <% } %>
            <% if (canEvaluate) { %>
            <form action="${pageContext.request.contextPath}/mo/interview-evaluation" method="post" class="form evaluation-form">
                <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                <input type="hidden" name="applicationId" value="<%= escHtml(a.getId()) %>">
                <div class="evaluation-score-grid">
                    <label>Technical <input type="number" name="technicalScore" min="1" max="5" value="<%= tech %>" required></label>
                    <label>Teaching <input type="number" name="teachingScore" min="1" max="5" value="<%= teach %>" required></label>
                    <label>Communication <input type="number" name="communicationScore" min="1" max="5" value="<%= comm %>" required></label>
                    <label>Availability <input type="number" name="availabilityScore" min="1" max="5" value="<%= avail %>" required></label>
                    <label>Responsibility <input type="number" name="responsibilityScore" min="1" max="5" value="<%= resp %>" required></label>
                </div>
                <label>Recommendation</label>
                <select name="recommendation" required>
                    <option value="STRONG_HIRE" <%= "STRONG_HIRE".equals(rec) ? "selected" : "" %>>Strong hire</option>
                    <option value="HIRE" <%= "HIRE".equals(rec) ? "selected" : "" %>>Hire</option>
                    <option value="WAITLIST" <%= "WAITLIST".equals(rec) ? "selected" : "" %>>Waitlist</option>
                    <option value="REJECT" <%= "REJECT".equals(rec) ? "selected" : "" %>>Reject</option>
                </select>
                <label>Strengths</label>
                <textarea name="strengths" rows="2"><%= ev != null && ev.getStrengths() != null ? escHtml(ev.getStrengths()) : "" %></textarea>
                <label>Concerns / risks</label>
                <textarea name="concerns" rows="2"><%= ev != null && ev.getConcerns() != null ? escHtml(ev.getConcerns()) : "" %></textarea>
                <label>Internal notes</label>
                <textarea name="internalNotes" rows="2"><%= ev != null && ev.getInternalNotes() != null ? escHtml(ev.getInternalNotes()) : "" %></textarea>
                <button type="submit" class="btn btn-primary">Save evaluation</button>
            </form>
            <% } else { %>
            <p class="muted-inline">Move this applicant to interview before recording an evaluation.</p>
            <% } %>
        </section>
        <% } %>
    </div>
    <h2>Application Timeline</h2>
    <div class="evaluation-card-grid">
        <% for (Object[] row : appRows) {
            Application a = (Application) row[0];
            Job j = (Job) row[1];
            List<ApplicationEvent> events = eventsByApplicationId.get(a.getId());
            if (events == null) events = java.util.Collections.emptyList();
        %>
        <section class="detail-card evaluation-card">
            <h3><%= j != null ? escHtml(j.getTitle()) : escHtml(a.getJobId()) %></h3>
            <p class="muted-inline">Application <code><%= escHtml(a.getId()) %></code></p>
            <% if (events.isEmpty()) { %>
            <p class="muted-inline">No timeline events recorded yet.</p>
            <% } else { %>
            <ol class="timeline-list">
                <% for (ApplicationEvent ev : events) { %>
                <li>
                    <strong><%= escHtml(ev.getTitle() != null && !ev.getTitle().isEmpty() ? ev.getTitle() : ev.getEventType()) %></strong>
                    <div class="muted-inline"><%= escHtml(ev.getCreatedAt() != null ? ev.getCreatedAt().replace("T", " ").replaceFirst("\\..*$", "") : "-") %> | <%= escHtml(ev.getActorRole() != null && !ev.getActorRole().isEmpty() ? ev.getActorRole() : "SYSTEM") %><% if (ev.getActorName() != null && !ev.getActorName().isEmpty()) { %> - <%= escHtml(ev.getActorName()) %><% } %></div>
                    <% if (ev.getFromStatus() != null && !ev.getFromStatus().isEmpty() && ev.getToStatus() != null && !ev.getToStatus().isEmpty() && !ev.getFromStatus().equals(ev.getToStatus())) { %>
                    <div class="muted-inline"><%= escHtml(ev.getFromStatus()) %> &rarr; <%= escHtml(ev.getToStatus()) %></div>
                    <% } %>
                    <% if (ev.getDetail() != null && !ev.getDetail().isEmpty()) { %>
                    <p class="pre-wrap"><%= escHtml(ev.getDetail()) %></p>
                    <% } %>
                </li>
                <% } %>
            </ol>
            <% } %>
        </section>
        <% } %>
    </div>
    <% } %>
        </main>
        <aside class="right-sidebar">
            <% if (!hidePersonal) { %>
            <div class="widget-card">
                <div class="widget-title">Applicant Snapshot</div>
                <p class="widget-line">Selected: <%= selected %></p>
                <p class="widget-line">Pending: <%= pending %> | Interview: <%= interview %></p>
                <p class="widget-line">Other: <%= other %></p>
            </div>
            <% } %>
        </aside>
    </div>
</div>
<script>
(function () {
    function escapeHtml(text) {
        return String(text == null ? "" : text)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    var card = document.querySelector("[data-applicant-insight]");
    if (!card) return;
    var btn = card.querySelector(".applicant-insight-btn");
    var resultBox = card.querySelector(".applicant-insight-result");
    var applicantId = card.getAttribute("data-applicant-id");

    btn.addEventListener("click", function () {
        if (!applicantId) return;
        var oldText = btn.textContent;
        btn.disabled = true;
        btn.textContent = "Generating...";
        resultBox.innerHTML = "<p class='muted-inline'>Calling AI, this may take up to 30 seconds...</p>";

        var url = "${pageContext.request.contextPath}/mo/match-insight?applicantId=" + encodeURIComponent(applicantId);
        fetch(url, { method: "GET", credentials: "same-origin" })
            .then(function (resp) {
                return resp.json().then(function (data) { return { ok: resp.ok, body: data }; });
            })
            .then(function (res) {
                if (!res.ok || !res.body || !res.body.ok) {
                    var msg = (res.body && res.body.error) ? res.body.error : "Failed to generate insight";
                    throw new Error(msg);
                }
                resultBox.innerHTML =
                    "<p class='pre-wrap llm-insight-body'>" + escapeHtml(res.body.insight) + "</p>" +
                    "<p class='muted-inline llm-insight-disclaimer'>Narrative only; verify against profile and interview.</p>";
                btn.textContent = "Regenerate AI insight";
                btn.disabled = false;
            })
            .catch(function (err) {
                resultBox.innerHTML = "<p class='error'>AI insight failed: " + escapeHtml(err.message || "Unknown error") + "</p>";
                btn.textContent = oldText;
                btn.disabled = false;
            });
    });
})();
</script>
</body>
</html>
