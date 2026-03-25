package server.dao;

import shared.model.Panier;
import shared.model.LignePanier;
import shared.model.Produit;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for the Cart and Cart Items.
 * Useful if we want to save carts to database when users log out, 
 * or just persist them. In this simple project, we can just save order items directly,
 * but this is mapping the cart as requested.
 */
public class PanierDAO {
    private Connection connection;
    private ProduitDAO produitDAO;

    public PanierDAO() {
        this.connection = DBConnection.getConnection();
        this.produitDAO = new ProduitDAO();
    }

    /**
     * Saves cart items associated with an order ID.
     * @param orderId the order ID
     * @param panier the cart containing items
     * @return true if all items were inserted
     */
    public boolean saveOrderItems(int orderId, Panier panier) {
        String query = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (LignePanier ligne : panier.getItems()) {
                    stmt.setInt(1, orderId);
                    stmt.setInt(2, ligne.getProduct().getId());
                    stmt.setInt(3, ligne.getQuantity());
                    stmt.setDouble(4, ligne.getProduct().getPrice());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                System.err.println("Error saving order items: " + e.getMessage());
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Database transaction error: " + e.getMessage());
            return false;
        }
    }
}
