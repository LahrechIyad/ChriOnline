package server.dao;

import shared.model.Produit;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for Products.
 */
public class ProduitDAO {
    private Connection connection;

    public ProduitDAO() {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Retrieves all products from the catalog.
     * @return List of products
     */
    public List<Produit> findAll() {
        List<Produit> products = new ArrayList<>();
        String query = "SELECT * FROM products";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                products.add(new Produit(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all products: " + e.getMessage());
        }
        return products;
    }

    /**
     * Finds a single product by its ID.
     * @param id Product ID
     * @return Product object if found, null otherwise
     */
    public Produit findById(int id) {
        String query = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Produit(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getInt("stock")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding product by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the stock of a product.
     * @param id Product ID
     * @param newStock New stock quantity
     * @return True if update is successful
     */
    public boolean updateStock(int id, int newStock) {
        String query = "UPDATE products SET stock = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, newStock);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product stock: " + e.getMessage());
            return false;
        }
    }
}
