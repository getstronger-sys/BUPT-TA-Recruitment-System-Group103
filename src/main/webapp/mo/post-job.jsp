<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jspf/html-esc.jspf" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="bupt.ta.model.AssignedModule" %>
<%@ page import="bupt.ta.model.JobTemplate" %>
<%@ page import="bupt.ta.model.WorkArrangementItem" %>
<%@ page import="bupt.ta.util.InterviewScheduleSupport" %>
<%@ page import="bupt.ta.util.PaymentSupport" %>
<%@ page import="bupt.ta.util.WorkArrangementSupport" %>
<%!
    static String fv(javax.servlet.http.HttpServletRequest r, String key, String def) {
        Object o = r.getAttribute(key);
        return o != null ? (String) o : (def != null ? def : "");
    }
    static String fva(javax.servlet.http.HttpServletRequest r, String key) {
        return escHtml(fv(r, key, ""));
    }
%>
<% List<JobTemplate> jobTemplates = (List<JobTemplate>) request.getAttribute("jobTemplates");
   if (jobTemplates == null) jobTemplates = java.util.Collections.emptyList();
   String selectedTemplateId = request.getAttribute("selectedTemplateId") != null ? request.getAttribute("selectedTemplateId").toString() : "";
   boolean fvAutoFill = Boolean.TRUE.equals(request.getAttribute("fvAutoFillFromWaitlist"));
   @SuppressWarnings("unchecked")
   List<WorkArrangementItem> waRows = (List<WorkArrangementItem>) request.getAttribute("fvWorkArrangements");
   if (waRows == null || waRows.isEmpty()) {
       waRows = new ArrayList<>();
       waRows.add(new WorkArrangementItem("", "", 1, 1, ""));
   }
   int waDefaultPlannedTaCount = waRows.stream().mapToInt(WorkArrangementItem::getTaCount).sum();
   if (waDefaultPlannedTaCount < 1) waDefaultPlannedTaCount = 1;
   String fvPlannedTaCount = fv(request, "fvPlannedTaCount", "");
   String fvInterviewSchedule = fv(request, "fvInterviewSchedule", "");
   String fvInterviewDate = InterviewScheduleSupport.dateInputValue(fvInterviewSchedule);
   String fvInterviewStart = InterviewScheduleSupport.startTimeInputValue(fvInterviewSchedule);
   String fvInterviewEnd = InterviewScheduleSupport.endTimeInputValue(fvInterviewSchedule);
   String fvPayment = fv(request, "fvPayment", "");
   String fvPaymentAmount = PaymentSupport.amountInputValue(fvPayment);
   String fvPaymentCurrency = PaymentSupport.currencyInputValue(fvPayment);
   String fvPaymentRateType = PaymentSupport.rateTypeInputValue(fvPayment);
   String[] paymentCurrencies = PaymentSupport.currencies();
   String[] paymentRateTypes = PaymentSupport.rateTypes();
   List<AssignedModule> assignedModules = (List<AssignedModule>) request.getAttribute("assignedModules");
   if (assignedModules == null) assignedModules = java.util.Collections.emptyList();
   String[] weekdays = WorkArrangementSupport.weekdays();
   request.setAttribute("moNavActive", "post");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <%@ include file="/WEB-INF/jspf/viewport.jspf" %>
    <title>Post Job - MO</title>
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
                <div class="icon-dot">J</div>
                <div class="icon-dot active">P</div>
                <div class="icon-dot">D</div>
            </div>
            <%@ include file="/WEB-INF/jspf/mo-side-nav.jspf" %>
        </div>
        <main class="main-panel mo-main mo-page mo-page--mo-post">
            <header class="ta-page-header">
                <p class="ta-page-kicker">New posting</p>
                <h1>Post a New Job</h1>
                <p class="ta-page-lead">Publish a vacancy for an admin-assigned module. Use section links below to jump the form; save as a <strong>template</strong> to reuse later.</p>
            </header>
            <% String err = (String) request.getAttribute("error"); if (err != null) { %>
            <div class="ta-page-flashes">
                <p class="error"><%= escHtml(err) %></p>
            </div>
            <% } %>
            <nav class="mo-post-anchor-nav" aria-label="Post job section shortcuts">
                <a href="#post-basic">Basic info</a>
                <a href="#post-work-arrangements">Work arrangements</a>
                <a href="#post-recruitment">Recruitment setup</a>
                <a href="#post-submit">Submit</a>
            </nav>
            <form action="${pageContext.request.contextPath}/mo/post-job" method="post" class="form form--mo-post">
        <%@ include file="/WEB-INF/jspf/csrf-hidden.jspf" %>
        <div id="post-basic" class="mo-post-section-anchor"></div>
        <h2 class="mo-post-section-title">Basic information</h2>
        <% if (assignedModules.isEmpty()) { %>
        <p class="error">No modules are assigned to your account for this term. Please contact admin before posting jobs.</p>
        <% } else { %>
        <label>Assigned modules this term</label>
        <select id="assigned-module-picker">
            <option value="">Choose assigned module to fill code/name</option>
            <% for (AssignedModule am : assignedModules) {
                   if (am == null || am.getModuleCode() == null || am.getModuleCode().trim().isEmpty()) continue;
                   String code = am.getModuleCode().trim().toUpperCase();
                   String name = am.getModuleName() != null ? am.getModuleName().trim() : "";
            %>
            <option value="<%= escHtml(code) %>" data-module-name="<%= escHtml(name) %>"><%= escHtml(code) %><%= name.isEmpty() ? "" : " - " + escHtml(name) %></option>
            <% } %>
        </select>
        <p class="muted-inline">MO can post jobs only for admin-assigned module codes.</p>
        <% } %>
        <label>Job Title *</label>
        <input type="text" name="title" required placeholder="e.g. TA for Software Engineering" value="<%= fva(request, "fvTitle") %>">
        <label>Module Code *</label>
        <input type="text" name="moduleCode" required placeholder="e.g. EBU6304" value="<%= fva(request, "fvModuleCode") %>">
        <label>Module Name *</label>
        <input type="text" name="moduleName" required placeholder="e.g. Software Engineering" value="<%= fva(request, "fvModuleName") %>">
        <label>Job Type</label>
        <select name="jobType">
            <% String jt = fv(request, "fvJobType", "MODULE_TA");
               if (jt.isEmpty()) jt = "MODULE_TA"; %>
            <option value="MODULE_TA" <%= "MODULE_TA".equals(jt) ? "selected" : "" %>>Module TA</option>
            <option value="INVIGILATION" <%= "INVIGILATION".equals(jt) ? "selected" : "" %>>Invigilation</option>
            <option value="OTHER" <%= "OTHER".equals(jt) ? "selected" : "" %>>Other</option>
        </select>
        <label>Description (overview)</label>
        <textarea name="description" placeholder="Short overview for the listing..."><%= fva(request, "fvDescription") %></textarea>
        <label>Responsibilities * <span class="muted-inline">(min 20 characters)</span></label>
        <textarea name="responsibilities" required minlength="20" rows="5" placeholder="What the TA will do: labs, marking, office hours..."><%= fva(request, "fvResponsibilities") %></textarea>

        <div id="post-work-arrangements" class="mo-post-section-anchor"></div>
        <h2 class="mo-post-section-title">Work arrangements</h2>
        <div class="wa-section mo-wa-card">
            <div class="mo-wa-table-scroll">
            <table class="wa-edit-table" role="grid" aria-label="Work arrangement rows">
                <caption class="sr-only">Work arrangement rows, required fields per column header</caption>
                <thead>
                <tr>
                    <th scope="col" class="wa-edit-table__col-name">Work name</th>
                    <th scope="col" class="wa-edit-table__col-duration">Per-session duration (hours)</th>
                    <th scope="col" class="wa-edit-table__col-num">Sessions</th>
                    <th scope="col" class="wa-edit-table__col-num">TAs</th>
                    <th scope="col" class="wa-edit-table__col-time">Specific time <span class="muted-inline">(optional)</span></th>
                    <th scope="col" class="wa-edit-table__col-actions"><span class="sr-only">Remove row</span></th>
                </tr>
                </thead>
                <tbody id="wa-rows" class="wa-rows wa-rows--edit">
                <% for (int waIdx = 0; waIdx < waRows.size(); waIdx++) {
                       WorkArrangementItem w = waRows.get(waIdx);
                       String wid = "wa-" + waIdx;
                       String wn = w.getWorkName() != null ? w.getWorkName() : "";
                       String sd = WorkArrangementSupport.durationHoursInputValue(w);
                       int oc = w.getOccurrenceCount() > 0 ? w.getOccurrenceCount() : 1;
                       int wc = w.getTaCount() > 0 ? w.getTaCount() : 1;
                       String wt = w.getSpecificTime() != null ? w.getSpecificTime() : "";
                       String wtDay = WorkArrangementSupport.specificDayInputValue(wt);
                       String wtClock = WorkArrangementSupport.specificTimeInputValue(wt);
                %>
                <tr class="wa-row" data-wa-row>
                    <td class="wa-edit-table__cell">
                        <label class="sr-only" for="<%= wid %>-wn">Work name</label>
                        <input id="<%= wid %>-wn" type="text" name="waWorkName" required value="<%= escHtml(wn) %>" placeholder="e.g. Lab" autocomplete="off" class="wa-edit-table__input">
                    </td>
                    <td class="wa-edit-table__cell">
                        <label class="sr-only" for="<%= wid %>-sd">Per-session duration</label>
                        <input id="<%= wid %>-sd" type="number" name="waSessionDuration" min="0.25" step="0.25" required value="<%= escHtml(sd) %>" placeholder="2" inputmode="decimal" autocomplete="off" class="wa-edit-table__input">
                    </td>
                    <td class="wa-edit-table__cell wa-edit-table__cell--num">
                        <label class="sr-only" for="<%= wid %>-oc">Sessions</label>
                        <input id="<%= wid %>-oc" type="number" name="waOccurrenceCount" min="1" required value="<%= oc %>" class="wa-input-num wa-edit-table__input">
                    </td>
                    <td class="wa-edit-table__cell wa-edit-table__cell--num">
                        <label class="sr-only" for="<%= wid %>-tc">TAs</label>
                        <input id="<%= wid %>-tc" type="number" name="waTaCount" min="1" required value="<%= wc %>" class="wa-input-num wa-edit-table__input">
                    </td>
                    <td class="wa-edit-table__cell">
                        <label class="sr-only" for="<%= wid %>-st">Specific time</label>
                        <div class="wa-time-controls">
                            <select id="<%= wid %>-day" name="waSpecificDay" class="wa-edit-table__input">
                                <option value="">TBD</option>
                                <% for (String day : weekdays) { %>
                                <option value="<%= escHtml(day) %>" <%= day.equals(wtDay) ? "selected" : "" %>><%= escHtml(day) %></option>
                                <% } %>
                            </select>
                            <input id="<%= wid %>-st" type="time" name="waSpecificStartTime" value="<%= escHtml(wtClock) %>" autocomplete="off" class="wa-edit-table__input">
                        </div>
                    </td>
                    <td class="wa-edit-table__cell wa-edit-table__cell--actions">
                        <button type="button" class="btn btn-secondary wa-remove-compact" title="Remove row" aria-label="Remove this row">&minus;</button>
                    </td>
                </tr>
                <% } %>
                </tbody>
            </table>
            </div>
            <div class="wa-toolbar">
                <button type="button" id="wa-add-row" class="btn btn-secondary" title="Add work arrangement row">+ Add row</button>
                <button type="button" id="wa-suggest-quota" class="btn btn-secondary" title="Suggest balanced TA quotas">Smart quota recommendation</button>
            </div>
            <div id="wa-suggestion-panel" class="wa-suggestion-panel" hidden>
                <p id="wa-suggestion-meta" class="muted-inline wa-suggestion-meta"></p>
                <div id="wa-suggestion-list" class="wa-suggestion-list"></div>
            </div>
        </div>
        <div class="mo-wa-planned-block">
        <label for="post-planned-ta">Planned recruits *</label>
        <input type="number" name="plannedTaCount" id="post-planned-ta" min="1" required value="<%= fvPlannedTaCount.isEmpty() ? String.valueOf(waDefaultPlannedTaCount) : escHtml(fvPlannedTaCount) %>">
        </div>

        <div id="post-recruitment" class="mo-post-section-anchor"></div>
        <h2 class="mo-post-section-title">Recruitment setup</h2>
        <label>Course timeline &amp; exam milestones *</label>
        <textarea name="examTimeline" required rows="4" placeholder="Week 1-3 onboarding; Week 4 quiz support; Week 8 mock exam; Week 12 final exam marking."><%= fva(request, "fvExamTimeline") %></textarea>
        <label>Estimated interview time *</label>
        <div class="interview-time-controls">
            <label class="structured-field">Date
                <input type="date" name="interviewDate" required value="<%= escHtml(fvInterviewDate) %>" aria-label="Estimated interview date">
            </label>
            <label class="structured-field">Start time
                <input type="time" name="interviewStartTime" required value="<%= escHtml(fvInterviewStart) %>" aria-label="Estimated interview start time">
            </label>
            <label class="structured-field">End time
                <input type="time" name="interviewEndTime" required value="<%= escHtml(fvInterviewEnd) %>" aria-label="Estimated interview end time">
            </label>
        </div>
        <label>Estimated interview location *</label>
        <input type="text" name="interviewLocation" required placeholder="e.g. EECS Bldg Room 402 / Teams link" value="<%= fva(request, "fvInterviewLocation") %>">
        <label>Payment / compensation *</label>
        <div class="payment-controls">
            <label class="structured-field">Amount
                <input type="number" name="paymentAmount" min="0.01" step="0.01" required inputmode="decimal" value="<%= escHtml(fvPaymentAmount) %>" placeholder="15">
            </label>
            <label class="structured-field">Currency
                <select name="paymentCurrency" required>
                    <% for (String currency : paymentCurrencies) { %>
                    <option value="<%= escHtml(currency) %>" <%= currency.equals(fvPaymentCurrency) ? "selected" : "" %>><%= escHtml(currency) %></option>
                    <% } %>
                </select>
            </label>
            <label class="structured-field">Rate type
                <select name="paymentRateType" required>
                    <% for (String rateType : paymentRateTypes) { %>
                    <option value="<%= escHtml(rateType) %>" <%= rateType.equals(fvPaymentRateType) ? "selected" : "" %>><%= escHtml(rateType) %></option>
                    <% } %>
                </select>
            </label>
        </div>
        <label>Application deadline * <span class="muted-inline">(YYYY-MM-DD)</span></label>
        <input type="date" name="deadline" required value="<%= fva(request, "fvDeadline") %>">
        <label>Required Skills * <span class="muted-inline">(comma-separated)</span></label>
        <input type="text" name="skills" required placeholder="e.g. Java, Python, Teaching" value="<%= fva(request, "fvSkills") %>">
        <label>Max Applicants (0 = unlimited)</label>
        <input type="number" name="maxApplicants" min="0" value="<%= fva(request, "fvMaxApplicants").isEmpty() ? "0" : fva(request, "fvMaxApplicants") %>">
        <label class="checkbox-line"><input type="checkbox" name="autoFillFromWaitlist" <%= fvAutoFill ? "checked" : "" %>> Auto-fill from waitlist when a selected TA withdraws</label>
        <label class="checkbox-line"><input type="checkbox" name="saveAsTemplate"> Save this posting setup as a reusable template</label>
        <label>Template name <span class="muted-inline">(optional when saving)</span></label>
        <input type="text" name="templateName" placeholder="e.g. EBU6304 standard TA template" value="<%= fva(request, "fvTemplateName") %>">
        <div id="post-submit" class="mo-post-section-anchor"></div>
        <button type="submit" class="btn btn-primary">Post Job</button>
    </form>
        </main>
        <aside class="right-sidebar">
            <div class="widget-card ta-widget-card mo-post-side-card mo-post-checklist-card">
                <div class="widget-title">Posting checklist</div>
                <ul class="mo-side-list">
                    <li>Fields marked * are required.</li>
                    <li>Deadline must be YYYY-MM-DD and not in the past.</li>
                    <li>Responsibilities must be at least 20 characters.</li>
                    <li>Set a fixed planned recruits count.</li>
                </ul>
            </div>
            <div class="widget-card ta-widget-card mo-post-side-card mo-post-template-card">
                <div class="widget-title">Reusable templates</div>
                <ul class="mo-side-list">
                    <li>Load a saved template to prefill fields.</li>
                    <li>Adjust only what changed.</li>
                </ul>
                <form action="${pageContext.request.contextPath}/mo/post-job" method="get" class="search-form search-form--mo-template">
                    <select name="templateId">
                        <option value="">Choose a saved template</option>
                        <% for (JobTemplate t : jobTemplates) { %>
                        <option value="<%= escHtml(t.getId()) %>" <%= t.getId().equals(selectedTemplateId) ? "selected" : "" %>><%= escHtml(t.getTemplateName()) %> - <%= escHtml(t.getModuleCode()) %></option>
                        <% } %>
                    </select>
                    <button type="submit" class="btn btn-primary">Load template</button>
                </form>
            </div>
        </aside>
    </div>
