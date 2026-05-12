package server.dao;

import shared.model.Commande;
import shared.model.DashboardStats;
import shared.model.OrderItemDetail;
import shared.model.Produit;
import server.database.DBConnection;
import server.dao.ProduitDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Handles database operations for Orders (Commandes).
 */
public class CommandeDAO {
    private Connection connection;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ProduitDAO produitDAO = new ProduitDAO();

    public CommandeDAO() {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Saves an order into the database.
     * @param commande The Command to save
     * @return the generated ID of the order, or -1 if failed
     */
    public int save(Commande commande) {
        String query = "INSERT INTO orders (user_id, order_date, total, status) VALUES (?, CURRENT_TIMESTAMP, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, commande.getUserId());
            stmt.setDouble(2, commande.getTotal());
            stmt.setString(3, commande.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        commande.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving commande: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Updates order status.
     */
    public boolean updateStatus(int orderId, String newStatus) {
        String query = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating order status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Finds orders by user ID.
     * @param userId User ID
     * @return List of Commandes
     */
    /**
     * Returns the user_id associated with the given order ID.
     */
    public int findUserIdByOrderId(int orderId) {
        String query = "SELECT user_id FROM orders WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            System.err.println("Error finding userId by orderId: " + e.getMessage());
        }
        return -1;
    }

    public List<Commande> findByUser(int userId) {
        List<Commande> orders = new ArrayList<>();
        String query = "SELECT * FROM orders WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Date orderDate = new Date(); // Fallback date if parse fails
                try {
                    String dateStr = rs.getString("order_date");
                    if (dateStr != null) {
                        orderDate = sdf.parse(dateStr);
                    }
                } catch (ParseException e) {
                    System.err.println("Date format parse error: " + e.getMessage());
                }

                orders.add(new Commande(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    orderDate,
                    rs.getDouble("total"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error finding orders by user: " + e.getMessage());
        }
        return orders;
    }

    public List<Commande> findAll() {
        List<Commande> orders = new ArrayList<>();
        String query = "SELECT o.*, u.email, u.username FROM orders o JOIN users u ON u.id = o.user_id ORDER BY o.order_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Commande commande = new Commande(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        parseDate(rs.getString("order_date")),
                        rs.getDouble("total"),
                        rs.getString("status")
                );
                commande.setUserEmail(rs.getString("email"));
                commande.setUsername(rs.getString("username"));
                orders.add(commande);
            }
        } catch (SQLException e) {
            System.err.println("Error finding all orders: " + e.getMessage());
        }
        return orders;
    }

    public List<Commande> findByStatus(String status) {
        List<Commande> orders = new ArrayList<>();
        String query = "SELECT o.*, u.email, u.username FROM orders o JOIN users u ON u.id = o.user_id WHERE o.status = ? ORDER BY o.order_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Commande commande = new Commande(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            parseDate(rs.getString("order_date")),
                            rs.getDouble("total"),
                            rs.getString("status")
                    );
                    commande.setUserEmail(rs.getString("email"));
                    commande.setUsername(rs.getString("username"));
                    orders.add(commande);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding orders by status: " + e.getMessage());
        }
        return orders;
    }

    public List<OrderItemDetail> findOrderItemsByOrderId(int orderId) {
        List<OrderItemDetail> items = new ArrayList<>();
        String query = "SELECT oi.order_id, oi.product_id, oi.quantity, oi.price, p.nom_produit, p.marque " +
                "FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItemDetail(
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getString("nom_produit"),
                            rs.getString("marque"),
                            rs.getInt("quantity"),
                            rs.getDouble("price")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading order details: " + e.getMessage());
        }
        return items;
    }

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        stats.setTotalProducts(produitDAO.countElectronicsProducts());
        stats.setLowStockProducts(produitDAO.findLowStockProducts(5).size());
        stats.setBestRatedProducts(produitDAO.findBestRatedProducts(5));
        stats.setRecentOrders(findRecentOrders(5));

        try (PreparedStatement totalOrdersStmt = connection.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet totalOrdersRs = totalOrdersStmt.executeQuery()) {
            if (totalOrdersRs.next()) {
                stats.setTotalOrders(totalOrdersRs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error counting orders: " + e.getMessage());
        }

        try (PreparedStatement revenueStmt = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS'");
             ResultSet revenueRs = revenueStmt.executeQuery()) {
            if (revenueRs.next()) {
                stats.setTotalRevenue(revenueRs.getDouble(1));
            }
        } catch (SQLException e) {
            System.err.println("Error calculating revenue: " + e.getMessage());
        }
        return stats;
    }

    private List<Commande> findRecentOrders(int limit) {
        List<Commande> orders = new ArrayList<>();
        String query = "SELECT o.*, u.email, u.username FROM orders o JOIN users u ON u.id = o.user_id ORDER BY o.order_date DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Commande commande = new Commande(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            parseDate(rs.getString("order_date")),
                            rs.getDouble("total"),
                            rs.getString("status")
                    );
                    commande.setUserEmail(rs.getString("email"));
                    commande.setUsername(rs.getString("username"));
                    orders.add(commande);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading recent orders: " + e.getMessage());
        }
        return orders;
    }

    private Date parseDate(String value) {
        if (value == null) {
            return new Date();
        }
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            return new Date();
        }
    }
}
