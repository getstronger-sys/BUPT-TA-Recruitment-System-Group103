package bupt.ta.servlet;

import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;

import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TA profile view and update (skills, availability, contact details).
 */
public class TAProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = (String) req.getSession().getAttribute("userId");
        DataStorage storage = new DataStorage(getServletContext());
        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile == null) {
            profile = new TAProfile(userId);
        }
        User accountUser = storage.findUserById(userId);
        req.setAttribute("accountUser", accountUser);
        req.setAttribute("profile", profile);
        String returnUrl = req.getParameter("returnUrl");
        if (isSafeTaRelativeReturn(returnUrl)) {
            req.setAttribute("returnUrl", returnUrl);
        }
        req.getRequestDispatcher("/ta/profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = (String) req.getSession().getAttribute("userId");
        String studentId = trim(req.getParameter("studentId"));
        String email = trim(req.getParameter("email"));
        String phone = trim(req.getParameter("phone"));
        String availability = trim(req.getParameter("availability"));
        String introduction = trim(req.getParameter("introduction"));
        String degree = trim(req.getParameter("degree"));
        String programme = trim(req.getParameter("programme"));
        String yearOfStudy = trim(req.getParameter("yearOfStudy"));
        String taExperience = trim(req.getParameter("taExperience"));
        String skillsStr = trim(req.getParameter("skills"));

        List<String> skills = skillsStr != null && !skillsStr.isEmpty()
                ? Arrays.stream(skillsStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : Collections.emptyList();

        DataStorage storage = new DataStorage(getServletContext());
        TAProfile profile = storage.getProfileByUserId(userId);
        if (profile == null) {
            profile = new TAProfile(userId);
        }
        populateProfile(profile, studentId, email, phone, availability, introduction, degree, programme, yearOfStudy, taExperience, skills);

        String error = validateProfile(profile);
        if (error != null) {
            req.setAttribute("profile", profile);
            req.setAttribute("errorMessage", error);
            RequestDispatcher dispatcher = req.getRequestDispatcher("/ta/profile.jsp");
            dispatcher.forward(req, resp);
            return;
        }

        profile.setSkills(skills);

        storage.saveProfile(profile);
        User account = storage.findUserById(userId);
        if (account != null) {
            account.setEmail(profile.getEmail());
            storage.saveUser(account);
        }

        String returnUrl = req.getParameter("returnUrl");
        if (isSafeTaRelativeReturn(returnUrl)) {
            resp.sendRedirect(req.getContextPath() + returnUrl);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/ta/profile?success=1");
    }

    static void populateProfile(TAProfile profile, String studentId, String email, String phone, String availability,
                                String introduction, String degree, String programme, String yearOfStudy,
                                String taExperience, List<String> skills) {
        profile.setStudentId(studentId != null ? studentId : "");
        profile.setEmail(email != null ? email : "");
        profile.setPhone(phone != null ? phone : "");
        profile.setAvailability(availability != null ? availability : "");
        profile.setIntroduction(introduction != null ? introduction : "");
        profile.setDegree(degree != null ? degree : "");
        profile.setProgramme(programme != null ? programme : "");
        profile.setYearOfStudy(yearOfStudy != null ? yearOfStudy : "");
        profile.setTaExperience(taExperience != null ? taExperience : "");
        profile.setSkills(skills);
    }

    static String validateProfile(TAProfile profile) {
        if (isBlank(profile.getStudentId())) return "Student ID is required.";
        if (isBlank(profile.getEmail())) return "Email is required.";
        if (!isValidEmail(profile.getEmail())) {
            return "Please enter a valid email address.";
        }
        if (isBlank(profile.getPhone())) return "Phone is required.";
        if (isBlank(profile.getDegree())) return "Degree is required.";
        if (isBlank(profile.getProgramme())) return "Programme / major is required.";
        if (isBlank(profile.getYearOfStudy())) return "Year of study is required.";
        if (profile.getSkills() == null || profile.getSkills().isEmpty()) return "At least one skill is required.";
        if (isBlank(profile.getAvailability())) return "Availability is required.";
        if (isBlank(profile.getTaExperience())) return "Previous experience is required.";
        if (isBlank(profile.getIntroduction())) return "Introduction is required.";
        return null;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean isValidEmail(String raw) {
        if (raw == null) return false;
        String e = raw.trim();
        int at = e.indexOf('@');
        if (at <= 0) return false;
        int dot = e.indexOf('.', at + 1);
        return dot > at + 1 && dot < e.length() - 1;
    }

    /** Only allow in-app redirects under /ta/ (no open redirect). */
    static boolean isSafeTaRelativeReturn(String u) {
        if (u == null || u.isEmpty()) {
            return false;
        }
        if (!u.startsWith("/ta/")) {
            return false;
        }
        if (u.contains("..") || u.contains("//") || u.contains("\r") || u.contains("\n")) {
            return false;
        }
        return true;
    }
}
