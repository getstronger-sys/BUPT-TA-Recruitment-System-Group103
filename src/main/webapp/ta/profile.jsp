<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="bupt.ta.model.TAProfile" %>
<%@ page import="bupt.ta.model.User" %>
<%@ page import="java.util.List" %>
<% TAProfile profile = (TAProfile) request.getAttribute("profile"); if (profile == null) profile = new TAProfile();
   User accountUser = (User) request.getAttribute("accountUser");
   String emailValue = "";
   if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
       emailValue = profile.getEmail();
   } else if (accountUser != null && accountUser.getEmail() != null && !accountUser.getEmail().isEmpty()) {
       emailValue = accountUser.getEmail();
   }
   String returnUrlAttr = (String) request.getAttribute("returnUrl");
   request.setAttribute("taNavActive", "profile");
   String aiStatus = request.getParameter("ai_status");
   boolean taProfileFlash = "1".equals(request.getParameter("success"))
           || "cv_success".equals(request.getParameter("cv_success")) || "1".equals(request.getParameter("cv_success"))
           || "no_file".equals(request.getParameter("error"))
           || "invalid_type".equals(request.getParameter("error"))
           || request.getAttribute("errorMessage") != null;
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>My Profile - TA Recruitment</title>
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
                <div class="icon-dot">F</div>
                <div class="icon-dot">S</div>
                <div class="icon-dot">A</div>
                <div class="icon-dot active">P</div>
            </div>
            <%@ include file="/WEB-INF/jspf/ta-side-nav.jspf" %>
        </div>
        <main class="main-panel ta-main ta-page ta-page--profile">
            <header class="ta-page-header">
                <p class="ta-page-kicker">Your account</p>
                <h1>My profile</h1>
                <p class="ta-page-lead">Keep your skills and CV up to date so module organisers and the matching system can rank you fairly.</p>
            </header>

            <div class="ta-panel ta-panel--tip">
                <strong class="ta-panel__title">Profile tip</strong>
                <p class="ta-panel__body">Complete academic details, experience, skills, availability and CV for easier review. The <strong>Email</strong> field below is shown to module organisers when you apply (along with your Student ID and phone).</p>
            </div>

            <% if (taProfileFlash) { %>
            <div class="ta-page-flashes">
            <% if ("1".equals(request.getParameter("success"))) { %><p class="success">Profile saved.</p><% } %>
            <% if ("cv_success".equals(request.getParameter("cv_success")) || "1".equals(request.getParameter("cv_success"))) { %>
            <p class="success">CV uploaded successfully.
            <% if ("filled".equals(aiStatus) || "1".equals(request.getParameter("ai_fill"))) { %>
             <strong>AI pre-filled empty profile fields</strong> from your CV. Please review and save.
            <% } else if ("no_changes".equals(aiStatus)) { %>
             AI reviewed the CV, but there were no empty profile fields to update.
            <% } else if ("no_text".equals(aiStatus)) { %>
             AI prefill was skipped because we could not extract readable text from the file.
            <% } else if ("disabled".equals(aiStatus)) { %>
             AI prefill is currently unavailable. Configure Admin &rarr; AI API or set <code>TA_AI_API_KEY</code> in <code>ai.env</code> and restart with <code>run-with-ai.ps1</code>.
            <% } else if ("failed".equals(aiStatus)) { %>
             AI prefill was skipped because the file could not be processed automatically.
            <% } %>
            </p>
            <% } %>
            <% if ("no_file".equals(request.getParameter("error"))) { %><p class="error">Please select a file to upload.</p><% } %>
            <% if ("invalid_type".equals(request.getParameter("error"))) { %><p class="error">Invalid file type. Use <%= escHtml(bupt.ta.cv.ResumeTextExtractor.supportedExtensionsDisplay()) %>.</p><% } %>
            <% if (request.getAttribute("errorMessage") != null) { %><p class="error"><%= escHtml((String) request.getAttribute("errorMessage")) %></p><% } %>
            </div>
            <% } %>

            <form action="${pageContext.request.contextPath}/ta/profile" method="post" class="form form--ta ta-profile-form">
                <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                <% if (returnUrlAttr != null && !returnUrlAttr.isEmpty()) { %>
                <input type="hidden" name="returnUrl" value="<%= escHtml(returnUrlAttr) %>">
                <% } %>
                <div class="ta-profile-form__body">
                    <section class="ta-profile-section" aria-labelledby="ta-prof-h-contact">
                        <h2 id="ta-prof-h-contact" class="ta-profile-section__title">Contact &amp; identity</h2>
                        <div class="ta-profile-grid ta-profile-grid--contact">
                            <div class="ta-profile-field ta-profile-field--sid">
                                <label for="ta-prof-studentId">Student ID</label>
                                <input id="ta-prof-studentId" type="text" name="studentId" required value="<%= escHtml(profile.getStudentId() != null ? profile.getStudentId() : "") %>">
                            </div>
                            <div id="ta-profile-email" class="ta-profile-field ta-profile-field--mail ta-profile-email-block">
                                <label for="ta-profile-email-input">Contact email <span class="muted-inline">(shown to module organisers)</span></label>
                                <input id="ta-profile-email-input" type="email" name="email" required autocomplete="email" value="<%= escHtml(emailValue) %>" placeholder="name@university.edu">
                            </div>
                            <div class="ta-profile-field ta-profile-field--phone">
                                <label for="ta-prof-phone">Phone</label>
                                <input id="ta-prof-phone" type="tel" name="phone" required value="<%= escHtml(profile.getPhone() != null ? profile.getPhone() : "") %>">
                            </div>
                        </div>
                    </section>

                    <section class="ta-profile-section" aria-labelledby="ta-prof-h-academic">
                        <h2 id="ta-prof-h-academic" class="ta-profile-section__title">Academic background</h2>
                        <div class="ta-profile-grid ta-profile-grid--3">
                            <div class="ta-profile-field">
                                <label for="ta-prof-degree">Degree</label>
                                <input id="ta-prof-degree" type="text" name="degree" required value="<%= escHtml(profile.getDegree() != null ? profile.getDegree() : "") %>" placeholder="e.g. BSc, MSc">
                            </div>
                            <div class="ta-profile-field">
                                <label for="ta-prof-programme">Programme / major</label>
                                <input id="ta-prof-programme" type="text" name="programme" required value="<%= escHtml(profile.getProgramme() != null ? profile.getProgramme() : "") %>" placeholder="e.g. Computer Science">
                            </div>
                            <div class="ta-profile-field">
                                <label for="ta-prof-year">Year of study</label>
                                <input id="ta-prof-year" type="text" name="yearOfStudy" required value="<%= escHtml(profile.getYearOfStudy() != null ? profile.getYearOfStudy() : "") %>" placeholder="e.g. Year 2">
                            </div>
                        </div>
                    </section>

                    <section class="ta-profile-section" aria-labelledby="ta-prof-h-exp">
                        <h2 id="ta-prof-h-exp" class="ta-profile-section__title">Experience &amp; availability</h2>
                        <div class="ta-profile-field ta-profile-field--block">
                            <label for="ta-prof-exp">Previous TA or teaching experience</label>
                            <textarea id="ta-prof-exp" name="taExperience" required rows="4" placeholder="Prior TA roles, tutoring, labs, etc. If none, write 'None'."><%= escHtml(profile.getTaExperience() != null ? profile.getTaExperience() : "") %></textarea>
                        </div>
                        <div class="ta-profile-grid ta-profile-grid--2">
                            <div class="ta-profile-field">
                                <label for="ta-prof-skills">Skills <span class="muted-inline">(comma-separated, e.g. Java, Python)</span></label>
                                <input id="ta-prof-skills" type="text" name="skills" required value="<%= escHtml((profile.getSkills() != null && !profile.getSkills().isEmpty()) ? String.join(", ", profile.getSkills()) : "") %>">
                            </div>
                            <div class="ta-profile-field">
                                <label for="ta-prof-availability">Availability</label>
                                <input id="ta-prof-availability" type="text" name="availability" required value="<%= escHtml(profile.getAvailability() != null ? profile.getAvailability() : "") %>" placeholder="e.g. Mon/Wed/Fri 9-12">
                            </div>
                        </div>
                    </section>

                    <section class="ta-profile-section" aria-labelledby="ta-prof-h-intro">
                        <h2 id="ta-prof-h-intro" class="ta-profile-section__title">Introduction</h2>
                        <div class="ta-profile-field ta-profile-field--block">
                            <label for="ta-prof-intro">About you</label>
                            <textarea id="ta-prof-intro" name="introduction" required rows="5" placeholder="A short paragraph for module organisers."><%= escHtml(profile.getIntroduction() != null ? profile.getIntroduction() : "") %></textarea>
                        </div>
                    </section>

                    <div class="ta-profile-form__actions">
                        <button type="submit">Save Profile</button>
                    </div>
                </div>
            </form>

            <section class="ta-cv-section" aria-labelledby="ta-cv-heading">
            <h2 id="ta-cv-heading">Upload CV</h2>
            <div class="ta-cv-panel">
            <% if (profile.getCvFilePath() != null && !profile.getCvFilePath().isEmpty()) { %>
            <p class="success ta-cv-status">Current CV on file.
                <a href="${pageContext.request.contextPath}/view-cv?userId=<%= profile.getUserId() %>" target="_blank" rel="noopener">View</a>
                <span class="muted-inline"> | </span>
                <a href="${pageContext.request.contextPath}/view-cv?userId=<%= profile.getUserId() %>&amp;download=1">Download</a>
            </p>
            <% } %>
            <form action="${pageContext.request.contextPath}/ta/upload-cv" method="post" enctype="multipart/form-data" class="form form--ta form--ta-cv">
                <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
                <label>Select file (<%= escHtml(bupt.ta.cv.ResumeTextExtractor.supportedExtensionsDisplay()) %>, max 5MB)</label>
                <input type="file" name="cvFile" accept=".pdf,.doc,.docx,.txt">
                <button type="submit">Upload CV</button>
            </form>
            </div>
            </section>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card">
                <div class="widget-title">Profile health</div>
                <p class="widget-line">Complete every required field before saving.</p>
                <p class="widget-line">Add detailed skills for better AI ranking.</p>
            </div>
            <div class="widget-card ta-widget-card">
                <div class="widget-title">CV reminder</div>
                <p class="widget-line">Accepted: <%= escHtml(bupt.ta.cv.ResumeTextExtractor.supportedExtensionsDisplay()) %></p>
                <p class="widget-line">Max upload size: 5MB</p>
            </div>
        </aside>
    </div>
</div>
<script>
document.addEventListener("DOMContentLoaded", function () {
    var messages = {
        valueMissing: "Please fill out this field.",
        typeMismatch: "Please enter a valid value.",
        patternMismatch: "Please match the requested format.",
        tooShort: "Please lengthen this text.",
        tooLong: "Please shorten this text.",
        rangeUnderflow: "Value is too small.",
        rangeOverflow: "Value is too large.",
        stepMismatch: "Please enter a valid value.",
        badInput: "Please enter a valid value."
    };

    document.querySelectorAll("form").forEach(function (form) {
        var controls = form.querySelectorAll("input, textarea, select");
        controls.forEach(function (control) {
            control.addEventListener("invalid", function () {
                control.setCustomValidity("");
                for (var key in messages) {
                    if (control.validity[key]) {
                        control.setCustomValidity(messages[key]);
                        break;
                    }
                }
            });
            control.addEventListener("input", function () { control.setCustomValidity(""); });
            control.addEventListener("change", function () { control.setCustomValidity(""); });
        });
    });
});
</script>
</body>
</html>
