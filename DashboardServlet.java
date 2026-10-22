 
package com.electricity;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String sql = """
            SELECT
              (SELECT COUNT(*) FROM customer) AS customers,
              (SELECT COUNT(*) FROM bills) AS bills,
              (SELECT COALESCE(SUM(amount),0) FROM bills
               WHERE payment_status='PAID') AS paid,
              (SELECT COALESCE(SUM(amount),0) FROM bills
               WHERE payment_status='UNPAID') AS unpaid
            """;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            rs.next();

            response.getWriter().printf(
                java.util.Locale.US,
                "{\"customers\":%d,\"bills\":%d,\"paid\":%.2f,\"unpaid\":%.2f}",
                rs.getInt("customers"),
                rs.getInt("bills"),
                rs.getDouble("paid"),
                rs.getDouble("unpaid")
            );

        } catch (SQLException e) {
            throw new ServletException("Could not load dashboard", e);
        }
    }
}
