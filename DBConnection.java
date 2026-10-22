
package com.electricity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/electricity_billing_new?serverTimezone=UTC";

    private static final String USER = "root";

    // Replace this with your actual MySQL password
    private static final String PASSWORD = "127869";

    public static Connection getConnection() throws SQLException {

        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC driver not found. Check WEB-INF/lib.", e);
        }

        // Connect to the MySQL database
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
