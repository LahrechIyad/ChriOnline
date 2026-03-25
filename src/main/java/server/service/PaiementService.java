package server.service;

import server.dao.CommandeDAO;
import server.dao.PaiementDAO;
import shared.model.Paiement;
import shared.network.Response;

/**
 * Service handling logic for Payments.
 */
public class PaiementService {
    private PaiementDAO paiementDAO;
    private CommandeDAO commandeDAO;
    private PanierService panierService;

    public PaiementService(PanierService panierService) {
        this.paiementDAO = new PaiementDAO();
        this.commandeDAO = new CommandeDAO();
        this.panierService = panierService;
    }

    /**
     * Simulates payment processing and confirms payment.
     */
    public Response processPayment(int orderId, double amount, String method) {
        // In a real application, we would call an external payment API here (Stripe, PayPal, etc.)
        System.out.println("Processing payment of " + amount + " via " + method + " for order " + orderId + "...");
        
        // Simulate a delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate successful payment
        Paiement paiement = new Paiement(orderId, amount, method, "SUCCESS");
        boolean isSaved = paiementDAO.save(paiement);

        if (isSaved) {
            // Update order status
            commandeDAO.updateStatus(orderId, "VALIDATED");

            // Clear the user's cart (safety net — also done in createOrder)
            int userId = commandeDAO.findUserIdByOrderId(orderId);
            if (userId != -1) {
                panierService.clearCart(userId);
            }

            return new Response(true, "Payment successful. Order VALIDATED.", paiement);
        } else {
            return new Response(false, "Payment failed to save.", null);
        }
    }
}
