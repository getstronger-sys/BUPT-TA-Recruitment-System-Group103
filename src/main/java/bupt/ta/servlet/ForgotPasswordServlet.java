package bupt.ta.servlet;

import bupt.ta.model.AdminSettings;
import bupt.ta.model.User;
import bupt.ta.service.EmailNotificationService;
import bupt.ta.service.EmailOtpService;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.PasswordHasher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Password reset flow using email OTP when enabled in admin settings.
 */
public class ForgotPasswordServlet extends HttpServlet {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String OTP_PURPOSE_RESET = "RESET_PASSWORD";
    private static final EmailNotificationService EMAIL_SERVICE = new EmailNotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AdminSettings adminSettings = storage.loadAdminSettings();
        req.setAttribute("otpRequired", isOtpRequired(adminSettings));
        req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DataStorage storage = new DataStorage(getServletContext());
        AdminSettings adminSettings = storage.loadAdminSettings();
        boolean otpRequired = isOtpRequired(adminSettings);
        req.setAttribute("otpRequired", otpRequired);

        String action = trim(req.getParameter("action"));
        String username = trim(req.getParameter("username"));
        String email = trim(req.getParameter("email"));
        String otp = trim(req.getParameter("otp"));
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        String error = null;

        if (!otpRequired) {
            req.setAttribute("success", "Password reset is currently disabled by admin email settings.");
            forward(req, resp, username, email, otp, error);
            return;
        }

        if ("sendOtp".equals(action)) {
            if (username.isEmpty()) {
                error = "Username is required.";
            } else if (email.isEmpty()) {
                error = "Email is required.";
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                error = "Please enter a valid email address.";
            } else {
                User user = storage.findByUsername(username);
                if (user == null) {
                    error = "User not found.";
                } else if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email.trim())) {
                    error = "Email does not match this username.";
                } else {
                    EmailOtpService otpService = new EmailOtpService();
                    EmailOtpService.OtpRequestResult result =
                            otpService.requestOtp(storage, email, OTP_PURPOSE_RESET, adminSettings);
                    if (result.isSuccess()) {
                        req.setAttribute("success", "Verification code sent. Please check your inbox.");
                    } else {
                        error = result.getDetail();
                    }
                }
            }
            forward(req, resp, username, email, otp, error);
            return;
        }

        if (!"resetPassword".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/forgot-password");
            return;
        }

        if (username.isEmpty()) {
            error = "Username is required.";
        } else if (email.isEmpty()) {
            error = "Email is required.";
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            error = "Please enter a valid email address.";
        } else if (otp.isEmpty()) {
            error = "Verification code is required.";
        } else if (newPassword == null || newPassword.length() < 4) {
            error = "Password must be at least 4 characters.";
        } else if (!newPassword.equals(confirmPassword)) {
            error = "Passwords do not match.";
        } else {
            User user = storage.findByUsername(username);
            if (user == null) {
                error = "User not found.";
            } else if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email.trim())) {
                error = "Email does not match this username.";
            } else {
                EmailOtpService otpService = new EmailOtpService();
                EmailOtpService.OtpVerifyResult verify = otpService.verifyOtp(storage, email, OTP_PURPOSE_RESET, otp);
                if (!verify.isSuccess()) {
                    error = "Email verification failed: " + verify.getDetail();
                } else {
                    user.setPassword(PasswordHasher.hash(newPassword));
                    storage.saveUser(user);
                    resp.sendRedirect(req.getContextPath() + "/index.jsp?reset=1");
                    return;
                }
            }
        }

        forward(req, resp, username, email, otp, error);
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp,
                         String username, String email, String otp, String error) throws ServletException, IOException {
        req.setAttribute("username", username);
        req.setAttribute("email", email);
        req.setAttribute("otp", otp);
        req.setAttribute("error", error);
        req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
    }

    private boolean isOtpRequired(AdminSettings adminSettings) {
        return adminSettings != null && adminSettings.isMailEnabled() && EMAIL_SERVICE.isConfigured(adminSettings);
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}

