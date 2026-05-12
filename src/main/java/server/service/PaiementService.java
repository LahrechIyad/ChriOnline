package server.service;

import server.dao.CommandeDAO;
import server.dao.PaiementDAO;
import server.dao.UserDAO;
import server.security.DatabaseCryptoService;
import shared.model.Paiement;
import shared.model.User;
import shared.network.Response;

import java.util.Map;

/**
 * Service handling logic for Payments.
 */
public class PaiementService {
    private PaiementDAO paiementDAO;
    private CommandeDAO commandeDAO;
    private PanierService panierService;
    private UserDAO userDAO;
    private EmailService emailService;
    private DatabaseCryptoService databaseCryptoService;

    public PaiementService(PanierService panierService) {
        this.paiementDAO = new PaiementDAO();
        this.commandeDAO = new CommandeDAO();
        this.panierService = panierService;
        this.userDAO = new UserDAO();
        this.emailService = new EmailService();
        this.databaseCryptoService = new DatabaseCryptoService();
    }

    /**
     * Simulates payment processing and confirms payment.
     */
    public Response processPayment(int orderId, double amount, String method) {
        return processPayment(orderId, amount, method, null, null);
    }

    public Response processPayment(int orderId, double amount, String method, String maskedCard, String deliveryAddress) {
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
        paiement.setMaskedCard(maskedCard);
        paiement.setEncryptedBillingOrDeliveryAddress(databaseCryptoService.encryptToBase64(deliveryAddress));
        boolean isSaved = paiementDAO.save(paiement);

        if (isSaved) {
            // Update order status
            commandeDAO.updateStatus(orderId, "VALIDATED");

            // Clear the user's cart (safety net - also done in createOrder)
            int userId = commandeDAO.findUserIdByOrderId(orderId);
            if (userId != -1) {
                panierService.clearCart(userId);
                
                // Send confirmation email
                User user = userDAO.findById(userId);
                if (user != null && user.getEmail() != null) {
                    new Thread(() -> emailService.sendOrderSummary(user.getEmail(), orderId, amount, method)).start();
                }
            }

            return new Response(true, "Payment successful. Order VALIDATED.", paiement);
        } else {
            return new Response(false, "Payment failed to save.", null);
        }
    }
}
