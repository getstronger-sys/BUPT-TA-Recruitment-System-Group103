package bupt.ta.servlet;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.TAProfile;
import bupt.ta.model.User;
import bupt.ta.service.EmailNotificationService;
import bupt.ta.service.EmailOtpService;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.PasswordHasher;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * TA self-registration with email OTP verification when enabled in admin settings.
 */
public class RegisterServlet extends HttpServlet {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String OTP_PURPOSE_REGISTER = "REGISTER";
    private static final EmailNotificationService EMAIL_SERVICE = new EmailNotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AdminSettings adminSettings = storage.loadAdminSettings();
        req.setAttribute("otpRequired", isOtpRequired(adminSettings));
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = trim(req.getParameter("action"));
        String username = trim(req.getParameter("username"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String role = trim(req.getParameter("role"));
        String email = trim(req.getParameter("email"));
        String emailOtp = trim(req.getParameter("emailOtp"));
        String studentId = trim(req.getParameter("studentId"));
        String realName = trim(req.getParameter("realName"));
        String error = null;
        DataStorage storage = new DataStorage(getServletContext());
        AdminSettings adminSettings = storage.loadAdminSettings();
        boolean otpRequired = isOtpRequired(adminSettings);
        req.setAttribute("otpRequired", otpRequired);

        // Send OTP flow (does not create accounts).
        if ("sendOtp".equals(action)) {
            if (!otpRequired) {
                req.setAttribute("success", "Email verification is currently disabled by admin settings.");
                forwardWithFormState(req, resp, username, role, email, emailOtp, studentId, realName, null);
                return;
            }
            if (email.isEmpty()) {
                error = "Email is required.";
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                error = "Please enter a valid email address.";
            } else {
                if (storage.findByEmail(email) != null) {
                    error = "Email already exists.";
                } else {
                    EmailOtpService otpService = new EmailOtpService();
                    EmailOtpService.OtpRequestResult result =
                            otpService.requestOtp(storage, email, OTP_PURPOSE_REGISTER, adminSettings);
                    if (result.isSuccess()) {
                        req.setAttribute("success", "Verification code sent. Please check your inbox.");
                    } else {
                        error = result.getDetail();
                    }
                }
            }
            forwardWithFormState(req, resp, username, role, email, emailOtp, studentId, realName, error);
            return;
        }

        if (username.isEmpty()) {
            error = "Username is required.";
        } else if (!"TA".equals(role) && !"MO".equals(role)) {
            error = "Please select a valid role (TA or MO).";
        } else if ("TA".equals(role) && studentId.isEmpty()) {
            error = "Student ID is required for applicant accounts.";
        } else if (email.isEmpty()) {
            error = "Email is required.";
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            error = "Please enter a valid email address.";
        } else if (password == null || password.length() < 4) {
            error = "Password must be at least 4 characters.";
        } else if (!password.equals(confirmPassword)) {
            error = "Passwords do not match.";
        } else {
            if (storage.findByUsername(username) != null) {
                error = "Username already exists.";
            } else if (storage.findByEmail(email) != null) {
                error = "Email already exists.";
            } else if ("TA".equals(role) && storage.findProfileByStudentId(studentId) != null) {
                error = "Student ID already exists.";
            } else {
                if (otpRequired) {
                    if (emailOtp.isEmpty()) {
                        error = "Email verification code is required.";
                    } else {
                        EmailOtpService otpService = new EmailOtpService();
                        EmailOtpService.OtpVerifyResult verify = otpService.verifyOtp(storage, email, OTP_PURPOSE_REGISTER, emailOtp);
                        if (!verify.isSuccess()) {
                            error = "Email verification failed: " + verify.getDetail();
                        }
                    }
                }
                if (error == null) {
                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(PasswordHasher.hash(password));
                    user.setRole(role);
                    user.setEmail(email);
                    user.setStudentId(studentId);
                    user.setRealName(realName.isEmpty() ? username : realName);
                    user = storage.addUser(user);

                    if ("TA".equals(role)) {
                        TAProfile profile = new TAProfile(user.getId());
                        profile.setStudentId(studentId);
                        storage.saveProfile(profile);
                    }

                    resp.sendRedirect(req.getContextPath() + "/index.jsp?registered=1");
                    return;
                }
            }
        }
        forwardWithFormState(req, resp, username, role, email, emailOtp, studentId, realName, error);
    }

    private void forwardWithFormState(HttpServletRequest req, HttpServletResponse resp,
                                      String username, String role, String email, String emailOtp,
                                      String studentId, String realName, String error) throws ServletException, IOException {
        req.setAttribute("username", username);
        req.setAttribute("role", role);
        req.setAttribute("email", email);
        req.setAttribute("emailOtp", emailOtp);
        req.setAttribute("studentId", studentId);
        req.setAttribute("realName", realName);
        req.setAttribute("error", error);
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isOtpRequired(AdminSettings adminSettings) {
        if (adminSettings == null) {
            return false;
        }
        // Only require OTP when admin enables email delivery AND SMTP is configured (so we can actually send codes).
        return adminSettings.isMailEnabled() && EMAIL_SERVICE.isConfigured(adminSettings);
    }
}
