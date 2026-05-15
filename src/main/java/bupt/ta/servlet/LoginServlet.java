package bupt.ta.servlet;

import bupt.ta.model.User;
import bupt.ta.storage.DataStorage;
import bupt.ta.util.PasswordHasher;
import bupt.ta.util.RememberMeCookie;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Authenticates users and establishes a session; optionally issues a remember-me cookie.
 */
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String error = null;

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            error = "Please enter username and password.";
        } else {
            DataStorage storage = new DataStorage(getServletContext());
            User user = storage.findByUsername(username.trim());
            if (user == null || !PasswordHasher.matches(password, user.getPassword())) {
                error = "Invalid username or password.";
            } else {
                if (!PasswordHasher.isHashed(user.getPassword())) {
                    user.setPassword(PasswordHasher.hash(password));
                    storage.saveUser(user);
                }
                HttpSession existing = req.getSession(false);
                if (existing != null) {
                    existing.invalidate();
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("user", user);
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("role", user.getRole());
                session.setAttribute("realName", user.getRealName());
                session.setAttribute("justLoggedIn", Boolean.TRUE);

                boolean remember = "1".equals(req.getParameter("remember"));
                if (remember) {
                    String token = storage.issueRememberMeToken(user.getId());
                    RememberMeCookie.setToken(resp, req.getContextPath(), req.isSecure(), token);
                } else {
                    storage.revokeAllRememberMeTokensForUser(user.getId());
                    RememberMeCookie.clear(resp, req.getContextPath(), req.isSecure());
                }

                String redirect = req.getContextPath() + "/dashboard.jsp";
                resp.sendRedirect(redirect);
                return;
            }
        }
        req.setAttribute("error", error);
        req.getRequestDispatcher("/index.jsp").forward(req, resp);
    }
}