</div>
<script>
document.addEventListener("DOMContentLoaded", function () {
    var assignedPicker = document.getElementById("assigned-module-picker");
    var moduleCodeInput = document.querySelector("input[name='moduleCode']");
    var moduleNameInput = document.querySelector("input[name='moduleName']");
    if (assignedPicker && moduleCodeInput && moduleNameInput) {
        assignedPicker.addEventListener("change", function () {
            var opt = assignedPicker.options[assignedPicker.selectedIndex];
            if (!opt || !opt.value) return;
            moduleCodeInput.value = opt.value;
            var moduleName = opt.getAttribute("data-module-name") || "";
            if (moduleName) {
                moduleNameInput.value = moduleName;
            }
        });
    }

    var rowsBox = document.getElementById("wa-rows");
    var addBtn = document.getElementById("wa-add-row");
    var suggestBtn = document.getElementById("wa-suggest-quota");
    var suggestionPanel = document.getElementById("wa-suggestion-panel");
    var suggestionMeta = document.getElementById("wa-suggestion-meta");
    var suggestionList = document.getElementById("wa-suggestion-list");
    var plannedTaInput = document.querySelector("input[name='plannedTaCount']");
    if (!rowsBox || !addBtn) return;

    function clearRowInputs(row) {
        row.querySelectorAll("input").forEach(function (inp) {
            if (inp.name === "waTaCount" || inp.name === "waOccurrenceCount") {
                inp.value = "1";
            } else {
                inp.value = "";
            }
        });
        row.querySelectorAll("select").forEach(function (sel) {
            sel.selectedIndex = 0;
        });
    }

    function bindRemove(row) {
        var btn = row.querySelector(".wa-remove-compact");
        if (!btn) return;
        btn.addEventListener("click", function () {
            if (rowsBox.querySelectorAll(".wa-row").length <= 1) return;
            row.remove();
        });
    }

    rowsBox.querySelectorAll(".wa-row").forEach(bindRemove);

    function stripIdsForClone(row) {
        row.querySelectorAll("[id]").forEach(function (el) { el.removeAttribute("id"); });
        row.querySelectorAll("label[for]").forEach(function (lb) { lb.removeAttribute("for"); });
    }

    addBtn.addEventListener("click", function () {
        var first = rowsBox.querySelector(".wa-row");
        if (!first) return;
        var clone = first.cloneNode(true);
        clearRowInputs(clone);
        stripIdsForClone(clone);
        rowsBox.appendChild(clone);
        bindRemove(clone);
    });

    function parseDurationHours(raw) {
        var text = (raw || "").trim();
        if (!text) return null;
        if (!/^\d+(\.\d+)?$/.test(text) && !/^\.\d+$/.test(text)) return null;
        var value = parseFloat(text);
        if (!isFinite(value) || value <= 0) return null;
        return value;
    }

    function collectRows() {
        var rows = [];
        rowsBox.querySelectorAll(".wa-row").forEach(function (row) {
            var name = (row.querySelector("input[name='waWorkName']") || {}).value || "";
            var duration = (row.querySelector("input[name='waSessionDuration']") || {}).value || "";
            var occRaw = parseInt(((row.querySelector("input[name='waOccurrenceCount']") || {}).value || "0"), 10);
            var taRaw = parseInt(((row.querySelector("input[name='waTaCount']") || {}).value || "0"), 10);
            var occ = isFinite(occRaw) ? occRaw : 0;
            var ta = isFinite(taRaw) ? taRaw : 0;
            rows.push({
                workName: name.trim(),
                durationText: duration.trim(),
                occurrenceCount: occ,
                taCount: ta
            });
        });
        return rows;
    }

    function renderSuggestion(result) {
        if (!suggestionPanel || !suggestionMeta || !suggestionList) return;
        suggestionPanel.hidden = false;
        suggestionMeta.textContent = result.meta;
        suggestionList.innerHTML = "";
        result.assignments.forEach(function (ta) {
            var card = document.createElement("div");
            card.className = "wa-suggestion-card";
            var details = [];
            Object.keys(ta.workCount).sort().forEach(function (workName) {
                details.push(workName + " x " + ta.workCount[workName]);
            });
            card.innerHTML =
                "<strong>" + ta.name + "</strong>" +
                "<div class='wa-suggestion-hours'>Estimated load: " + ta.hours.toFixed(2) + " h</div>" +
                "<div class='wa-suggestion-work'>" + (details.length ? details.join(" | ") : "No assigned items") + "</div>";
            suggestionList.appendChild(card);
        });
    }

    if (suggestBtn) {
        suggestBtn.addEventListener("click", function () {
            var rows = collectRows();
            var units = [];
            var unknownDurationRows = 0;
            rows.forEach(function (r, idx) {
                if (!r.workName || r.occurrenceCount < 1 || r.taCount < 1) return;
                var hours = parseDurationHours(r.durationText);
                if (hours == null) {
                    unknownDurationRows += 1;
                    hours = 1;
                }
                var totalUnits = r.occurrenceCount * r.taCount;
                for (var i = 0; i < totalUnits; i++) {
                    units.push({
                        workName: r.workName,
                        hours: hours,
                        rowIndex: idx
                    });
                }
            });
            if (!units.length) {
                alert("Please complete at least one valid work arrangement row before requesting recommendations.");
                return;
            }
            var taCountRaw = parseInt((plannedTaInput && plannedTaInput.value ? plannedTaInput.value : "0"), 10);
            var taCount = isFinite(taCountRaw) ? taCountRaw : 0;
            if (taCount < 1) {
                alert("Planned recruits must be at least 1.");
                return;
            }
            var tas = [];
            for (var t = 0; t < taCount; t++) {
                tas.push({ name: "Workload share " + (t + 1), hours: 0, workCount: {} });
            }
            units.sort(function (a, b) { return b.hours - a.hours; });
            units.forEach(function (u) {
                tas.sort(function (a, b) { return a.hours - b.hours; });
                var pick = tas[0];
                pick.hours += u.hours;
                pick.workCount[u.workName] = (pick.workCount[u.workName] || 0) + 1;
            });
            var totalHours = units.reduce(function (s, u) { return s + u.hours; }, 0);
            var avg = totalHours / taCount;
            var max = Math.max.apply(null, tas.map(function (x) { return x.hours; }));
            var min = Math.min.apply(null, tas.map(function (x) { return x.hours; }));
            var meta = "Planned recruits: " + taCount + "; total estimated workload: " + totalHours.toFixed(2) + " h; average per TA: " + avg.toFixed(2) + " h; imbalance (max-min): " + (max - min).toFixed(2) + " h.";
            if (unknownDurationRows > 0) {
                meta += " " + unknownDurationRows + " row(s) used default 1h because duration text could not be parsed.";
            }
            renderSuggestion({ meta: meta, assignments: tas });
        });
    }
});
</script>
</body>
</html>
