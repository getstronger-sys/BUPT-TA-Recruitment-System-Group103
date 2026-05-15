package bupt.ta.servlet;

import bupt.ta.model.AssignedModule;
import bupt.ta.model.Job;
import bupt.ta.model.WorkArrangementItem;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.InterviewScheduleSupport;
import bupt.ta.util.PaymentSupport;
import bupt.ta.util.WorkArrangementSupport;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates or updates an MO job posting, including structured work arrangements.
 */
public class PostJobServlet extends HttpServlet {

    private static final int MIN_RESPONSIBILITIES_LEN = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String postedBy = (String) req.getSession().getAttribute("userId");
        DataStorage storage = new DataStorage(getServletContext());
        List<AssignedModule> assignedModules = storage.loadAssignedModulesForMo(postedBy);
        req.setAttribute("assignedModules", assignedModules);
        req.getRequestDispatcher("/mo/post-job.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String title = trim(req.getParameter("title"));
        String moduleCode = trim(req.getParameter("moduleCode"));
        String moduleName = trim(req.getParameter("moduleName"));
        String description = trim(req.getParameter("description"));
        String responsibilities = trim(req.getParameter("responsibilities"));
        String paymentAmount = trim(req.getParameter("paymentAmount"));
        String paymentCurrency = trim(req.getParameter("paymentCurrency"));
        String paymentRateType = trim(req.getParameter("paymentRateType"));
        String payment = "";
        String deadline = trim(req.getParameter("deadline"));
        String examTimeline = trim(req.getParameter("examTimeline"));
        String interviewDate = trim(req.getParameter("interviewDate"));
        String interviewStartTime = trim(req.getParameter("interviewStartTime"));
        String interviewEndTime = trim(req.getParameter("interviewEndTime"));
        String interviewSchedule = "";
        String interviewLocation = trim(req.getParameter("interviewLocation"));
        String skillsStr = req.getParameter("skills");
        String maxApplicantsStr = req.getParameter("maxApplicants");
        String plannedTaCountStr = req.getParameter("plannedTaCount");
        String jobType = req.getParameter("jobType");
        String postedBy = (String) req.getSession().getAttribute("userId");
        String postedByName = (String) req.getSession().getAttribute("realName");
        DataStorage storage = new DataStorage(getServletContext());
        List<AssignedModule> assignedModules = storage.loadAssignedModulesForMo(postedBy);

        if (postedByName == null) {
            postedByName = (String) req.getSession().getAttribute("username");
        }

        List<String> skills = skillsStr != null && !skillsStr.trim().isEmpty()
                ? Arrays.stream(skillsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : Arrays.asList();

        int maxApplicants = 0;
        try {
            if (maxApplicantsStr != null && !maxApplicantsStr.trim().isEmpty()) {
                maxApplicants = Integer.parseInt(maxApplicantsStr.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        int plannedTaCount = 0;
        try {
            if (plannedTaCountStr != null && !plannedTaCountStr.trim().isEmpty()) {
                plannedTaCount = Integer.parseInt(plannedTaCountStr.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        List<WorkArrangementItem> workRows = WorkArrangementSupport.parseWorkRowsFromRequest(req);
        String error = WorkArrangementSupport.validateWorkRowsForPosting(workRows);
        if (error == null) {
            try {
                interviewSchedule = InterviewScheduleSupport.normalizeFromForm(
                        interviewDate, interviewStartTime, interviewEndTime);
            } catch (IllegalArgumentException ex) {
                error = ex.getMessage();
            }
        }
        if (error == null) {
            try {
                payment = PaymentSupport.normalizeFromForm(paymentAmount, paymentCurrency, paymentRateType);
            } catch (IllegalArgumentException ex) {
                error = ex.getMessage();
            }
        }
        if (error == null) {
            error = validateJobForm(title, moduleCode, moduleName, responsibilities, payment, deadline, examTimeline,
                    interviewSchedule, interviewLocation, skills, maxApplicants, plannedTaCount);
        }

        if (error != null) {
            repopulateForm(req, title, moduleCode, moduleName, description, responsibilities, payment, deadline,
                    examTimeline, interviewSchedule, interviewLocation, skillsStr, maxApplicantsStr, jobType, workRows,
                    req.getParameter("autoFillFromWaitlist") != null, plannedTaCountStr);
            req.setAttribute("assignedModules", assignedModules);
            req.setAttribute("error", error);
            req.getRequestDispatcher("/mo/post-job.jsp").forward(req, resp);
            return;
        }

        Job job = new Job();
        job.setTitle(title);
        job.setModuleCode(moduleCode.toUpperCase());
        job.setModuleName(moduleName);
        job.setDescription(description);
        job.setResponsibilities(responsibilities);
        WorkArrangementSupport.applyDerivedFields(job, workRows);
        job.setTaSlots(plannedTaCount);
        job.setPayment(payment);
        job.setDeadline(deadline);
        job.setExamTimeline(examTimeline);
        job.setInterviewSchedule(interviewSchedule);
        job.setInterviewLocation(interviewLocation);
        job.setRequiredSkills(skills);
        job.setPostedBy(postedBy);
        job.setPostedByName(postedByName != null ? postedByName : "MO");
        job.setMaxApplicants(maxApplicants);
        job.setJobType(jobType != null && !jobType.isEmpty() ? jobType : "MODULE_TA");
        job.setAutoFillFromWaitlist(req.getParameter("autoFillFromWaitlist") != null);

        String assignmentError = validateAssignedModule(moduleCode, moduleName, assignedModules);
        if (assignmentError != null) {
            repopulateForm(req, title, moduleCode, moduleName, description, responsibilities, payment, deadline,
                    examTimeline, interviewSchedule, interviewLocation, skillsStr, maxApplicantsStr, jobType, workRows,
                    req.getParameter("autoFillFromWaitlist") != null, plannedTaCountStr);
            req.setAttribute("assignedModules", assignedModules);
            req.setAttribute("error", assignmentError);
            req.getRequestDispatcher("/mo/post-job.jsp").forward(req, resp);
            return;
        }
        storage.addJob(job);
        resp.sendRedirect(req.getContextPath() + "/mo/job?posted=1&jobId="
                + java.net.URLEncoder.encode(job.getId(), java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String validateJobForm(String title, String moduleCode, String moduleName,
                                          String responsibilities, String payment, String deadline, String examTimeline,
                                          String interviewSchedule, String interviewLocation,
                                          List<String> skills, int maxApplicants, int plannedTaCount) {
        if (title.isEmpty()) {
            return "Job title is required.";
        }
        if (moduleCode.isEmpty()) {
            return "Module code is required.";
        }
        if (moduleName.isEmpty()) {
            return "Module name is required.";
        }
        if (responsibilities.length() < MIN_RESPONSIBILITIES_LEN) {
            return "Responsibilities must be at least " + MIN_RESPONSIBILITIES_LEN + " characters.";
        }
        if (payment.isEmpty()) {
            return "Payment / compensation is required.";
        }
        if (deadline.isEmpty()) {
            return "Application deadline is required.";
        }
        try {
            LocalDate d = LocalDate.parse(deadline);
            if (d.isBefore(LocalDate.now())) {
                return "Application deadline must be today or a future date.";
            }
        } catch (DateTimeParseException e) {
            return "Deadline must be a valid date (YYYY-MM-DD).";
        }
        if (skills.isEmpty()) {
            return "At least one required skill is needed (comma-separated).";
        }
        if (maxApplicants < 0) {
            return "Max applicants cannot be negative.";
        }
        if (plannedTaCount < 1) {
            return "Planned recruits must be at least 1.";
        }
        if (examTimeline.isEmpty()) {
            return "Course timeline / exam milestones are required.";
        }
        if (interviewSchedule.isEmpty()) {
            return "Interview schedule is required.";
        }
        if (interviewLocation.isEmpty()) {
            return "Interview location is required.";
        }
        return null;
    }

    private static void repopulateForm(HttpServletRequest req, String title, String moduleCode, String moduleName,
                                       String description, String responsibilities, String payment, String deadline,
                                       String examTimeline, String interviewSchedule, String interviewLocation,
                                       String skillsStr, String maxApplicantsStr, String jobType,
                                       List<WorkArrangementItem> workRows, boolean autoFillFromWaitlist,
                                       String plannedTaCountStr) {
        req.setAttribute("fvTitle", title);
        req.setAttribute("fvModuleCode", moduleCode);
        req.setAttribute("fvModuleName", moduleName);
        req.setAttribute("fvDescription", description);
        req.setAttribute("fvResponsibilities", responsibilities);
        req.setAttribute("fvPayment", payment);
        req.setAttribute("fvDeadline", deadline);
        req.setAttribute("fvExamTimeline", examTimeline);
        req.setAttribute("fvInterviewSchedule", interviewSchedule);
        req.setAttribute("fvInterviewLocation", interviewLocation);
        req.setAttribute("fvSkills", skillsStr != null ? skillsStr : "");
        req.setAttribute("fvMaxApplicants", maxApplicantsStr != null ? maxApplicantsStr : "0");
        req.setAttribute("fvJobType", jobType != null ? jobType : "MODULE_TA");
        req.setAttribute("fvWorkArrangements", workRows != null ? workRows : new ArrayList<>());
        req.setAttribute("fvAutoFillFromWaitlist", autoFillFromWaitlist);
        req.setAttribute("fvPlannedTaCount", plannedTaCountStr != null ? plannedTaCountStr : "");
    }

    private static String validateAssignedModule(String moduleCode, String moduleName, List<AssignedModule> assignedModules) {
        if (assignedModules == null || assignedModules.isEmpty()) {
            return "No modules are assigned to your account for this term. Please ask admin to assign courses first.";
        }
        String code = moduleCode != null ? moduleCode.trim().toUpperCase() : "";
        AssignedModule matched = assignedModules.stream()
                .filter(m -> m != null && m.getModuleCode() != null && code.equalsIgnoreCase(m.getModuleCode().trim()))
                .findFirst()
                .orElse(null);
        if (matched == null) {
            String allowedCodes = assignedModules.stream()
                    .filter(m -> m != null && m.getModuleCode() != null)
                    .map(m -> m.getModuleCode().trim().toUpperCase())
                    .distinct()
                    .collect(Collectors.joining(", "));
            return "You can only post jobs for modules assigned by admin. Assigned module codes: " + allowedCodes;
        }
        String assignedName = matched.getModuleName() != null ? matched.getModuleName().trim() : "";
        String providedName = moduleName != null ? moduleName.trim() : "";
        if (!assignedName.isEmpty() && !providedName.isEmpty() && !assignedName.equalsIgnoreCase(providedName)) {
            return "Module name does not match the admin assignment for " + code + ". Expected: " + assignedName;
        }
        return null;
    }
}
