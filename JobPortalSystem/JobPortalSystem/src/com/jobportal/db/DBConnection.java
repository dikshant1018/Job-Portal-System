package com.jobportal.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String HOST = "metro.proxy.rlwy.net";
    private static final String PORT = "54506";
    private static final String DATABASE = "railway";
    private static final String USER = "root";
    private static final String PASSWORD = "oWfOZRNHGTgGdYpMqctTxdhXuGWwAZVR";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver missing. Ensure mysql-connector-j jar is added to your project dependencies.", e);
        }
    }

    // Quick connection test
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("SUCCESS: Connected to Railway MySQL database successfully!");
        } catch (Exception e) {
            System.err.println("CONNECTION FAILED:");
            e.printStackTrace();
        }
    }
}