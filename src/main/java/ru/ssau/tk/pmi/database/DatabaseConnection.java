package ru.ssau.tk.pmi.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    private static final String URL = "jdbc:postgresql://localhost:5432/lab_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "user";

    static {
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("PostgreSQL JDBC Driver registered successfully");
        } catch (ClassNotFoundException e) {
            logger.severe("PostgreSQL JDBC Driver not found: " + e.getMessage());
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            logger.info("Database connection established successfully");
            return connection;
        } catch (SQLException e) {
            logger.severe("Failed to establish database connection: " + e.getMessage());
            throw e;
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            logger.info("Database connection test: SUCCESS");
        } catch (SQLException e) {
            logger.severe("Database connection test: FAILED - " + e.getMessage());
        }
    }
}
