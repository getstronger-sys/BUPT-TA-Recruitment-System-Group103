package bupt.ta.servlet;

import bupt.ta.storage.DataStorage;
import bupt.ta.util.RememberMeCookie;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Ends the current session and clears remember-me tokens for the user.
 */
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST to log out.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        DataStorage storage = new DataStorage(req.getServletContext());
        String raw = RememberMeCookie.readRawToken(req);
        if (raw != null) {
            storage.revokeRememberMeToken(raw);
        }
        if (session != null) {
            Object uid = session.getAttribute("userId");
            if (uid != null) {
                storage.revokeAllRememberMeTokensForUser(String.valueOf(uid));
            }
            session.invalidate();
        }
        RememberMeCookie.clear(resp, req.getContextPath(), req.isSecure());
        resp.sendRedirect(req.getContextPath() + "/index.jsp");
    }
}
