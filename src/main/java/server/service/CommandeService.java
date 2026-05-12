package server.service;

import server.dao.CommandeDAO;
import server.dao.PanierDAO;
import shared.model.Commande;
import shared.model.DashboardStats;
import shared.model.OrderItemDetail;
import shared.model.Panier;
import shared.network.Response;
import java.util.List;

/**
 * Service handling logic for Orders.
 */
public class CommandeService {
    private CommandeDAO commandeDAO;
    private PanierDAO panierDAO;
    private ProduitService produitService;
    private PanierService panierService;

    public CommandeService(PanierService panierService) {
        this.commandeDAO = new CommandeDAO();
        this.panierDAO = new PanierDAO();
        this.produitService = new ProduitService();
        this.panierService = panierService;
    }

    /**
     * Validates a cart and creates an order.
     */
    public Response createOrder(int userId) {
        Response cartResponse = panierService.getCart(userId);
        if (!cartResponse.isSuccess()) {
            return new Response(false, "Failed to retrieve cart.", null);
        }

        Panier cart = (Panier) cartResponse.getData();
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return new Response(false, "Cart is empty.", null);
        }

        // Validate stock for all items
        for (shared.model.LignePanier item : cart.getItems()) {
            shared.model.Produit p = item.getProduct();
            if (p.getStock() < item.getQuantity()) {
                return new Response(false, "Not enough stock for product: " + p.getName(), null);
            }
        }

        // Create the Order
        Commande commande = new Commande(userId, new java.util.Date(), cart.getTotal(), "PENDING");
        int orderId = commandeDAO.save(commande);

        if (orderId != -1) {
            // Deduct stock and save order items
            for (shared.model.LignePanier item : cart.getItems()) {
                produitService.checkAndUpdateStock(item.getProduct().getId(), item.getQuantity());
            }
            panierDAO.saveOrderItems(orderId, cart);

            // Clear the user's cart
            panierService.clearCart(userId);

            commande.setId(orderId);
            return new Response(true, "Order validated successfully.", commande);
        } else {
            return new Response(false, "Failed to create order.", null);
        }
    }

    public Response getOrdersByUser(int userId) {
        List<Commande> orders = commandeDAO.findByUser(userId);
        if (orders != null && !orders.isEmpty()) {
            return new Response(true, "Orders retrieved.", orders);
        } else {
            return new Response(false, "No orders found.", null);
        }
    }

    public Response getAllOrders(String status) {
        List<Commande> orders = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? commandeDAO.findAll()
                : commandeDAO.findByStatus(status);
        return new Response(true, "Orders retrieved.", orders);
    }

    public Response getOrderDetails(int orderId) {
        List<OrderItemDetail> details = commandeDAO.findOrderItemsByOrderId(orderId);
        return new Response(true, "Order details retrieved.", details);
    }

    public Response updateOrderStatus(int orderId, String newStatus) {
        if (!isValidStatus(newStatus)) {
            return new Response(false, "Invalid order status.", null);
        }
        if (commandeDAO.updateStatus(orderId, newStatus)) {
            return new Response(true, "Order status updated.", null);
        }
        return new Response(false, "Failed to update order status.", null);
    }

    public Response getDashboardStats() {
        DashboardStats stats = commandeDAO.getDashboardStats();
        return new Response(true, "Dashboard stats retrieved.", stats);
    }

    private boolean isValidStatus(String status) {
        return "PENDING".equals(status) || "VALIDATED".equals(status) || "SHIPPED".equals(status) || "CANCELLED".equals(status) || "PAID".equals(status);
    }
}
