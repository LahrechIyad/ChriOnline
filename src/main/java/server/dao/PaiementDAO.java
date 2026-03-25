package server.dao;

import shared.model.Paiement;
import server.database.DBConnection;

import java.sql.*;

/**
 * Handles database operations for Payments.
 */
public class PaiementDAO {
    private Connection connection;

    public PaiementDAO() {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Saves a payment to the database.
     * @param paiement The payment object
     * @return true if successful
     */
    public boolean save(Paiement paiement) {
        String query = "INSERT INTO payments (order_id, amount, method, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, paiement.getOrderId());
            stmt.setDouble(2, paiement.getAmount());
            stmt.setString(3, paiement.getMethod());
            stmt.setString(4, paiement.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        paiement.setId(rs.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving payment: " + e.getMessage());
        }
        return false;
    }
}
