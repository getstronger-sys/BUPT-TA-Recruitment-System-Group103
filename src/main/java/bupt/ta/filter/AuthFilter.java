package bupt.ta.filter;

import bupt.ta.storage.DataStorage;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Ensures user is logged in and has correct role before accessing protected pages.
 */
public class AuthFilter implements Filter {

    /**
     * {@inheritDoc}
     * <p>Redirects unauthenticated users to login and enforces role-based path access.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=login");
            return;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        String role = (String) session.getAttribute("role");
        if (role == null) {
            response.sendRedirect(contextPath + "/index.jsp?error=login");
            return;
        }

        boolean allowed = false;
        if (path.startsWith("/ta/") && "TA".equals(role)) allowed = true;
        else if (path.startsWith("/mo/") && "MO".equals(role)) allowed = true;
        else if (path.startsWith("/admin/") && "ADMIN".equals(role)) allowed = true;

        if (!allowed) {
            response.sendRedirect(contextPath + "/dashboard.jsp?error=forbidden");
            return;
        }

        // Unread in-app message count for TA shell (sidebar badge)
        if ("TA".equals(role) && path.startsWith("/ta/")) {
            try {
                String uid = (String) session.getAttribute("userId");
                DataStorage storage = new DataStorage(request.getServletContext());
                int unread = storage.countUnreadSiteNotificationsForUser(uid);
                request.setAttribute("taUnreadNotificationCount", unread);
            } catch (IOException e) {
                request.setAttribute("taUnreadNotificationCount", 0);
            }
        }

        chain.doFilter(req, resp);
    }
}
