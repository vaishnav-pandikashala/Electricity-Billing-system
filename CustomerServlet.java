
package com.electricity;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@WebServlet("/api/customers")
public class CustomerServlet extends HttpServlet {

    private String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private void sendJson(HttpServletResponse res, int status,
                          String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        res.getWriter().write("{\"message\":\"" + safeMessage + "\"}");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // GET: Display all customers
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        StringBuilder output = new StringBuilder("[");

        String sql = "SELECT id, name, meter_number, address, phone, email "
                   + "FROM customer ORDER BY id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean first = true;

            while (rs.next()) {
                if (!first) {
                    output.append(",");
                }
                first = false;

                output.append("{")
                      .append("\"id\":").append(rs.getInt("id"))
                      .append(",\"name\":\"")
                      .append(escape(rs.getString("name")))
                      .append("\",\"meterNumber\":\"")
                      .append(escape(rs.getString("meter_number")))
                      .append("\",\"address\":\"")
                      .append(escape(rs.getString("address")))
                      .append("\",\"phone\":\"")
                      .append(escape(rs.getString("phone")))
                      .append("\",\"email\":\"")
                      .append(escape(rs.getString("email")))
                      .append("\"}");
            }

            output.append("]");
            res.getWriter().write(output.toString());

        } catch (SQLException e) {
            throw new ServletException("Could not load customers", e);
        }
    }

    // POST: Add a customer or update an existing customer
    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse res)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String id = param(req, "id");
        String name = param(req, "name");
        String meter = param(req, "meterNumber");
        String address = param(req, "address");
        String phone = param(req, "phone");
        String email = param(req, "email");

        // Diagnostic messages in the Tomcat console
        System.out.println("=== CUSTOMER SAVE DEBUG ===");
        System.out.println("Content-Type: " + req.getContentType());
        System.out.println("Name: [" + name + "]");
        System.out.println("Meter: [" + meter + "]");
        System.out.println("Address: [" + address + "]");
        System.out.println("============================");

        if (name.isEmpty() || meter.isEmpty() || address.isEmpty()) {
            sendJson(res, 400,
                    "Required field is empty. Name=[" + name
                    + "], Meter=[" + meter
                    + "], Address=[" + address + "]");
            return;
        }

        boolean editing = !id.isEmpty();

        String sql;

        if (editing) {
            sql = "UPDATE customer SET name=?, meter_number=?, "
                + "address=?, phone=?, email=? WHERE id=?";
        } else {
            sql = "INSERT INTO customer "
                + "(name, meter_number, address, phone, email) "
                + "VALUES (?, ?, ?, ?, ?)";
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, meter);
            ps.setString(3, address);
            ps.setString(4, phone);
            ps.setString(5, email);

            if (editing) {
                int customerId = Integer.parseInt(id);
                ps.setInt(6, customerId);
            }

            int changed = ps.executeUpdate();

            if (changed == 0) {
                sendJson(res, 404, "Customer not found.");
                return;
            }

            sendJson(res, 200, editing
                    ? "Customer updated successfully."
                    : "Customer saved successfully.");

        } catch (NumberFormatException e) {
            sendJson(res, 400, "Invalid customer ID.");
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                sendJson(res, 409,
                        "This meter number may already exist. Check the database.");
                return;
            }

            throw new ServletException("Could not save customer", e);
        }
    }

    // DELETE: Delete a customer
    @Override
    protected void doDelete(HttpServletRequest req,
                            HttpServletResponse res)
            throws ServletException, IOException {

        String id = param(req, "id");

        if (id.isEmpty()) {
            sendJson(res, 400, "Customer ID is required.");
            return;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM customer WHERE id=?")) {

            ps.setInt(1, Integer.parseInt(id));

            int changed = ps.executeUpdate();

            if (changed == 0) {
                sendJson(res, 404, "Customer not found.");
                return;
            }

            res.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (NumberFormatException e) {
            sendJson(res, 400, "Invalid customer ID.");
        } catch (SQLException e) {
            throw new ServletException("Could not delete customer", e);
        }
    }
}
