 
package com.electricity;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/api/bills")
public class BillServlet extends HttpServlet {

    private String p(HttpServletRequest r, String name) {
        return r.getParameter(name) == null ? "" : r.getParameter(name).trim();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String sql = """
            SELECT b.id,b.customer_id,c.name,c.meter_number,
                   b.units_consumed,b.amount,b.bill_month,b.payment_status
            FROM bills b JOIN customer c ON b.customer_id=c.id
            ORDER BY b.id DESC
            """;

        StringBuilder out = new StringBuilder("[");
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            boolean first = true;
            while (rs.next()) {
                if (!first) out.append(",");
                first = false;
                out.append("{\"id\":").append(rs.getInt("id"))
                   .append(",\"customerId\":").append(rs.getInt("customer_id"))
                   .append(",\"name\":\"").append(esc(rs.getString("name")))
                   .append("\",\"meterNumber\":\"").append(esc(rs.getString("meter_number")))
                   .append("\",\"units\":").append(rs.getInt("units_consumed"))
                   .append(",\"amount\":").append(rs.getBigDecimal("amount"))
                   .append(",\"month\":\"").append(esc(rs.getString("bill_month")))
                   .append("\",\"status\":\"").append(esc(rs.getString("payment_status")))
                   .append("\"}");
            }
            res.getWriter().print(out.append("]"));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String action = p(req, "action");

        try (Connection con = DBConnection.getConnection()) {

            if ("status".equals(action)) {
                int id = Integer.parseInt(p(req, "id"));
                String status = p(req, "status").toUpperCase();
                if (!status.equals("PAID") && !status.equals("UNPAID")) {
                    res.sendError(400, "Invalid payment status");
                    return;
                }

                try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE bills SET payment_status=? WHERE id=?")) {
                    ps.setString(1, status);
                    ps.setInt(2, id);
                    if (ps.executeUpdate() == 0) {
                        res.sendError(404, "Bill not found");
                        return;
                    }
                }
                res.sendRedirect(req.getContextPath() + "/bills.html");
                return;
            }

            int customerId = Integer.parseInt(p(req, "customerId"));
            int units = Integer.parseInt(p(req, "units"));
            String month = p(req, "month");

            if (units < 0 || month.isEmpty()) {
                res.sendError(400, "Valid units and billing month are required");
                return;
            }

            try (PreparedStatement check = con.prepareStatement(
                "SELECT id FROM customer WHERE id=?")) {
                check.setInt(1, customerId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        res.sendError(400, "Select a valid customer");
                        return;
                    }
                }
            }

            double amount = calculate(units);

            try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO bills(customer_id,units_consumed,amount,bill_month,payment_status) VALUES(?,?,?,?, 'UNPAID')")) {
                ps.setInt(1, customerId);
                ps.setInt(2, units);
                ps.setDouble(3, amount);
                ps.setString(4, month);
                ps.executeUpdate();
            }

            res.sendRedirect(req.getContextPath() + "/bills.html");

        } catch (SQLException | NumberFormatException e) {
            throw new ServletException("Could not process bill", e);
        }
    }

    private double calculate(int units) {
        if (units <= 100) return units * 3.0;
        if (units <= 200) return 100 * 3.0 + (units - 100) * 5.0;
        return 100 * 3.0 + 100 * 5.0 + (units - 200) * 7.0;
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "DELETE FROM bills WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(req.getParameter("id")));
            if (ps.executeUpdate() == 0) {
                res.sendError(404, "Bill not found");
                return;
            }
            res.setStatus(204);
        } catch (SQLException | NumberFormatException e) {
            throw new ServletException(e);
        }
    }
}
