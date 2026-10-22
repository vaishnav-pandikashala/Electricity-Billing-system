 
package com.electricity;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            response.sendError(400, "Username and password are required");
            return;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT id, username FROM admin WHERE username=? AND password=?")) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("adminId", rs.getInt("id"));
                    session.setAttribute("adminUsername", rs.getString("username"));
                    session.setMaxInactiveInterval(1800);

                    response.sendRedirect(request.getContextPath() + "/dashboard.html");
                } else {
                    response.sendRedirect(request.getContextPath()
                        + "/login.html?error=1");
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Login database error", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.html");
    }
}
