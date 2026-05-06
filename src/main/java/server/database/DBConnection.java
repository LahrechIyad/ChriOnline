package server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

/**
 * Manages the SQLite Database Connection.
 */
public class DBConnection {
    private static final String URL = "jdbc:sqlite:chri_online.db";
    private static Connection connection = null;

    private DBConnection() {}

    /**
     * Gets the singleton database connection.
     * @return Connection object
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Ensure the JDBC driver is loaded
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                System.out.println("Connected to SQLite database: chri_online.db");
            } catch (SQLException | ClassNotFoundException e) {
                System.err.println("Database Connection failed: " + e.getMessage());
            }
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }

    /**
     * Initializes the database with the schema.sql file if it hasn't been created.
     */
    public static void initializeDatabase() {
        Connection conn = getConnection();
        if (conn == null) return;
        
        File schemaFile = new File("database/schema.sql");
        if (!schemaFile.exists()) {
            System.err.println("Schema file not found at database/schema.sql");
            return;
        }

        try (Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(schemaFile))) {
            
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0); // Clear string builder
                }
            }
            System.out.println("Database schema initialized successfully.");

            // Migration: Add is_verified and verification_code silently (ignores error if already exist)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT 0");
                System.out.println("Migrated users table: added is_verified");
            } catch (SQLException e) { /* Column might exist */ }
            
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN verification_code VARCHAR(10)");
                System.out.println("Migrated users table: added verification_code");
            } catch (SQLException e) { /* Column might exist */ }

            try {
                stmt.execute("ALTER TABLE users ADD COLUMN public_key TEXT");
                System.out.println("Migrated users table: added public_key");
            } catch (SQLException e) { /* Column might exist */ }

        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }
}
