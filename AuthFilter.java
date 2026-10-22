 
package com.electricity;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns = {
    "/dashboard.html", "/customers.html", "/bills.html",
    "/api/*"
})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("adminId") == null) {
            if (req.getRequestURI().contains("/api/")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                              "Please log in first");
            } else {
                res.sendRedirect(req.getContextPath() + "/login.html");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
