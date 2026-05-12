package server.dao;

import server.database.DBConnection;
import shared.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitDAO {
    private static final String ELECTRONICS_FILTER = " WHERE categorie_metier = 'Electronique' ";
    private final Connection connection;

    public ProduitDAO() {
        this.connection = DBConnection.getConnection();
    }

    public List<Produit> findAll() {
        return queryProducts("SELECT * FROM products" + ELECTRONICS_FILTER + "ORDER BY rating DESC, nom_produit ASC");
    }

    public Produit findById(int id) {
        List<Produit> products = queryProducts("SELECT * FROM products WHERE id = " + id + " AND categorie_metier = 'Electronique'");
        return products.isEmpty() ? null : products.get(0);
    }

    public boolean updateStock(int id, int newStock) {
        String availability = newStock <= 0 ? "Out of Stock" : newStock <= 5 ? "Low Stock" : "In Stock";
        String query = "UPDATE products SET stock = ?, disponibilite = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, newStock);
            stmt.setString(2, availability);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product stock: " + e.getMessage());
            return false;
        }
    }

    public boolean createProduct(Produit produit) {
        String query = "INSERT INTO products (sku, nom_produit, marque, categorie_source, categorie_metier, prix_usd, remise_pct, prix_net_usd, rating, stock, disponibilite, description, image_principale, nb_images, source_catalogue) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            bindProduct(stmt, produit);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating product: " + e.getMessage());
            return false;
        }
    }

    public boolean updateProduct(Produit produit) {
        String query = "UPDATE products SET sku = ?, nom_produit = ?, marque = ?, categorie_source = ?, categorie_metier = ?, prix_usd = ?, remise_pct = ?, prix_net_usd = ?, rating = ?, stock = ?, disponibilite = ?, description = ?, image_principale = ?, nb_images = ?, source_catalogue = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            bindProduct(stmt, produit);
            stmt.setInt(16, produit.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProduct(int id) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM products WHERE id = ?")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }

    public List<Produit> findByCategory(String category) {
        String normalized = normalizeCategory(category);
        String query = "SELECT * FROM products WHERE categorie_metier = 'Electronique' AND LOWER(categorie_source) = ? ORDER BY rating DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, normalized);
            return readProducts(stmt);
        } catch (SQLException e) {
            System.err.println("Error filtering products by category: " + e.getMessage());
            return List.of();
        }
    }

    public List<Produit> searchProducts(String keyword) {
        String query = "SELECT * FROM products WHERE categorie_metier = 'Electronique' AND (" +
                "LOWER(nom_produit) LIKE ? OR LOWER(marque) LIKE ? OR LOWER(description) LIKE ? OR LOWER(categorie_source) LIKE ?) " +
                "ORDER BY rating DESC, nom_produit ASC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String like = "%" + keyword.toLowerCase() + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            stmt.setString(3, like);
            stmt.setString(4, like);
            return readProducts(stmt);
        } catch (SQLException e) {
            System.err.println("Error searching products: " + e.getMessage());
            return List.of();
        }
    }

    public List<Produit> findLowStockProducts(int threshold) {
        String query = "SELECT * FROM products WHERE categorie_metier = 'Electronique' AND stock <= ? ORDER BY stock ASC, nom_produit ASC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, threshold);
            return readProducts(stmt);
        } catch (SQLException e) {
            System.err.println("Error finding low stock products: " + e.getMessage());
            return List.of();
        }
    }

    public int countElectronicsProducts() {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM products WHERE categorie_metier = 'Electronique'");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Error counting products: " + e.getMessage());
            return 0;
        }
    }

    public List<Produit> findBestRatedProducts(int limit) {
        String query = "SELECT * FROM products WHERE categorie_metier = 'Electronique' ORDER BY rating DESC, prix_net_usd DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            return readProducts(stmt);
        } catch (SQLException e) {
            System.err.println("Error finding best rated products: " + e.getMessage());
            return List.of();
        }
    }

    private void bindProduct(PreparedStatement stmt, Produit produit) throws SQLException {
        stmt.setString(1, produit.getSku());
        stmt.setString(2, produit.getNomProduit());
        stmt.setString(3, produit.getMarque());
        stmt.setString(4, normalizeCategory(produit.getCategorieSource()));
        stmt.setString(5, produit.getCategorieMetier() == null ? "Electronique" : produit.getCategorieMetier());
        stmt.setDouble(6, produit.getPrixUsd() > 0 ? produit.getPrixUsd() : produit.getFinalPrice());
        stmt.setDouble(7, produit.getRemisePct());
        stmt.setDouble(8, produit.getFinalPrice());
        stmt.setDouble(9, produit.getRating());
        stmt.setInt(10, produit.getStock());
        stmt.setString(11, produit.getDisponibilite() == null ? deriveAvailability(produit.getStock()) : produit.getDisponibilite());
        stmt.setString(12, produit.getDescription());
        stmt.setString(13, produit.getImagePrincipale());
        stmt.setInt(14, produit.getNbImages());
        stmt.setString(15, produit.getSourceCatalogue() == null ? "admin-panel" : produit.getSourceCatalogue());
    }

    private String deriveAvailability(int stock) {
        return stock <= 0 ? "Out of Stock" : stock <= 5 ? "Low Stock" : "In Stock";
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank() || "All".equalsIgnoreCase(category)) {
            return "smartphones";
        }
        return category.trim().toLowerCase()
                .replace("accessories", "mobile-accessories");
    }

    private List<Produit> queryProducts(String sql) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapProducts(rs);
        } catch (SQLException e) {
            System.err.println("Error querying products: " + e.getMessage());
            return List.of();
        }
    }

    private List<Produit> readProducts(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            return mapProducts(rs);
        }
    }

    private List<Produit> mapProducts(ResultSet rs) throws SQLException {
        List<Produit> products = new ArrayList<>();
        while (rs.next()) {
            Produit produit = new Produit(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    coalesce(rs.getString("nom_produit"), readLegacyString(rs, "name")),
                    rs.getString("marque"),
                    rs.getString("categorie_source"),
                    coalesce(rs.getString("categorie_metier"), "Electronique"),
                    coalesceDouble(rs.getDouble("prix_usd"), readLegacyDouble(rs, "price")),
                    rs.getDouble("remise_pct"),
                    coalesceDouble(rs.getDouble("prix_net_usd"), readLegacyDouble(rs, "price")),
                    rs.getDouble("rating"),
                    rs.getInt("stock"),
                    coalesce(rs.getString("disponibilite"), deriveAvailability(rs.getInt("stock"))),
                    rs.getString("description"),
                    rs.getString("image_principale"),
                    rs.getInt("nb_images"),
                    rs.getString("source_catalogue")
            );
            products.add(produit);
        }
        return products;
    }

    private String readLegacyString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private double readLegacyDouble(ResultSet rs, String column) {
        try {
            return rs.getDouble(column);
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private String coalesce(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private double coalesceDouble(double value, double fallback) {
        return value > 0 ? value : fallback;
    }
}
